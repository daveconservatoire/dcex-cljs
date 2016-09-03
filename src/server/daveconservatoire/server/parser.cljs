(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [common.async :refer-macros [<? go-catch]]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [daveconservatoire.models]
            [daveconservatoire.server.lib :as l]
            [daveconservatoire.server.data :as d]))

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
                   :user/about "biog"}}

         {:key    :user-view
          :name   "UserVideoView"
          :fields (assoc #:user-view {:user-id   "userId"
                                      :lesson-id "lessonId"
                                      :status    "status"
                                      :position  "position"
                                      :timestamp "timestamp"}
                                     :db/id "id")}])

      ; Course
      (l/row-getter :course/topics
        #(l/has-many % :topic :topic/course-id {:sort ["sortorder"]}))
      (l/row-getter :course/lessons
        #(l/has-many % :lesson :lesson/course-id {:sort ["lessonno"]}))
      (l/row-getter :course/topics-count
        (fn [{:keys [row] :as env}]
          (let [{:keys [id]} row]
            (go-catch
              (-> (l/cached-query env "Topic" [[:count "id"]
                                               [:where {:courseid id}]])
                  <? first vals first)))))
      (l/row-getter :course/home-type
        (fn [{:keys [row] :as env}]
          (go-catch
            (if (= (<? (l/row-get env row :course/topics-count))
                   1)
              :course.type/single-topic
              :course.type/multi-topic))))

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

(defn ast-sort [env sort]
  (assoc-in env [:ast :params :sort] sort))

(def root-endpoints
  {:route/data     #(l/read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(l/sql-first-node (assoc % :table :topic)
                     [[:where {:urltitle (l/ast-key-id (:ast %))}]])
   :lesson/by-slug #(l/sql-first-node (assoc % :table :lesson ::l/union-selector :lesson/type)
                     [[:where {:urltitle (l/ast-key-id (:ast %))}]])
   :app/courses    #(l/sql-table-node (-> (ast-sort % "homepage_order")
                                          (assoc ::l/union-selector :course/home-type)) :course)
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

(defmethod mutate 'lesson/save-view
  [{:keys [current-user-id] :as env} _ {:keys [db/id]}]
  {:action
   (fn []
     (go
       (when current-user-id
         (<? (d/hit-video-view env #:user-view {:user-id   current-user-id
                                             :lesson-id id}))
         nil)))})

;; PARSER

(def parser (om/parser {:read l/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::l/readers root-readers
          ::l/query-cache (atom {})
          ::l/db-specs db-specs) tx)
      (l/read-chan-values)))
