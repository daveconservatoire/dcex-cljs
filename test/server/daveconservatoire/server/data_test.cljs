(ns daveconservatoire.server.data-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [common.async :refer [<?]]
            [nodejs.knex :as knex]
            [daveconservatoire.server.data :as d]
            [pathom.core :as l]
            [daveconservatoire.server.test-shared :as ts]))

(deftest test-hit-video-view
  (async done
    (go
      (try
        (<? (knex/raw ts/connection "delete from UserVideoView" []))
        (testing "creates hit for empty record"
          (<? (d/hit-video-view ts/env {:user-view/user-id 10
                                        :user-view/lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "don't create new entry when last lesson is the same"
          (<? (d/hit-video-view ts/env {:user-view/user-id 10
                                        :user-view/lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "create new entry when lesson is different"
          (<? (d/hit-video-view ts/env {:user-view/user-id 10
                                        :user-view/lesson-id 6}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 2)))
        (catch :default e
          (js/console.log (.-stack e))
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))

(deftest test-update-current-user
  (async done
    (go
      (try
        (<? (knex/raw ts/connection "update User set biog='' where id = 720" []))
        (testing "creates hit for empty record"
          (<? (d/update-current-user (assoc ts/env
                                       :current-user-id 720)
                                     {:user/about "New Description"}))
          (is (= (-> (knex/query-first ts/connection "User" [[:where {"id" 720}]])
                     <? (get "biog"))
                 "New Description")))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))
