(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [knex.core :as knex]
            [daveconservatoire.models]
            [cljs.spec :as s]
            [daveconservatoire.server.lib :as l]))

(def db-specs
  (-> (l/prepare-schema
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

      ; Course
      (l/row-getter :course/topics
        #(l/has-many % :topic :topic/course-id {:sort ["sortorder"]}))
      (l/row-getter :course/lessons
        #(l/has-many % :lesson :lesson/course-id {:sort ["lessonno"]}))

      ; Topic
      (l/row-getter :topic/course
        #(l/has-one % :course :topic/course-id))
      (l/row-getter :topic/lessons
        #(l/has-many % :lesson :lesson/topic-id {:sort ["lessonno"]}))

      ; Lesson
      (l/row-getter :lesson/course
        #(l/has-one % :course :lesson/course-id))
      (l/row-getter :lesson/topic
        #(l/has-one % :topic :lesson/topic-id))
      (l/row-getter :lesson/type
        #(case (get-in % [:row :filetype])
          "l" :lesson.type/video
          "e" :lesson.type/exercise
          "p" :lesson.type/playlist))
      (l/row-getter :lesson/playlist-items
        #(l/has-many % :playlist-item :playlist-item/lesson-id {:sort "sort"}))))

;; ROOT READS

(def root-endpoints
  {:route/data     #(l/read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(l/sql-first-node (assoc % :table :topic)
                                      [[:where {:urltitle (l/ast-key-id (:ast %))}]])
   :lesson/by-slug #(l/sql-first-node (assoc % :table :lesson ::l/union-selector :lesson/type)
                                      [[:where {:urltitle (l/ast-key-id (:ast %))}]])
   :app/courses    #(l/sql-table-node (assoc-in % [:ast :params :sort] "homepage_order") :course)
   :app/topics     #(l/sql-table-node % :topic)})

(defn read [{:keys [query-cache] :as env} _ _]
  (let [env (if query-cache env (assoc env :query-cache (atom {})
                                           :db-specs db-specs))]
    {:value
     (l/read-from env
       [root-endpoints
        l/placeholder-node
        #(vector :error :not-found)])}))

(def parser (om/parser {:read read}))

(defn parse [env tx]
  (-> (parser env tx) (l/read-chan-values)))
