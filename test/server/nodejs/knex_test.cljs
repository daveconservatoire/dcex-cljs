(ns nodejs.knex-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [is are run-tests async testing deftest use-fixtures do-report]]
            [cljs.core.async :refer [<!]]
            [pathom.test :refer [db-test dbs]]
            [common.async :refer [<? go-catch]]
            [nodejs.knex :as knex]
            [daveconservatoire.server.test-shared :as ts]))

(use-fixtures :each
  {:before (fn []
             (async done
               (go
                 (try
                   (doseq [[_ db] dbs]
                     (<? (knex/truncate db "galera")))
                   (catch :default e
                     (do-report
                       {:type :error, :message (.-message e) :actual e})))
                 (done))))})

(deftest test-call-chain
  (testing "::call-this"
    (let [obj #js {:n      0
                   :inc    (fn [] (set! (.-n (js-this)) (inc (.-n (js-this))))
                             (js-this))
                   :higher (fn [f]
                             (.call f (js-this)))}]
      (knex/call-chain obj [[:higher [::knex/call-this [:inc] [:inc]]]])
      (is (= (.-n obj)
             2)))))

(deftest test-run
  (db-test [con dbs]
    (<! (knex/insert con "galera" {:name "Jane"}))
    (is (= (-> (knex/query con [[:from "galera"]]) <!
               first (get "name"))
           "Jane"))))

(deftest test-query-first
  (db-test [con dbs]
    (<! (knex/insert con "galera" {:name "Jane"}))
    (is (= (-> (knex/query-first con [[:from "galera"]]) <!
               (get "name"))
           "Jane"))))

(deftest test-query-count
  (db-test [con dbs]
    (<? (knex/insert con "galera" {:name "Jane"}))
    (<? (knex/insert con "galera" {:name "Bett"}))
    (is (= (->> (knex/query-count con [[:from "galera"]]) <?)
           2))))
