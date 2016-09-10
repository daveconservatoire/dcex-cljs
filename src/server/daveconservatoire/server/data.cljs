(ns daveconservatoire.server.data
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [nodejs.knex :as knex]
            [pathom.sql :as ps]
            [clojure.set :refer [rename-keys]]
            [common.async :refer [go-catch <?]]))

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

(defn current-timestamp []
  (js/Math.round (/ (.getTime (js/Date.)) 1000)))

(defn create-user [connection {:user/keys [name email]}]
  (go-catch
    (let [user {:name         name
                :email        email
                :points       0
                :joinDate     (current-timestamp)
                :lastActivity (current-timestamp)
                :biog         "Please tell us about your musical interests and goals. This will help develop the site to better support your learning. It will not be made public."}
          [id] (<? (knex/insert connection "User" user))]
      id)))

(defn update-current-user [{:keys [current-user-id] :as env} data]
  (if current-user-id
    (let [enabled-keys #{:user/about}]
      (ps/save env (-> (select-keys data enabled-keys)
                       (assoc :db/id current-user-id
                              :db/table :user))))
    (go nil)))

(defn passport-sign-in [{:keys [::ps/db] :as env} {:keys [emails displayName] :as profile}]
  (go-catch
    (let [email (some-> emails first :value)]
      (if-let [user (<? (ps/find-by env {:db/table :user :user/email email}))]
        (:id user)
        (<? (create-user db {:user/name displayName :user/email email}))))))

(defn passport-sign-callback [connection]
  (fn [_ _ profile done]
    (go
      (try
        (done nil (<? (passport-sign-in {::ps/db     connection
                                         ::ps/schema schema} (js->clj profile :keywordize-keys true))))
        (catch :default e
          (done e))))))

(defn hit-video-view [{:keys [::ps/db ::ps/schema] :as env} {:keys [:user-view/user-id :user-view/lesson-id] :as view}]
  (go-catch
    (let [{:keys [::ps/table-name ::ps/fields ::ps/fields']} (get schema :user-view)
          last-view (some-> (knex/query-first db table-name
                                              [[:where {"userId" user-id}]
                                               [:orderBy "timestamp" "desc"]
                                               [:limit 1]])
                            <? (rename-keys fields'))]
      (if (not= lesson-id (:user-view/lesson-id last-view))
        (<? (ps/save env (assoc view :db/table :user-view
                                     :user-view/timestamp (current-timestamp))))))))

(defn compute-ex-answer [{::ps/keys [db db-specs] :as env} {:keys [url/slug]}]
  (go-catch
    (let [lesson (<? (ps/find-by env {:db/table :lesson
                                      :url/slug slug}))])))
