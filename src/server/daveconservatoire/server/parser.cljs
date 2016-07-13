(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [knex.core :as knex]
            [daveconservatoire.models]
            [cljs.spec :as s]))

;; SUPPORT FUNCTIONS

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
                                 (s/fspec :args (s/cat :env ::s/any)
                                          :ret ::s/any))
                      :map (s/map-of keyword? ::reader)
                      :list (s/coll-of ::reader [])))

(defn placeholder-node [{:keys [ast parser] :as env}]
  (if (= "ph" (namespace (:dispatch-key ast)))
    (read-chan-values (parser env (:query ast)))
    ::continue))

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

;;; DB PART

(defn prepare-schema [schema]
  (zipmap (keys schema)
          (map #(let [m (:fields %)]
                 (assoc %
                   :fields-getters
                   (zipmap (keys m)
                           (map
                             (fn [field]
                               (let [field (keyword field)]
                                 (fn [{:keys [row]}]
                                   (get row field))))
                             (vals m)))
                   :fields' (zipmap (map keyword (vals m)) (keys m))))
               (vals schema))))

(declare has-many)
(declare has-one)

(defn row-getter [schema k f]
  (let [table (keyword (namespace k))]
    (assoc-in schema [table :fields-getters k] f)))

(def db-specs
  (-> (prepare-schema
        {:playlist-item
         {:key    :playlist-item,
          :name   "PlaylistItem",
          :fields {:db/id                   "id"
                   :youtube/id              "youtubeid"
                   :playlist-item/lesson-id "relid"
                   :playlist-item/title     "title"
                   :playlist-item/text      "text"
                   :playlist-item/credit    "credit"}}

         :search-term
         {:key    :search-term
          :name   "SearchTerm"
          :fields {}}

         :topic
         {:key    :topic,
          :name   "Topic",
          :fields {:db/id             "id"
                   :url/slug          "urltitle"
                   :ordering/position "sortorder"
                   :topic/course-id   "courseId"
                   :topic/title       "title"
                   :topic/colour      "colour"}},

         :course
         {:key    :course,
          :name   "Course",
          :fields {:db/id              "id"
                   :course/title       "title"
                   :course/description "description"
                   :course/author      "author"
                   :url/slug           "urltitle"
                   :ordering/position  "homepage_order"}},

         :lesson
         {:key    :lesson,
          :name   "Lesson",
          :fields {:db/id              "id"
                   :url/slug           "urltitle"
                   :youtube/id         "youtubeid"
                   :lesson/topic-id    "topicno"
                   :lesson/course-id   "seriesno"
                   :lesson/title       "title"
                   :lesson/description "description"
                   :lesson/keywords    "keywords"}}})
      (row-getter :course/topics
        #(has-many % :topic :topic/course-id {:sort ["sortorder"]}))
      (row-getter :course/lessons
        #(has-many % :lesson :lesson/course-id {:sort ["lessonno"]}))
      (row-getter :topic/course
        #(has-one % :course :topic/course-id))
      (row-getter :topic/lessons
        #(has-many % :lesson :lesson/topic-id {:sort ["lessonno"]}))
      (row-getter :lesson/course
        #(has-one % :course :lesson/course-id))
      (row-getter :lesson/topic
        #(has-one % :topic :lesson/topic-id))
      (row-getter :lesson/type
        #(case (get-in % [:row :filetype])
          "l" :lesson.type/video
          "e" :lesson.type/exercise
          "p" :lesson.type/playlist))
      (row-getter :lesson/playlist-items
        #(has-many % :playlist-item :playlist-item/lesson-id {:sort "sort"}))))

(defn union-children? [ast]
  (= :union (some-> ast :children first :type)))

(defn row-get [{:keys [table] :as env} row attr]
  (read-from (assoc env :row row :ast {:key attr :dispatch-key attr})
    (:fields-getters table)))

(defn parse-row [{:keys [table ast ::union-selector] :as env} row]
  (let [row' {:db/table (:key table) :db/id (row-get env row :db/id)}
        readers [(:fields-getters table) placeholder-node]
        ast (if (union-children? ast)
              (some-> ast :query (get (row-get env row union-selector)) (om/query->ast))
              ast)]
    (-> (reduce (fn [row' {:keys [key] :as ast}]
                  (assoc row' key (read-from (assoc env :ast ast :row row) readers)))
                row'
                (:children ast))
        (read-chan-values))))

(defn cached-query [{:keys [db query-cache]} name cmds]
  (go
    (let [cache-key [name cmds]]
      (if (contains? @query-cache cache-key)
        (get @query-cache cache-key)
        (let [res (<! (knex/query db name cmds))]
          (swap! query-cache assoc cache-key res)
          res)))))

(defn query-sql [{:keys [table] :as env} cmds]
  (if-let [{:keys [name fields] :as table-spec} (get db-specs table)]
    (go
      (let [cmds (map (fn [[type v :as cmd]]
                        (if (= type :where)
                          [type (set/rename-keys v fields)]
                          cmd)) cmds)
            rows (<! (cached-query env name cmds))
            env (assoc env :table table-spec)]
        (<! (read-chan-seq #(parse-row env %) rows))))
    (throw (str "[Query SQL] No specs for table " table))))

(defn query-sql-first [env cmds]
  (go
    (-> (query-sql env cmds) <!
        (first)
        (or [:error :row-not-found]))))

(defn- ensure-list [x]
  (if (sequential? x) x [x]))

(defn query-table
  [{:keys [ast] :as env} table]
  (if-let [{:keys [name]} (get db-specs table)]
    (let [{:keys [limit where sort]} (:params ast)
          limit (or limit 50)]
      (query-sql (assoc env :table table)
                 (cond-> [[:limit limit]]
                   where (conj [:where where])
                   sort (conj (concat [:orderBy] (ensure-list sort))))))
    (throw (str "[Query Table] No specs for table " table))))

;; RELATIONAL MAPPING

(defn has-one [{:keys [row] :as env} foreign-table local-field]
  (let [foreign-id (row-get env row local-field)]
    (query-sql-first (assoc env :table foreign-table) [[:where {:db/id foreign-id}]])))

(defn has-many [{:keys [row] :as env} foreign-table local-field & [params]]
  (query-table
    (cond-> (update-in env [:ast :params :where]
                       #(assoc (or % {}) local-field (row-get env row :db/id)))
      (:sort params) (update-in [:ast :params :sort] #(or % (:sort params))))
    foreign-table))

;; ROOT READS

(defn ast-key-id [ast] (some-> ast :key second))

(def root-endpoints
  {:route/data     #(read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(query-sql-first (assoc % :table :topic)
                                     [[:where {:urltitle (ast-key-id (:ast %))}]])
   :lesson/by-slug #(query-sql-first (assoc % :table :lesson ::union-selector :lesson/type)
                                     [[:where {:urltitle (ast-key-id (:ast %))}]])
   :app/courses    #(query-table (assoc-in % [:ast :params :sort] "homepage_order") :course)
   :app/topics     #(query-table % :topic)})

(defn read [{:keys [query-cache] :as env} _ _]
  (let [env (if query-cache env (assoc env :query-cache (atom {})))]
    {:value
     (read-from env
       [root-endpoints
        placeholder-node
        #(vector :error :not-found)])}))

(def parser (om/parser {:read read}))

(defn parse [env tx]
  (-> (parser env tx) (read-chan-values)))
