(ns daveconservatoire.server.data-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [common.async :refer [<?]]
            [nodejs.knex :as knex]
            [daveconservatoire.server.data :as d]
            [pathom.sql :as ps]
            [daveconservatoire.server.test-shared :as ts :refer [env]]))

(deftest test-create-user
  (async done
    (go
      (try
        (<? (knex/raw (::ps/db env) "delete from User where email = ?" ["mary@email.com"]))
        (let [id (<? (d/create-user env {:user/name  "Mary"
                                         :user/email "mary@email.com"}))]
          (is (not (nil? (<? (ps/find-by env {:db/id    id
                                              :db/table :user}))))))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))

(deftest test-hit-video-view
  (async done
    (go
      (try
        (<? (knex/raw ts/connection "delete from UserVideoView" []))
        (testing "creates hit for empty record"
          (<? (d/hit-video-view env {:user-view/user-id   10
                                     :user-view/lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "don't create new entry when last lesson is the same"
          (<? (d/hit-video-view env {:user-view/user-id   10
                                     :user-view/lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "create new entry when lesson is different"
          (<? (d/hit-video-view env {:user-view/user-id   10
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
          (<? (d/update-current-user (assoc env
                                       :current-user-id 720)
                                     {:user/about "New Description"}))
          (is (= (-> (knex/query-first ts/connection "User" [[:where {"id" 720}]])
                     <? (get "biog"))
                 "New Description")))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))

(deftest test-compute-ex-answer
  (async done
    (go
      (try
        (<? (knex/truncate (::ps/db env) "UserExerciseAnswer"))
        (<? (ps/save env {:db/table   :user
                          :db/id      720
                          :user/score 1}))
        (testing "does nothing for unlogged users"
          (<? (d/compute-ex-answer env {:url/slug "bass-clef-reading"}))
          (is (zero? (<? (ps/count env :ex-answer)))))

        (testing "compute score for logged user"
          (<? (d/compute-ex-answer (assoc env :current-user-id 720)
                                   {:url/slug "bass-clef-reading"}))
          (is (= (<? (ps/count env :ex-answer))
                 1))
          (is (= (-> (ps/find-by env {:db/table :user :db/id 720})
                     <? :user/score)
                 2)))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))
