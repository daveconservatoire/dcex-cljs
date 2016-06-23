(ns daveconservatoire.server.parser-tests
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [is are run-tests async testing deftest run-tests]]
            [cljs.core.async :refer [<!]]
            [daveconservatoire.server.parser :as p]
            [daveconservatoire.server.test-shared :as ts]
            [com.rpl.specter :as sk :refer [setval select selected? view transform ALL FIRST LAST END filterer comp-paths keypath]]
            [om.next :as om]))

(deftest parse-read-not-found
  (is (= (p/parser {} [:invalid])
         {:invalid [:error :not-found]})))

(deftest parser-read-courses
  (async done
    (go
      (is (= (<! (p/parse {:db ts/connection} [:app/courses]))
             {:app/courses [{:id 4} {:id 7}]}))

      (is (= (<! (p/parse {:db ts/connection} [{:app/courses [:id :title]}]))
             {:app/courses [{:id 4 :title "Reading Music"}
                            {:id 7 :title "Music:  A Beginner's Guide"}]}))

      (done))))

(deftest test-query-row
  (async done
    (go
      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :table :course
                                     :ast   (om/query->ast [:id :title])}
                                    [[:where {:id 4}]]))
             {:id 4, :title "Reading Music"}))

      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :table :course
                                     :ast   (om/query->ast [:id (list
                                                                  {:topics [:id :title]}
                                                                  {:limit 2})])}
                                    [[:where {:id 4}]]))
             {:id 4 :topics [{:id 18 :title "Getting Started"} {:id 19 :title "Staff and Clefs"}]}))
      (done))))

(deftest parser-read-topics
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{:app/topics [:id :title {:course [:id]}]}]) <!
                  (select [:app/topics FIRST]))
             [{:id 2, :title "Getting Started" :course {:id 7}}]))
      (done))))

(deftest test-parse-read-ident
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{[:course/by-id 4] [:id :title]}]) <!)
             {[:course/by-id 4] {:id 4, :title "Reading Music"}}))
      (is (= (->> (p/parse {:db ts/connection} [{[:course/by-id 45848] [:id :title]}]) <!)
             {[:course/by-id 45848] [:error :row-not-found]}))
      (done))))

(deftest test-parse-read-lesson-by-slug
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "percussion"] [:id :title]}]) <!)
             {[:lesson/by-slug "percussion"] {:id 9, :title "Percussion"}}))
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "invalid"] [:id :title]}]) <!)
             {[:lesson/by-slug "invalid"] [:error :row-not-found]}))
      (done))))

(comment (run-tests))
