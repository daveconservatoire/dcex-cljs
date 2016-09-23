(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [nodejs.knex :as knex]
            [common.async :refer-macros [<? go-catch]]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [clojure.set :refer [rename-keys]]
            [daveconservatoire.models]
            [pathom.core :as p]
            [pathom.sql :as ps]))

;; DATA

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
                           :user/score         "points"
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
          ::ps/fields     {:db/id               "id"
                           :ex-answer/user-id   "userId"
                           :ex-answer/lesson-id "exerciseId"
                           :ex-answer/timestamp "timestamp"}}])

      ; Course
      (ps/row-getter :course/topics
        #(ps/has-many % :topic :topic/course-id {:sort ["sortorder"]}))
      (ps/row-getter :course/lessons
        #(ps/has-many % :lesson :lesson/course-id {:sort ["lessonno"]}))
      (ps/row-getter :course/topics-count
        (fn [{:keys [::ps/row] :as env}]
          (let [id (ps/row-get env row :db/id)]
            (go-catch
              (-> (ps/cached-query env [[:count "id"]
                                        [:from "Topic"]
                                        [:where {:courseid id}]])
                  <? first vals first)))))
      (ps/row-getter :course/home-type
        (fn [{:keys [::ps/row] :as env}]
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
      (ps/row-getter :topic/started?
        (fn [{:keys [::ps/schema ::ps/row current-user-id] :as env}]
          (if current-user-id
            (go-catch
              (let [watch-count (-> (ps/cached-query env [[:count [::ps/f :user-view :db/id]]
                                                          [:from :topic]
                                                          [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                                                          [:left-join :user-view [::knex/call-this
                                                                                       [:on [::ps/f :user-view/lesson-id] "=" [::ps/f :lesson :db/id]]
                                                                                       [:on [::ps/f :user-view/user-id] "=" current-user-id]]]
                                                          [:where {[::ps/f :topic :db/id] (ps/row-get env row :db/id)}]])
                                    <? first vals first js/parseInt)]
                (> watch-count 0)))
            false)))

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
      (ps/row-getter :lesson/viewed?
        (fn [{:keys [current-user-id ::ps/row] :as env}]
          (if current-user-id
            (go-catch
              (boolean
                (<? (ps/find-by env {:db/table            :user-view
                                     :user-view/lesson-id (ps/row-get env row :db/id)
                                     :user-view/user-id   current-user-id}))))
            false)))

      ; User
      (ps/row-getter :user/user-views
        #(ps/has-many % :user-view :user-view/user-id {:sort ["timestamp" "desc"]}))
      (ps/row-getter :user/lessons-viewed-count
        (fn [{:keys [::ps/row] :as env}]
          (let [id (ps/row-get env row :db/id)]
            (ps/count env :user-view [[:where {:user-view/user-id id}]]))))

      ; User View
      (ps/row-getter :user-view/user
        #(ps/has-one % :user :user-view/user-id))
      (ps/row-getter :user-view/lesson
        #(ps/has-one % :lesson :user-view/lesson-id))))

(defn current-timestamp []
  (js/Math.round (/ (.getTime (js/Date.)) 1000)))

(defn create-user [env {:keys [user/name user/email]}]
  (go-catch
    (let [user {:db/table           :user
                :user/name          name
                :user/email         email
                :user/score         0
                :user/created-at    (current-timestamp)
                :user/last-activity (current-timestamp)
                :user/about         "Please tell us about your musical interests and goals. This will help develop the site to better support your learning. It will not be made public."}
          {:keys [db/id]} (<? (ps/save env user))]
      id)))

(defn update-current-user [{:keys [current-user-id] :as env} data]
  (if current-user-id
    (let [enabled-keys #{:user/about}]
      (ps/save env (-> (select-keys data enabled-keys)
                       (assoc :db/id current-user-id
                              :db/table :user))))
    (go nil)))

(defn passport-sign-in [env {:keys [emails displayName]}]
  (go-catch
    (let [email (some-> emails first :value)]
      (if-let [user (<? (ps/find-by env {:db/table :user :user/email email}))]
        (:db/id user)
        (<? (create-user env {:user/name displayName :user/email email}))))))

(defn passport-sign-callback [connection]
  (fn [_ _ profile done]
    (go
      (try
        (done nil (<? (passport-sign-in {::ps/db     connection
                                         ::ps/schema schema} (js->clj profile :keywordize-keys true))))
        (catch :default e
          (done e))))))

(defn hit-video-view [env {:keys [:user-view/user-id :user-view/lesson-id] :as view}]
  (go-catch
    (let [last-view (<? (ps/find-by env {:db/table          :user-view
                                         :user-view/user-id user-id
                                         ::ps/query         [[:orderBy :user-view/timestamp "desc"]]}))]
      (if (not= lesson-id (:user-view/lesson-id last-view))
        (<? (ps/save env (assoc view :db/table :user-view
                                     :user-view/timestamp (current-timestamp))))))))

(defn compute-ex-answer [{:keys [current-user-id] :as env}
                         {:keys [url/slug]}]
  (if current-user-id
    (go-catch
      (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))
            lesson (<? (ps/find-by env {:db/table :lesson
                                        :url/slug slug}))]
        (ps/save env (update user :user/score inc))
        (ps/save env {:db/table            :ex-answer
                      :ex-answer/timestamp (current-timestamp)
                      :ex-answer/user-id   current-user-id
                      :ex-answer/lesson-id (:db/id lesson)})
        true))
    (go nil)))

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
         (<? (hit-video-view env {:user-view/user-id   current-user-id
                                  :user-view/lesson-id id}))
         nil)))})

(defmethod mutate 'user/update
  [env _ data]
  {:action #(update-current-user env data)})

(defmethod mutate 'exercise/score
  [env _ data]
  {:action #(compute-ex-answer env data)})

;; PARSER

(def parser (om/parser {:read p/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::p/reader root-reader
          ::ps/query-cache (atom {})
          ::ps/schema schema) tx)
      (p/read-chan-values)))
