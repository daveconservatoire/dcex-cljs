(ns daveconservatoire.server.data
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [knex.core :as knex]
            [common.async :refer-macros [go-catch <?]]
            [daveconservatoire.server.facebook :as fb]))

(defn user-by-email [connection email]
  (knex/query-first connection "User" [[:where {:email email}]]))

(defn facebook-sign-in [conn {:keys [name email]}]
  (go-catch
    (if-let [user (<? (user-by-email conn email))]
      user
      (let [[id] (<? (knex/insert conn "User" {:name name :email email}))]
        (<? (knex/query-first conn "User" [[:where {:id id}]]))))))
