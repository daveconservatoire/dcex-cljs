(ns daveconservatoire.server.lib
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [knex.core :as knex]
            [cljs.spec :as s]))

;; SUPPORT FUNCTIONS

(defn union-children? [ast]
  (= :union (some-> ast :children first :type)))

(defn ast-key-id [ast] (some-> ast :key second))

(defn chan? [v] (satisfies? Channel v))

(defn resolved-chan [v]
  (let [c (async/promise-chan)]
    (put! c v)
    c))

(defn read-chan-values [m]
  (if (first (filter chan? (vals m)))
    (let [c (async/promise-chan)
          in (async/to-chan m)]
      (go-loop [out {}]
        (if-let [[k v] (<! in)]
          (recur (assoc out k (if (chan? v) (<! v) v)))
          (>! c out)))
      c)
    (resolved-chan m)))

(defn read-chan-seq [f s]
  (go
    (let [out (async/chan 64)]
      (async/pipeline-async 10 out
                            (fn [in c]
                              (go
                                (let [in (if (chan? in) (<! in) in)
                                      out (f in)]
                                  (>! c (if (chan? out) (<! out) out)))
                                (close! c)))
                            (async/to-chan s))
      (<! (async/into [] out)))))

(s/def ::reader (s/or :fn (s/and fn?
                                 (s/fspec :args (s/cat :env any?)
                                          :ret any?))
                      :map (s/map-of keyword? ::reader)
                      :list (s/coll-of ::reader)))

(defn read-from* [{:keys [ast] :as env} reader]
  (let [k (:dispatch-key ast)]
    (cond
      (fn? reader) (reader env)
      (map? reader) (if-let [[_ v] (find reader k)]
                      (read-from* env v)
                      ::continue)
      (vector? reader) (let [res (into [] (comp (map #(read-from* env %))
                                                (remove #{::continue})
                                                (take 1))
                                          reader)]
                         (if (seq res)
                           (first res)
                           ::continue)))))

(defn read-from [env reader]
  (let [res (read-from* env reader)]
    (if (= res ::continue) nil res)))

;; NODE HELPERS

(defn placeholder-node [{:keys [ast parser] :as env}]
  (if (= "ph" (namespace (:dispatch-key ast)))
    (read-chan-values (parser env (:query ast)))
    ::continue))

;;; DB PART

(defn prepare-schema [schema]
  (zipmap (keys schema)
          (map #(let [m (:fields %)]
                 (assoc %
                   ::reader-map
                   (zipmap (keys m)
                           (map
                             (fn [field]
                               (let [field (keyword field)]
                                 (fn [{:keys [row]}]
                                   (get row field))))
                             (vals m)))
                   :fields' (zipmap (map keyword (vals m)) (keys m))))
               (vals schema))))

(defn row-getter [schema k f]
  (let [table (keyword (namespace k))]
    (assoc-in schema [table ::reader-map k] f)))

(defn row-get [{:keys [table] :as env} row attr]
  (read-from (assoc env :row row :ast {:key attr :dispatch-key attr})
    (::reader-map table)))

(defn parse-row [{:keys [table ast ::union-selector parser] :as env} row]
  (let [row' {:db/table (:key table) :db/id (row-get env row :db/id)}
        query (if (union-children? ast)
                (some-> ast :query (get (row-get env row union-selector)))
                (:query ast))]
    (if query
      (-> (merge
            row'
            (parser (assoc env :row row) query))
          (read-chan-values))
      row')))

(defn cached-query [{:keys [db query-cache]} name cmds]
  (go
    (let [cache-key [name cmds]]
      (if (contains? @query-cache cache-key)
        (get @query-cache cache-key)
        (let [res (<! (knex/query db name cmds))]
          (swap! query-cache assoc cache-key res)
          res)))))

(defn sql-node [{:keys [table db-specs] :as env} cmds]
  (if-let [{:keys [name fields ::reader-map] :as table-spec} (get db-specs table)]
    (go
      (let [cmds (map (fn [[type v :as cmd]]
                        (if (= type :where)
                          [type (set/rename-keys v fields)]
                          cmd)) cmds)
            rows (<! (cached-query env name cmds))
            env (assoc env :table table-spec
                           ::readers [reader-map placeholder-node])]
        (<! (read-chan-seq #(parse-row env %) rows))))
    (throw (str "[Query SQL] No specs for table " table))))

(defn sql-first-node [env cmds]
  (go
    (-> (sql-node env cmds) <!
        (first)
        (or [:error :row-not-found]))))

(defn- ensure-list [x]
  (if (sequential? x) x [x]))

(defn sql-table-node
  [{:keys [ast db-specs] :as env} table]
  (if (get db-specs table)
    (let [{:keys [limit where sort]} (:params ast)
          limit (or limit 50)]
      (sql-node (assoc env :table table)
                (cond-> [[:limit limit]]
                   where (conj [:where where])
                   sort (conj (concat [:orderBy] (ensure-list sort))))))
    (throw (str "[Query Table] No specs for table " table))))

;; RELATIONAL MAPPING

(defn has-one [{:keys [row] :as env} foreign-table local-field]
  (let [foreign-id (row-get env row local-field)]
    (sql-first-node (assoc env :table foreign-table) [[:where {:db/id foreign-id}]])))

(defn has-many [{:keys [row] :as env} foreign-table local-field & [params]]
  (sql-table-node
    (cond-> (update-in env [:ast :params :where]
                       #(assoc (or % {}) local-field (row-get env row :db/id)))
      (:sort params) (update-in [:ast :params :sort] #(or % (:sort params))))
    foreign-table))

;; PARSER

(defn read [{:keys [::readers] :as env} _ _]
  {:value
   (read-from env readers)})

(def parser (om/parser {:read read}))
