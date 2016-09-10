(ns daveconservatoire.server.data
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [nodejs.knex :as knex]
            [pathom.core :as l]
            [clojure.set :refer [rename-keys]]
            [common.async :refer [go-catch <?]]))

(defn current-timestamp []
  (js/Math.round (/ (.getTime (js/Date.)) 1000)))

(defn user-by-email [connection email]
  (knex/query-first connection "User" [[:where {:email email}]]))

(defn create-user [connection {:user/keys [name email]}]
  (go-catch
    (let [user {:name  name
                :email email
                :points 0
                :joinDate (current-timestamp)
                :lastActivity (current-timestamp)
                :biog  "Please tell us about your musical interests and goals. This will help develop the site to better support your learning. It will not be made public."}
          [id] (<? (knex/insert connection "User" user))]
      id)))

(defn update-current-user [{::l/keys [db schema]
                            :keys [current-user-id]}
                           data]
  (if current-user-id
    (let [{:keys [fields]} (get schema :user)
          enabled-keys #{:user/about}]
      (knex/run db "User" [[:where {"id" current-user-id}]
                           [:update (-> (select-keys data enabled-keys)
                                        (rename-keys fields)
                                        clj->js)]]))
    (go nil)))

(defn passport-sign-in [connection {:keys [emails displayName] :as profile}]
  (go-catch
    (let [email (some-> emails first :value)]
      (if-let [user (<? (user-by-email connection email))]
        (:id user)
        (<? (create-user connection #:user {:name displayName :email email}))))))

(defn passport-sign-callback [connection]
  (fn [_ _ profile done]
    (go
      (try
        (done nil (<? (passport-sign-in connection (js->clj profile :keywordize-keys true))))
        (catch :default e
          (done e))))))

(defn hit-video-view [{::l/keys [db schema]} {:user-view/keys [user-id lesson-id] :as view}]
  (go-catch
    (let [{:keys [name fields fields']} (get schema :user-view)
          last-view (some-> (knex/query-first db name
                                              [[:where {"userId" user-id}]
                                               [:orderBy "timestamp" "desc"]
                                               [:limit 1]])
                            <? (rename-keys fields'))]
      (if (not= lesson-id (:user-view/lesson-id last-view))
        (let [new-view (-> (merge view #:user-view {:timestamp (current-timestamp)})
                           (rename-keys fields))]
          (<? (knex/insert db name new-view)))))))

(defn compute-ex-answer [{::l/keys [db db-specs] :as env} {:keys [url/slug]}]
  (go-catch
    #_ (let [lesson (<? (l/find-by env :lesson {:url/slug slug}))])))
