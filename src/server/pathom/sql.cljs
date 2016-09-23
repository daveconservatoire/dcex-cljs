(ns pathom.sql
  (:refer-clojure :exclude [count])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [common.async :refer-macros [go-catch <?]]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs.spec :as s]
            [pathom.core :as p]
            [nodejs.knex :as knex]))

(s/def ::db any?)

(s/def ::table keyword?)
(s/def ::table-name string?)

(s/def ::f qualified-keyword?)
(s/def ::fields (s/map-of ::f string?))
(s/def ::fields' (s/map-of keyword? ::f))

(s/def ::reader-map ::p/reader-map)

(s/def ::table-spec' (s/keys :req [::table ::table-name ::fields]))
(s/def ::table-spec (s/merge ::table-spec' (s/keys :req [::fields' ::reader-map])))

(s/def ::translate-value (s/or :string string?
                               :multiple #{::translate-multiple}))

(s/def ::translate-index (s/map-of keyword? ::translate-value))

(s/def ::schema' (s/coll-of ::table-spec'))
(s/def ::schema (and (s/keys :req [::translate-index])
                     (s/map-of ::table ::table-spec)))

(s/def ::row (s/map-of string? (s/or :string string?
                                     :number number?)))

(s/def ::union-selector keyword?)
(s/def ::query-cache (partial instance? IAtom))

(defn prepare-schema [schema]
  (assoc (zipmap (map ::table schema)
                 (map #(let [m (::fields %)]
                        (assoc %
                          ::reader-map
                          (zipmap (keys m)
                                  (map
                                    (fn [field]
                                      (fn [{:keys [::row]}]
                                        (get row field)))
                                    (vals m)))
                          ::fields' (zipmap (vals m) (keys m))))
                      schema))
    ::translate-index
    (apply merge-with (fn [a b]
                        (if (= a b) a ::translate-multiple))
      (zipmap (map ::table schema)
              (map ::table-name schema))
      (->> (map ::fields schema)))))

(s/fdef prepare-schema
  :args (s/cat :schema ::schema')
  :ret ::schema)

(defn variant? [value name]
  (and (vector? value)
       (= (first value) name)))

(defn translate-args [{:keys [::schema] :as env} cmds]
  (let [{:keys [::translate-index]} schema]
    (walk/prewalk
      (fn [x]
        (cond
          (keyword? x)
          (if-let [v (get translate-index x)]
            (if (= v ::translate-multiple)
              (throw (ex-info (str "Multiple possibilities for key " x) {}))
              v)
            x)

          (variant? x ::f)
          (let [v (second x)
                x (if (and (= 2 (cljs.core/count x))
                           (qualified-keyword? v))
                    [(first x) (keyword (namespace v)) v]
                    x)]
            (str/join "." (translate-args env (rest x))))

          :else
          x))
      cmds)))

(defn row-getter [schema k f]
  (let [table (keyword (namespace k))]
    (assoc-in schema [table ::reader-map k] f)))

(defn row-get [{:keys [::table-spec] :as env} row attr]
  (p/read-from (assoc env ::row row :ast {:key attr :dispatch-key attr})
               (::reader-map table-spec)))

(defn ensure-chan [x]
  (if (p/chan? x)
    x
    (go x)))

(defn parse-row [{:keys [::table ast ::union-selector parser] :as env} row]
  (go-catch
    (let [row' {:db/table table :db/id (row-get env row :db/id)}
          query (if (p/union-children? ast)
                  (let [union-type (-> (row-get env row union-selector)
                                       ensure-chan <?)]
                    (some-> ast :query (get union-type)))
                  (:query ast))]
      (if query
        (-> (merge
              row'
              (parser (assoc env ::row row) query))
            (p/read-chan-values) <?)
        row'))))

(defn query [{:keys [::db] :as env} cmds]
  (knex/query db (translate-args env cmds)))

(defn cached-query [{:keys [::query-cache] :as env} cmds]
  (if query-cache
    (go-catch
      (let [cache-key cmds]
        (if (contains? @query-cache cache-key)
          (get @query-cache cache-key)
          (let [res (<? (query env cmds))]
            (swap! query-cache assoc cache-key res)
            res))))
    (query env cmds)))

(defn sql-node [{:keys [::table ::schema] :as env} cmds]
  (assert (get schema table) (str "[Query SQL] No specs for table " table))
  (let [{:keys [::table-name ::fields ::reader-map] :as table-spec} (get schema table)]
    (go-catch
      (let [cmds (map (fn [[type v :as cmd]]
                        (if (= type :where)
                          [type (set/rename-keys v fields)]
                          cmd)) cmds)
            rows (<? (cached-query env (cons [:from table-name] cmds)))
            env (assoc env ::table-spec table-spec
                           ::p/reader [reader-map p/placeholder-node])]
        (<? (p/read-chan-seq #(parse-row env %) rows))))))

(defn sql-first-node [env cmds]
  (go-catch
    (-> (sql-node env cmds) <?
        (first)
        (or [:error :row-not-found]))))

(defn- ensure-list [x]
  (if (sequential? x) x [x]))

(defn sql-table-node
  [{:keys [ast ::schema] :as env} table]
  (assert (get schema table) (str "[Query Table] No specs for table " table))
  (let [{:keys [limit where sort]} (:params ast)
        limit (or limit 50)]
    (sql-node (assoc env ::table table)
              (cond-> [[:limit limit]]
                where (conj [:where where])
                sort (conj (concat [:orderBy] (ensure-list sort)))))))

(defn record->map [record fields]
  (reduce (fn [m [k v]]
            (assoc m k (get record v)))
          {}
          fields))

(defn find-by [{:keys [::schema] :as env} {:keys [db/table ::query] :as search}]
  (assert table "Table is required")
  (go-catch
    (let [{:keys [::table-name ::fields]} (get schema table)
          search (-> (dissoc search :db/table ::query)
                     (set/rename-keys fields))]
      (some-> (cached-query env
                            (cond-> [[:from table-name]
                                     [:where search]
                                     [:limit 1]]
                              query (concat query)))
              <? first (record->map fields) (assoc :db/table table)))))

(defn count
  ([env table] (count env table []))
  ([{:keys [::db ::schema] :as env} table cmds]
   (assert (get schema table) (str "No specs for table " table))
   (knex/query-count db (->> (cons [:from table] cmds)
                             (translate-args env)))))

(defn save [{:keys [::schema ::db]} {:keys [db/table db/id] :as record}]
  (assert table "Table is required")
  (go-catch
    (let [{:keys [::table-name ::fields]} (get schema table)
          js-record (-> record
                        (select-keys (keys fields))
                        (dissoc :db/id)
                        (set/rename-keys fields))]
      (if id
        (do
          (<? (knex/run db [[:from table-name]
                            [:where {(get fields :db/id) id}]
                            [:update js-record]]))
          record)
        (let [id (<? (knex/insert db table-name js-record (:db/id fields)))]
          (assoc record :db/id id))))))

;; RELATIONAL MAPPING

(defn has-one [foreign-table local-field]
  (with-meta
    (fn [{:keys [::row] :as env}]
      (let [foreign-id (row-get env row local-field)]
        (sql-first-node (assoc env ::table foreign-table) [[:where {:db/id foreign-id}]])))
    {::join-one true}))

(defn has-many [foreign-table foreign-field & [params]]
  (with-meta
    (fn [{:keys [::row] :as env}]
      (sql-table-node
        (cond-> (update-in env [:ast :params :where]
                           #(assoc (or % {}) foreign-field (row-get env row :db/id)))
          (:sort params) (update-in [:ast :params :sort] #(or % (:sort params))))
        foreign-table))
    {::join-many true}))
