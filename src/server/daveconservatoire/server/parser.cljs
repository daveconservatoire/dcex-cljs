(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [knex.core :as knex]
            [daveconservatoire.models]))

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

;;; DB PART

(def db-specs
  (let [specs
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
                   :lesson/keywords    "keywords"}}}]
    (zipmap (keys specs)
            (map #(assoc % :fields'
                           (let [m (:fields %)] (zipmap (map keyword (vals m)) (keys m))))
                 (vals specs)))))

(defmulti row-vattribute (fn [env] (get-in env [:ast :key])))

(defmethod row-vattribute :default [env] [:error :not-found])

(defn union-children? [ast]
  (= :union (some-> ast :children first :type)))

(defn row-get [{:keys [table] :as env} row attr]
  (if-let [field (get (:fields table) attr)]
    (get row field)
    (row-vattribute (assoc env :ast {:key attr} :row row))))

(defn parse-row [{:keys [table ast ::union-selector] :as env} row]
  (let [row (assoc row :db/table (:key table))
        ast (if (union-children? ast)
              (some-> ast :query (get (row-get env row union-selector)) (om/query->ast))
              ast)
        accessors (into #{:db/id :db/table} (map :key) (:children ast))
        non-table (set/difference accessors (set (conj (keys (:fields table)) :db/table)))
        virtual (filter #(contains? non-table (:key %)) (:children ast))
        row (set/rename-keys row (:fields' table))]
    (-> (reduce (fn [row {:keys [key] :as ast}]
                  (assoc row key (row-vattribute (assoc env :ast ast :row row))))
                row
                virtual)
        (select-keys accessors)
        (read-chan-values))))

(defn query-sql [{:keys [table db] :as env} cmds]
  (if-let [{:keys [name fields] :as table-spec} (get db-specs table)]
    (go
      (let [cmds (map (fn [[type v :as cmd]]
                        (if (= type :where)
                          [type (set/rename-keys v fields)]
                          cmd)) cmds)
            rows (<! (knex/query db name cmds))
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

(defn has-one [env foreign-table local-field]
  (let [foreign-id (get-in env [:row local-field])]
    (query-sql-first (assoc env :table foreign-table) [[:where {:db/id foreign-id}]])))

(defn has-many [{:keys [row] :as env} foreign-table local-field & [params]]
  (query-table
    (cond-> (update-in env [:ast :params :where]
                       #(assoc (or % {}) local-field (:db/id row)))
      (:sort params) (update-in [:ast :params :sort] #(or % (:sort params))))
    foreign-table))

(defmethod row-vattribute :course/topics [env] (has-many env :topic :topic/course-id {:sort ["sortorder"]}))
(defmethod row-vattribute :course/lessons [env] (has-many env :lesson :lesson/course-id {:sort ["lessonno"]}))

(defmethod row-vattribute :topic/course [env] (has-one env :course :topic/course-id))
(defmethod row-vattribute :topic/lessons [env] (has-many env :lesson :lesson/topic-id {:sort ["lessonno"]}))

(defmethod row-vattribute :lesson/course [env] (has-one env :course :lesson/course-id))
(defmethod row-vattribute :lesson/topic [env] (has-one env :topic :lesson/topic-id))
(defmethod row-vattribute :lesson/type [env]
  (case (get-in env [:row :filetype])
    "l" :lesson.type/video
    "e" :lesson.type/exercise
    "p" :lesson.type/playlist))

(defmethod row-vattribute :lesson/playlist-items [env] (has-many env :playlist-item :playlist-item/lesson-id {:sort "sort"}))

;; ROOT READS

(defn ast-key-id [ast] (some-> ast :key second))

(defn read [{:keys [ast parser] :as env} key params]
  (case key
    :route/data {:value (read-chan-values (parser env (:query ast)))}
    :topic/by-slug {:value (query-sql-first (assoc env :table :topic)
                                            [[:where {:urltitle (ast-key-id ast)}]])}
    :lesson/by-slug {:value (query-sql-first (assoc env :table :lesson ::union-selector :lesson/type)
                                             [[:where {:urltitle (ast-key-id ast)}]])}
    :app/courses {:value (query-table (assoc-in env [:ast :params :sort] "homepage_order") :course)}
    :app/topics {:value (query-table env :topic)}

    {:value [:error :not-found]}))

(def parser (om/parser {:read read}))

(defn parse [env tx]
  (-> (parser env tx) (read-chan-values)))
