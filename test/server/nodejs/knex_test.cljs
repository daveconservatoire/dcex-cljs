(ns nodejs.knex-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [nodejs.knex :as knex]
            [daveconservatoire.server.test-shared :as ts]))

(deftest test-query
  (async done
    (go
      (is (= (->> (knex/query ts/connection "Course") <!
                  first :title)
             "Reading Music"))
      (done))))

(deftest test-query-first
  (async done
    (go
      (is (= (->> (knex/query-first ts/connection "Course" []) <!
                  :title)
             "Reading Music"))
      (done))))

(deftest test-query-count
  (async done
    (go
      (is (= (->> (knex/query-count ts/connection "Course" []) <!)
             2))
      (done))))
