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
            [pathom.sql :as ps]
            [cljs.spec.alpha :as s]))

(s/def :user-activity/type #{"answer" "mastery" "view"})

;; DATA

(defn topic-watch-count [{:keys [current-user-id] :as env}]
  (go-catch
    (let [cmds [[:count [::ps/f :user-activity :db/id]]
                [:from :topic]
                [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                [:left-join :user-activity [::knex/call-this
                                            [:on [::ps/f :user-activity/lesson-id] "=" [::ps/f :lesson :db/id]]
                                            [:on [::ps/f :user-activity/user-id] "=" current-user-id]]]
                [:where {[::ps/f :topic :db/id]       (ps/row-get env :db/id)
                         [::ps/f :user-activity/type] "view"}]]]
      (-> (ps/cached-query env cmds) <? first vals first js/parseInt))))

(defn topic-ex-answer-count [{:keys [current-user-id] :as env}]
  (go-catch
    (let [cmds [[:count [::ps/f :user-activity :db/id]]
                [:from :topic]
                [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                [:left-join :user-activity [::knex/call-this
                                            [:on [::ps/f :user-activity/lesson-id] "=" [::ps/f :lesson :db/id]]
                                            [:on [::ps/f :user-activity/user-id] "=" current-user-id]]]
                [:where {[::ps/f :topic :db/id]       (ps/row-get env :db/id)
                         [::ps/f :user-activity/type] "answer"}]]]
      (-> (ps/cached-query env cmds) <? first vals first js/parseInt))))

(defn topic-activity-count [{:keys [current-user-id] :as env}]
  (go-catch
    (let [cmds [[:count [::ps/f :user-activity :db/id]]
                [:from :topic]
                [:left-join :lesson [::ps/f :lesson :lesson/topic-id] [::ps/f :topic :db/id]]
                [:left-join :user-activity [::knex/call-this
                                            [:on [::ps/f :user-activity/lesson-id] "=" [::ps/f :lesson :db/id]]
                                            [:on [::ps/f :user-activity/user-id] "=" current-user-id]]]
                [:where {[::ps/f :topic :db/id] (ps/row-get env :db/id)}]]]
      (-> (ps/cached-query env cmds) <? first vals first js/parseInt))))

(defn topic-started? [{:keys [current-user-id] :as env}]
  (if current-user-id
    (go-catch
      (> (<? (topic-activity-count env)) 0))
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
                       :lesson/order          "lessonno"
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
                                                      (if (<? (ps/find-by env {:db/table                :user-activity
                                                                               :user-activity/type      "view"
                                                                               :user-activity/lesson-id (ps/row-get env :db/id)
                                                                               :user-activity/user-id   current-user-id}))
                                                        :lesson.view-state/viewed))

                                                    :lesson.type/exercise
                                                    (go-catch
                                                      (or
                                                        (if (<? (ps/find-by env {:db/table                :user-activity
                                                                                 :user-activity/type      "mastery"
                                                                                 :user-activity/lesson-id (ps/row-get env :db/id)
                                                                                 :user-activity/user-id   current-user-id}))
                                                          :lesson.view-state/mastered)
                                                        (if (<? (ps/find-by env {:db/table                :user-activity
                                                                                 :user-activity/type      "answer"
                                                                                 :user-activity/lesson-id (ps/row-get env :db/id)
                                                                                 :user-activity/user-id   current-user-id}))
                                                          :lesson.view-state/started)))
                                                    nil)
                                                  nil))
                       :lesson/prev           (fn [env]
                                                (go-catch
                                                  (let [res (<? (ps/sql-first-node env [[:where :lesson/order "<" (ps/row-get env :lesson/order)]
                                                                                        [:and-where {:lesson/topic-id (ps/row-get env :lesson/topic-id)}]
                                                                                        [:order-by :lesson/order "desc"]
                                                                                        [:limit 1]]))]
                                                    (if (not= res [:error :row-not-found]) res))))
                       :lesson/next           (fn [env]
                                                (go-catch
                                                  (let [res (<? (ps/sql-first-node env [[:where :lesson/order ">" (ps/row-get env :lesson/order)]
                                                                                        [:and-where {:lesson/topic-id (ps/row-get env :lesson/topic-id)}]
                                                                                        [:order-by :lesson/order]
                                                                                        [:limit 1]]))]
                                                    (if (not= res [:error :row-not-found]) res))))}}

     {::ps/table      :user
      ::ps/table-name "User"
      ::ps/fields     {:db/id                     "id"
                       :user/name                 "name"
                       :user/email                "email"
                       :user/about                "biog"
                       :user/created-at           "joinDate"
                       :user/score                "points"
                       :user/last-activity        "lastActivity"
                       :user/subscription-amount  "subamount"
                       :user/subscription-updated "subupdated"
                       :user/user-views           (ps/has-many :user-activity :user-activity/user-id {:sort ["timestamp" "desc"] :where {:user-activity/type "view"}})
                       :user/ex-answers           (ps/has-many :user-activity :user-activity/user-id {:sort ["timestamp" "desc"] :where {:user-activity/type "answer"}})
                       :user/ex-masteries         (ps/has-many :user-activity :user-activity/user-id {:sort ["timestamp" "desc"] :where {:user-activity/type "mastery"}})
                       :user/activity             (ps/has-many :user-activity :user-activity/user-id {:sort ["timestamp" "desc"]})
                       :user/lessons-viewed-count (fn [env]
                                                    (let [id (ps/row-get env :db/id)]
                                                      (ps/count env :user-activity [[:where {:user-activity/user-id id
                                                                                             :user-activity/type    "view"}]])))
                       :user/ex-answer-count      (fn [env]
                                                    (go-catch
                                                      (let [id (ps/row-get env :db/id)]
                                                        (<? (ps/count env :user-activity [[:where-in :user-activity/type ["answer" "mastery"]]
                                                                                          [:and-where {:user-activity/user-id id}]])))))}}

     {::ps/table      :user-activity
      ::ps/table-name "UserActivity"
      ::ps/fields     {:db/id                    "id"
                       :db/timestamp             "timestamp"
                       :user-activity/user-id    "userId"
                       :user-activity/lesson-id  "lessonId"
                       :user-activity/status     "status"
                       :user-activity/position   "position"
                       :user-activity/type       "type"
                       :user-activity/user       (ps/has-one :user :user-activity/user-id)
                       :user-activity/lesson     (ps/has-one :lesson :user-activity/lesson-id)
                       :user-activity/hints-used "countHints"
                       :activity/lesson          (ps/has-one :lesson :user-activity/lesson-id)}}

     {::ps/table      :user-view
      ::ps/table-name "UserVideoView"
      ::ps/fields     {:db/id                   "id"
                       :db/timestamp            "timestamp"
                       :user-activity/user-id   "userId"
                       :user-activity/lesson-id "lessonId"
                       :user-activity/status    "status"
                       :user-activity/position  "position"}}

     {::ps/table      :ex-answer
      ::ps/table-name "UserExerciseAnswer"
      ::ps/fields     {:db/id                   "id"
                       :db/timestamp            "timestamp"
                       :user-activity/user-id   "userId"
                       :user-activity/lesson-id "exerciseId"
                       :user-activity/hints-used "countHints"}}

     {::ps/table      :ex-mastery
      ::ps/table-name "UserExSingleMastery"
      ::ps/fields     {:db/id                   "id"
                       :db/timestamp            "timestamp"
                       :user-activity/user-id   "userId"
                       :user-activity/lesson-id "exerciseId"}}]))

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
    (let [enabled-keys #{:user/about :user/subscription-updated :user/subscription-amount}]
      (ps/save env (-> (select-keys data enabled-keys)
                       (assoc :db/id current-user-id
                              :db/table :user))))
    (go nil)))

(defn consume-guest-tx [{:keys [http-request current-user-id] :as env}]
  (go-catch
    (let [guest-tx (ex/session-get http-request :guest-tx)
          score    (atom 0)]
      (doseq [{:keys [guest-tx/increase-score] :as tx} guest-tx
              :let [tx (assoc tx :user-activity/user-id current-user-id)]]
        (swap! score (partial + increase-score))
        (<! (ps/save env tx)))
      (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))]
        (<! (ps/save env (update user :user/score (partial + @score))))))
    (ex/session-set! http-request :guest-tx [])))

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
                                         ::ps/schema schema}
                                        (js->clj profile :keywordize-keys true))))
        (catch :default e
          (done e))))))

(defn hit-video-view [env {:user-activity/keys [user-id lesson-id] :as view}]
  (go-catch
    (let [last-view (<? (ps/find-by env {:db/table              :user-activity
                                         :user-activity/user-id user-id
                                         ::ps/query             [[:orderBy :db/timestamp "desc"]]}))]
      (when (not= lesson-id (:user-activity/lesson-id last-view))
        (<? (ps/save env (assoc view :db/table :user-activity
                                     :db/timestamp (current-timestamp)
                                     :user-activity/type "view")))
        (<? (ps/save env (assoc view :db/table :user-view
                                     :db/timestamp (current-timestamp))))))))

(defn conj-vec [v x]
  (conj (or v []) x))

(defn compute-ex-answer [{:keys [current-user-id http-request] :as env}
                         {:keys [url/slug user-activity/hints-used]}]
  (go-catch
    (let [lesson (<? (ps/find-by env {:db/table :lesson
                                      :url/slug slug}))
          answer {:db/table                 :user-activity
                  :db/timestamp             (current-timestamp)
                  :user-activity/type       "answer"
                  :user-activity/lesson-id  (:db/id lesson)
                  :user-activity/hints-used hints-used
                  :guest-tx/increase-score  1}]
      (if current-user-id
        (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))]
          (<? (ps/save env (update user :user/score inc)))
          (<? (ps/save env (assoc answer :user-activity/user-id current-user-id)))
          (<? (ps/save env (assoc answer :user-activity/user-id current-user-id
                                         :db/table :ex-answer))))

        ; save for guest
        (ex/session-update! http-request :guest-tx
                            #(conj-vec % answer)))
      true)))

(defn compute-ex-answer-master [{:keys [current-user-id http-request] :as env}
                                {:keys [url/slug]}]
  (go-catch
    (let [lesson (<? (ps/find-by env {:db/table :lesson
                                      :url/slug slug}))
          answer {:db/table                 :user-activity
                  :db/timestamp             (current-timestamp)
                  :user-activity/type       "mastery"
                  :user-activity/lesson-id  (:db/id lesson)
                  :guest-tx/increase-score  100}]
      (if current-user-id
        (let [user (<? (ps/find-by env {:db/table :user :db/id current-user-id}))]
          (if (zero? (<? (ps/count env :user-activity [[:where {:user-activity/user-id   current-user-id
                                                                :user-activity/type      "mastery"
                                                                :user-activity/lesson-id (:db/id lesson)}]])))
            (do
              (<? (ps/save env (update user :user/score (partial + 100))))
              (<? (ps/save env (assoc answer :user-activity/user-id current-user-id)))
              (<? (ps/save env (assoc answer :user-activity/user-id current-user-id
                                             :db/table :ex-mastery))))
            (<? (ps/save env (update user :user/score inc)))))

        ; save for guest
        (ex/session-update! http-request :guest-tx
                            #(conj-vec % answer)))
      true)))

;; ROOT READS

(defn ast-sort [env sort]
  (assoc-in env [:ast :params :sort] sort))

(def guest-user-reader
  {:db/table   (constantly :user)
   :db/id      (constantly -1)
   :user/score (fn [{:keys [http-request]}]
                 (let [tx-list (ex/session-get http-request :guest-tx)]
                   (transduce (map :guest-tx/increase-score) + tx-list)))})

(def root-endpoints
  {:route/data     #(p/read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(ps/sql-first-node (assoc % ::ps/table :topic)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :lesson/by-slug #(ps/sql-first-node (assoc % ::ps/table :lesson ::ps/union-selector :lesson/type)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :lesson/search  #(if-let [search (get-in % [:ast :params :lesson/title])]
                      (ps/sql-table-node (-> (assoc-in % [:ast :params :where] [[::ps/f :lesson :lesson/title] "like" (str "%" search "%")]))
                                         :lesson))
   :app/courses    #(ps/sql-table-node (-> (ast-sort % "homepage_order")
                                           (assoc ::ps/union-selector :course/home-type)) :course)
   :app/me         #(if-let [id (:current-user-id %)]
                      (ps/sql-first-node (assoc % ::ps/table :user)
                                         [[:where {:id id}]])
                      (p/continue-with-reader % guest-user-reader))})

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
         (<? (hit-video-view env {:user-activity/user-id   current-user-id
                                  :user-activity/lesson-id id}))
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

(comment
  (go
    (let [env (assoc (daveconservatoire.server.core/api-env {})
                ::p/reader root-reader
                ::ps/query-cache (atom {})
                ::ps/schema schema)]
      (try
        (-> (ps/count env :user-activity [[:where-in :user-activity/type ["answer" "mastery"]]
                                          [:and-where {:user-activity/user-id 2}]])
            <? js/console.log)
        (catch :default e
          (js/console.log "ERROR" e)))))

  (go
    (let [env (assoc (daveconservatoire.server.core/api-env {})
                ::p/reader root-reader
                ::ps/query-cache (atom {})
                ::ps/schema schema)]
      (try
        (-> (parse env '[({:lesson/search [:lesson/title]} {:lesson/title "bass"})])
            <? println)
        (catch :default e
          (js/console.log "ERROR" e))))))
