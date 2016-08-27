(ns daveconservatoire.server.data
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [nodejs.knex :as knex]
            [common.async :refer-macros [go-catch <?]]))

(defn user-by-email [connection email]
  (knex/query-first connection "User" [[:where {:email email}]]))

(defn create-user [connection {:user/keys [name email]}]
  (go-catch
    (let [user {:name  name
                :email email
                :biog  "Please tell us about your musical interests and goals. This will help develop the site to better support your learning. It will not be made public."}
          [id] (<? (knex/insert connection "User" user))]
      id)))

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
