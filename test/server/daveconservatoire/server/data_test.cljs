(ns daveconservatoire.server.data-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [daveconservatoire.server.data :as d]
            [daveconservatoire.server.test-shared :as ts]
            [knex.core :as knex]))

(deftest test-user-by-email
  (async done
    (go
      (is (= (-> (d/user-by-email ts/connection "noemailyet@tempuser.com") <!
                 :username)
             "ZruMczeEIffrGMBDjlXo"))
      (done))))

(def user
  {:id      "10154316352107936",
   :name    "Wilker Lucio",
   :email   "wilkerlucio@gmail.com",
   :picture {:data {:is_silhouette false,
                    :url           "https://fbcdn-profile-a.akamaihd.net/hprofile-ak-xpf1/v/t1.0-1/p50x50/12553021_10153747425957936_5110097774267897266_n.jpg?oh=3b7ca7e856b5b0ee3e6bf920e1af560d&oe=585CF1F9&__gda__=1482329375_f13b0f80ba2c0f5fe51c77eeed911443"}}})

(deftest test-facebook-sign-in-existing
  (async done
    (go
      (<! (d/facebook-sign-in ts/connection (assoc user
                                              :email "noemailyet@tempuser.com")))
      (is (= (<! (knex/count ts/connection "User" "email")) 1))
      (done))))

(deftest test-facebook-sign-in-non-existing
  (async done
    (go
      (is (= (-> (d/facebook-sign-in ts/connection user) <! :name)
             "Wilker Lucio"))
      (is (= (<! (knex/count ts/connection "User" "email")) 2))
      (<! (knex/raw ts/connection "DELETE FROM User where email = ?" [(:email user)]))
      (done))))
