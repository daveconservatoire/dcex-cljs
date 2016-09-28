(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [nodejs.knex :as knex]
            [nodejs.express :as ex]
            [common.async :refer-macros [<? go-catch]]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [clojure.set :refer [rename-keys]]
            [daveconservatoire.models]
            [pathom.core :as p]
            [pathom.sql :as ps]))

;; DATA

(defn topic-watch-count [{:keys [current-user-id] :as env}]
  (go-catch
    (let [cmds [[:count [::ps/f :user-view :db/id]]
                [:from :topic]
                [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                [:left-join :user-view [::knex/call-this
                                        [:on [::ps/f :user-view/lesson-id] "=" [::ps/f :lesson :db/id]]
                                        [:on [::ps/f :user-view/user-id] "=" current-user-id]]]
                [:where {[::ps/f :topic :db/id] (ps/row-get env :db/id)}]]]
      (-> (ps/cached-query env cmds) <? first vals first js/parseInt))))

(defn topic-ex-answer-count [{:keys [current-user-id] :as env}]
  (go-catch
    (let [cmds [[:count [::ps/f :ex-answer :db/id]]
                [:from :topic]
                [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                [:left-join :ex-answer [::knex/call-this
                                        [:on [::ps/f :ex-answer/lesson-id] "=" [::ps/f :lesson :db/id]]
                                        [:on [::ps/f :ex-answer/user-id] "=" current-user-id]]]
                [:where {[::ps/f :topic :db/id] (ps/row-get env :db/id)}]]]
      (-> (ps/cached-query env cmds) <? first vals first js/parseInt))))

(defn topic-started? [{:keys [current-user-id] :as env}]
  (if current-user-id
    (go-catch
      (or (> (<? (topic-watch-count env)) 0)
          (> (<? (topic-ex-answer-count env)) 0)))
    false))

(def schema
  (ps/prepare-schema
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
      ::ps/fields     {:db/id                 "id"
                       :url/slug              "urltitle"
                       :ordering/position     "sortorder"
                       :topic/course-id       "courseId"
                       :topic/title           "title"
                       :topic/colour          "colour"
                       :topic/course          (ps/has-one :course :topic/course-id)
                       :topic/lessons         (ps/has-many :lesson :lesson/topic-id {:sort ["lessonno"]})
                       :topic/watch-count     topic-watch-count
                       :topic/ex-answer-count topic-ex-answer-count
                       :topic/started?        topic-started?}},

     {::ps/table      :course,
      ::ps/table-name "Course",
      ::ps/fields     {:db/id               "id"
                       :url/slug            "urltitle"
                       :course/title        "title"
                       :course/description  "description"
                       :course/author       "author"
                       :course/topics       (ps/has-many :topic :topic/course-id {:sort ["sortorder"]})
                       :course/lessons      (ps/has-many :lesson :lesson/course-id {:sort ["lessonno"]})
                       :course/topics-count (fn [env]
                                              (let [id (ps/row-get env :db/id)]
                                                (go-catch
                                                  (-> (ps/cached-query env [[:count "id"]
                                                                            [:from "Topic"]
                                                                            [:where {:courseid id}]])
                                                      <? first vals first))))
                       :course/home-type    (fn [env]
                                              (go-catch
                                                (if (= (<? (ps/row-get env :course/topics-count))
                                                       1)
                                                  :course.type/single-topic
                                                  :course.type/multi-topic)))

                       :ordering/position   "homepage_order"}},

     {::ps/table      :lesson,
      ::ps/table-name "Lesson",
      ::ps/fields     {:db/id                 "id"
                       :url/slug              "urltitle"
                       :youtube/id            "youtubeid"
                       :lesson/topic-id       "topicno"
                       :lesson/course-id      "seriesno"
                       :lesson/title          "title"
                       :lesson/description    "description"
                       :lesson/keywords       "keywords"
                       :lesson/course         (ps/has-one :course :lesson/course-id)
                       :lesson/topic          (ps/has-one :topic :lesson/topic-id)
                       :lesson/type           (fn [{:keys [::ps/row]}]
                                                (case (get row "filetype")
                                                  "l" :lesson.type/video
                                                  "e" :lesson.type/exercise
                                                  "p" :lesson.type/playlist
                                                  :lesson.type/unknown))
                       :lesson/playlist-items (ps/has-many :playlist-item :playlist-item/lesson-id {:sort "sort"})
                       :lesson/view-state     (fn [{:keys [current-user-id] :as env}]
                                                (if current-user-id
                                                  (case (ps/row-get env :lesson/type)
                                                    :lesson.type/video
                                                    (go-catch
                                                      (if (<? (ps/find-by env {:db/table            :user-view
                                                                               :user-view/lesson-id (ps/row-get env :db/id)
                                                                               :user-view/user-id   current-user-id}))
                                                        :lesson.view-state/viewed))

                                                    :lesson.type/exercise
                                                    (go-catch
                                                      (or
                                                        (if (<? (ps/find-by env {:db/table             :ex-mastery
                                                                                 :ex-mastery/lesson-id (ps/row-get env :db/id)
                                                                                 :ex-mastery/user-id   current-user-id}))
                                                          :lesson.view-state/mastered)
                                                        (if (<? (ps/find-by env {:db/table            :ex-answer
                                                                                 :ex-answer/lesson-id (ps/row-get env :db/id)
                                                                                 :ex-answer/user-id   current-user-id}))
                                                          :lesson.view-state/started)))
                                                    nil)
                                                  nil))}}

     {::ps/table      :user
      ::ps/table-name "User"
      ::ps/fields     {:db/id                     "id"
                       :user/name                 "name"
                       :user/email                "email"
                       :user/about                "biog"
                       :user/created-at           "joinDate"
                       :user/score                "points"
                       :user/last-activity        "lastActivity"
                       :user/user-views           (ps/has-many :user-view :user-view/user-id {:sort ["timestamp" "desc"]})
                       :user/lessons-viewed-count (fn [env]
                                                    (let [id (ps/row-get env :db/id)]
                                                      (ps/count env :user-view [[:where {:user-view/user-id id}]])))}}

     {::ps/table      :user-view
      ::ps/table-name "UserVideoView"
      ::ps/fields     {:db/id               "id"
                       :user-view/user-id   "userId"
                       :user-view/lesson-id "lessonId"
                       :user-view/status    "status"
                       :user-view/position  "position"
                       :user-view/timestamp "timestamp"
                       :user-view/user      (ps/has-one :user :user-view/user-id)
                       :user-view/lesson    (ps/has-one :lesson :user-view/lesson-id)}}

     {::ps/table      :ex-answer
      ::ps/table-name "UserExerciseAnswer"
      ::ps/fields     {:db/id               "id"
                       :ex-answer/user-id   "userId"
                       :ex-answer/lesson-id "exerciseId"
                       :ex-answer/timestamp "timestamp"}}

     {::ps/table      :ex-mastery
      ::ps/table-name "UserExSingleMastery"
      ::ps/fields     {:db/id                "id"
                       :ex-mastery/user-id   "userId"
                       :ex-mastery/lesson-id "exerciseId"
                       :ex-mastery/timestamp "timestamp"}}]))

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

(defn conj-vec [v x]
  (conj (or v []) x))

(defn compute-ex-answer [{:keys [current-user-id http-request] :as env}
                         {:keys [url/slug]}]
  (go-catch
    (let [lesson (<? (ps/find-by env {:db/table :lesson
                                      :url/slug slug}))
          answer {:db/table            :ex-answer
                  :ex-answer/timestamp (current-timestamp)
                  :ex-answer/lesson-id (:db/id lesson)
                  :guest-tx/increase-score 1}]
      (if current-user-id
        (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))]
          (ps/save env (update user :user/score inc))
          (ps/save env (assoc answer :ex-answer/user-id current-user-id)))

        ; save for guest
        (ex/session-update! http-request :guest-tx
          #(conj-vec % answer)))
      true)))

(defn compute-ex-answer-master [{:keys [current-user-id http-request] :as env}
                                {:keys [url/slug]}]
  (go-catch
    (let [lesson (<? (ps/find-by env {:db/table :lesson
                                      :url/slug slug}))
          answer {:db/table             :ex-mastery
                  :ex-mastery/timestamp (current-timestamp)
                  :ex-mastery/lesson-id (:db/id lesson)
                  :guest-tx/increase-score 100}]
      (if current-user-id
        (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))]
          (ps/save env (update user :user/score (partial + 100)))
          (ps/save env (assoc answer :ex-mastery/user-id current-user-id)))

        ; save for guest
        (ex/session-update! http-request :guest-tx
          #(conj-vec % answer)))
      true)))

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

(defmethod mutate 'exercise/score-master
  [env _ data]
  {:action #(compute-ex-answer-master env data)})

;; PARSER

(def parser (om/parser {:read p/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::p/reader root-reader
          ::ps/query-cache (atom {})
          ::ps/schema schema) tx)
      (p/read-chan-values)))
