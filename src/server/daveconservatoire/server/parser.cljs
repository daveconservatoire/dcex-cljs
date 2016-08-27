(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [clojure.set :as set]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs.spec :as s]
            [express.core :as ex]
            [knex.core :as knex]
            [daveconservatoire.models]
            [daveconservatoire.server.lib :as l]))

(def db-specs
  (-> (l/prepare-schema
        [{:key    :playlist-item,
          :name   "PlaylistItem",
          :fields {:db/id                   "id"
                   :youtube/id              "youtubeid"
                   :playlist-item/lesson-id "relid"
                   :playlist-item/title     "title"
                   :playlist-item/text      "text"
                   :playlist-item/credit    "credit"}}

         {:key    :search-term
          :name   "SearchTerm"
          :fields {}}

         {:key    :topic,
          :name   "Topic",
          :fields {:db/id             "id"
                   :url/slug          "urltitle"
                   :ordering/position "sortorder"
                   :topic/course-id   "courseId"
                   :topic/title       "title"
                   :topic/colour      "colour"}},

         {:key    :course,
          :name   "Course",
          :fields {:db/id              "id"
                   :course/title       "title"
                   :course/description "description"
                   :course/author      "author"
                   :url/slug           "urltitle"
                   :ordering/position  "homepage_order"}},

         {:key    :lesson,
          :name   "Lesson",
          :fields {:db/id              "id"
                   :url/slug           "urltitle"
                   :youtube/id         "youtubeid"
                   :lesson/topic-id    "topicno"
                   :lesson/course-id   "seriesno"
                   :lesson/title       "title"
                   :lesson/description "description"
                   :lesson/keywords    "keywords"}}

         {:key    :user
          :name   "User"
          :fields {:db/id      "id"
                   :user/name  "name"
                   :user/email "email"
                   :user/about "biog"}}])

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
   :app/topics     #(l/sql-table-node % :topic)
   :app/me         #(if-let [id (:current-user-id %)]
                     (l/sql-first-node (assoc % :table :user)
                      [[:where {:id id}]]))})

(def root-readers
  [root-endpoints l/placeholder-node #(vector :error :not-found)])

;; MUTATIONS

(defmulti mutate om/dispatch)

(defmethod mutate 'app/logout
  [{:keys [http-request]} _ _]
  {:action (fn [] (.logout http-request))})

;; PARSER

(def parser (om/parser {:read l/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::l/readers root-readers
          :query-cache (atom {})
          :db-specs db-specs) tx)
      (l/read-chan-values)))
