(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [common.async :refer-macros [<? go-catch]]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [daveconservatoire.models]
            [pathom.core :as p]
            [pathom.sql :as ps]
            [daveconservatoire.server.data :as d]))

(def schema
  (-> (ps/prepare-schema
        [{::ps/table      :playlist-item,
          ::ps/table-name "PlaylistItem",
          ::ps/fields     {:db/id                   "id"
                           :youtube/id              "youtubeid"
                           :playlist-item/lesson-id "relid"
                           :playlist-item/title     "title"
                           :playlist-item/text      "text"
                           :playlist-item/credit    "credit"}}

         {::ps/table      :search-term
          ::ps/table-name "SearchTerm"
          ::ps/fields     {}}

         {::ps/table      :topic,
          ::ps/table-name "Topic",
          ::ps/fields     {:db/id             "id"
                           :url/slug          "urltitle"
                           :ordering/position "sortorder"
                           :topic/course-id   "courseId"
                           :topic/title       "title"
                           :topic/colour      "colour"}},

         {::ps/table      :course,
          ::ps/table-name "Course",
          ::ps/fields     {:db/id              "id"
                           :url/slug           "urltitle"
                           :course/title       "title"
                           :course/description "description"
                           :course/author      "author"
                           :ordering/position  "homepage_order"}},

         {::ps/table      :lesson,
          ::ps/table-name "Lesson",
          ::ps/fields     {:db/id              "id"
                           :url/slug           "urltitle"
                           :youtube/id         "youtubeid"
                           :lesson/topic-id    "topicno"
                           :lesson/course-id   "seriesno"
                           :lesson/title       "title"
                           :lesson/description "description"
                           :lesson/keywords    "keywords"}}

         {::ps/table      :user
          ::ps/table-name "User"
          ::ps/fields     {:db/id              "id"
                           :user/name          "name"
                           :user/email         "email"
                           :user/about         "biog"
                           :user/created-at    "joinDate"
                           :user/points        "points"
                           :user/last-activity "lastActivity"}}

         {::ps/table      :user-view
          ::ps/table-name "UserVideoView"
          ::ps/fields     {:db/id               "id"
                           :user-view/user-id   "userId"
                           :user-view/lesson-id "lessonId"
                           :user-view/status    "status"
                           :user-view/position  "position"
                           :user-view/timestamp "timestamp"}}

         {::ps/table      :ex-answer
          ::ps/table-name "UserExerciseAnswer"
          ::ps/fields     {:db/id     "id"
                           :lesson-id "exerciseId"}}])

      ; Course
      (ps/row-getter :course/topics
        #(ps/has-many % :topic :topic/course-id {:sort ["sortorder"]}))
      (ps/row-getter :course/lessons
        #(ps/has-many % :lesson :lesson/course-id {:sort ["lessonno"]}))
      (ps/row-getter :course/topics-count
        (fn [{:keys [row] :as env}]
          (let [{:keys [id]} row]
            (go-catch
              (-> (ps/cached-query env "Topic" [[:count "id"]
                                                [:where {:courseid id}]])
                  <? first vals first)))))
      (ps/row-getter :course/home-type
        (fn [{:keys [row] :as env}]
          (go-catch
            (if (= (<? (ps/row-get env row :course/topics-count))
                   1)
              :course.type/single-topic
              :course.type/multi-topic))))

      ; Topic
      (ps/row-getter :topic/course
        #(ps/has-one % :course :topic/course-id))
      (ps/row-getter :topic/lessons
        #(ps/has-many % :lesson :lesson/topic-id {:sort ["lessonno"]}))

      ; Lesson
      (ps/row-getter :lesson/course
        #(ps/has-one % :course :lesson/course-id))
      (ps/row-getter :lesson/topic
        #(ps/has-one % :topic :lesson/topic-id))
      (ps/row-getter :lesson/type
        #(case (get-in % [::ps/row "filetype"])
          "l" :lesson.type/video
          "e" :lesson.type/exercise
          "p" :lesson.type/playlist
          :lesson.type/unknown))
      (ps/row-getter :lesson/playlist-items
        #(ps/has-many % :playlist-item :playlist-item/lesson-id {:sort "sort"}))

      ; User
      (ps/row-getter :user/user-views
        #(ps/has-many % :user-view :user-view/user-id))

      ; User View
      (ps/row-getter :user-view/user
        #(ps/has-one % :user :user-view/user-id))
      (ps/row-getter :user-view/lesson
        #(ps/has-one % :lesson :user-view/lesson-id))))

;; ROOT READS

(defn ast-sort [env sort]
  (assoc-in env [:ast :params :sort] sort))

(def root-endpoints
  {:route/data     #(p/read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(ps/sql-first-node (assoc % ::ps/table :topic)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :lesson/by-slug #(ps/sql-first-node (assoc % ::ps/table :lesson ::ps/union-selector :lesson/type)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :app/courses    #(ps/sql-table-node (-> (ast-sort % "homepage_order")
                                           (assoc ::ps/union-selector :course/home-type)) :course)
   :app/me         #(if-let [id (:current-user-id %)]
                     (ps/sql-first-node (assoc % ::ps/table :user)
                                        [[:where {:id id}]]))})

(def root-reader
  [root-endpoints p/placeholder-node #(vector :error :not-found)])

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

(defmethod mutate 'user/update
  [env _ data]
  {:action #(d/update-current-user env data)})

;; PARSER

(def parser (om/parser {:read p/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::p/reader root-reader
          ::ps/query-cache (atom {})
          ::ps/schema schema) tx)
      (p/read-chan-values)))
