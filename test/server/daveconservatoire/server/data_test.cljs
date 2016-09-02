(ns daveconservatoire.server.data-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [common.async :refer [<?]]
            [nodejs.knex :as knex]
            [daveconservatoire.server.data :as d]
            [daveconservatoire.server.lib :as l]
            [daveconservatoire.server.test-shared :as ts]))

(deftest test-user-by-email
  (async done
    (go
      (is (= (-> (d/user-by-email ts/connection "noemailyet@tempuser.com") <!
                 :username)
             "ZruMczeEIffrGMBDjlXo"))
      (done))))

(deftest test-hit-video-view
  (async done
    (go
      (try
        (testing "creates hit for empty record"
          (<? (d/hit-video-view ts/env #:user-view {:user-id 10 :lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "don't create new entry when last lesson is the same"
          (<? (d/hit-video-view ts/env #:user-view {:user-id 10 :lesson-id 5}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 1)))
        (testing "create new entry when lesson is different"
          (<? (d/hit-video-view ts/env #:user-view {:user-id 10 :lesson-id 6}))
          (is (= (<? (knex/query-count ts/connection "UserVideoView" []))
                 2)))
        (done)
        (catch :default e
          (js/console.log "Error" (.-stack e))
          (done))
        (finally
          (<! (knex/raw ts/connection "delete from UserVideoView" [])))))))
