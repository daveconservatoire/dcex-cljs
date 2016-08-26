(ns daveconservatoire.server.data
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [knex.core :as knex]
            [common.async :refer-macros [go-catch <?]]))

(defn user-by-email [connection email]
  (knex/query-first connection "User" [[:where {:email email}]]))

(defn create-user [connection {:user/keys [name email]}]
  (go-catch
    (let [user {:name  name
                :email email
                :biog  "Please tell us about your musical interests and goals. This will help develop the site to better support your learning. It will not be made public."}
          [id] (<? (knex/insert connection "User" user))]
      (<? (knex/query-first connection "User" [[:where {:id id}]])))))

(defn facebook-sign-in [connection {:keys [name email]}]
  (go-catch
    (if-let [user (<? (user-by-email connection email))]
      user
      (create-user connection #:user {:name name :email email}))))
