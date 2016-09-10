(ns pathom.sql
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [common.async :refer-macros [go-catch <?]]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs.spec :as s]
            [pathom.core :as p]
            [goog.object :as gobj]
            [nodejs.knex :as knex]))

(s/def ::db any?)

(s/def ::table keyword?)
(s/def ::table-name string?)

(s/def ::field qualified-keyword?)
(s/def ::fields (s/map-of ::field string?))
(s/def ::fields' (s/map-of keyword? ::field))

(s/def ::reader-map ::p/reader-map)

(s/def ::table-spec' (s/keys :req [::table ::table-name ::fields]))
(s/def ::table-spec (s/merge ::table-spec' (s/keys :req [::fields' ::reader-map])))

(s/def ::schema' (s/coll-of ::table-spec'))
(s/def ::schema (s/map-of ::table ::table-spec))

(s/def ::row (s/map-of string? string?))

(s/def ::union-selector keyword?)
(s/def ::query-cache (partial instance? IAtom))

(defn prepare-schema [schema]
  (zipmap (map ::table schema)
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
               schema)))

(s/fdef prepare-schema
  :args (s/cat :schema ::schema')
  :ret ::schema)

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

(defn parse-row [{:keys [::table ast ::union-selector parser path] :as env} row]
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
              (parser (assoc env ::row row
                                 :path (conj path (:dispatch-key ast))) query))
            (p/read-chan-values) <?)
        row'))))

(defn cached-query [{:keys [::db ::query-cache]} name cmds]
  (if query-cache
    (go-catch
      (let [cache-key [name cmds]]
        (if (contains? @query-cache cache-key)
          (get @query-cache cache-key)
          (let [res (<? (knex/query db name cmds))]
            (swap! query-cache assoc cache-key res)
            res))))
    (knex/query db name cmds)))

(defn sql-node [{:keys [::table ::schema] :as env} cmds]
  (assert (get schema table) (str "[Query SQL] No specs for table " table))
  (let [{:keys [::table-name ::fields ::reader-map] :as table-spec} (get schema table)]
    (go-catch
      (let [cmds (map (fn [[type v :as cmd]]
                        (if (= type :where)
                          [type (set/rename-keys v fields)]
                          cmd)) cmds)
            rows (<? (cached-query env table-name cmds))
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

(defn find-by [{:keys [::schema] :as env} {:keys [db/table] :as search}]
  (assert table "Table is required")
  (go-catch
    (let [{::keys [table-name fields]} (get schema table)
          search (-> (dissoc search :db/table)
                     (set/rename-keys fields))]
      (-> (cached-query env table-name [[:where search]]) <? first
          (record->map fields)))))

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
          (<? (knex/run db table-name [[:where {(get fields :db/id) id}]
                                       [:update js-record]]))
          record)
        (let [id (<? (knex/insert db table-name js-record (:db/id fields)))]
          (assoc record :db/id id))))))

;; RELATIONAL MAPPING

(defn has-one [{:keys [::row] :as env} foreign-table local-field]
  (let [foreign-id (row-get env row local-field)]
    (sql-first-node (assoc env ::table foreign-table) [[:where {:db/id foreign-id}]])))

(defn has-many [{:keys [::row] :as env} foreign-table foreign-field & [params]]
  (sql-table-node
    (cond-> (update-in env [:ast :params :where]
                       #(assoc (or % {}) foreign-field (row-get env row :db/id)))
      (:sort params) (update-in [:ast :params :sort] #(or % (:sort params))))
    foreign-table))
