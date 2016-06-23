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
             {:app/courses [{:db/id 4} {:db/id 7}]}))

      (is (= (<! (p/parse {:db ts/connection} [{:app/courses [:db/id :course/title]}]))
             {:app/courses [{:db/id 4 :course/title "Reading Music"}
                            {:db/id 7 :course/title "Music:  A Beginner's Guide"}]}))

      (done))))

(deftest test-query-row
  (async done
    (go
      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :table :course
                                     :ast   (om/query->ast [:db/id :course/title])}
                                    [[:where {:url/slug "reading-music"}]]))
             {:db/id 4, :course/title "Reading Music"}))

      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :table :course
                                     :ast   (om/query->ast [:db/id (list
                                                                  {:course/topics [:db/id :topic/title]}
                                                                  {:limit 2})])}
                                    [[:where {:db/id 4}]]))
             {:db/id 4 :course/topics [{:db/id 18 :topic/title "Getting Started"}
                                       {:db/id 19 :topic/title "Staff and Clefs"}]}))
      (done))))

(deftest parser-read-topics
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{:app/topics [:db/id :topic/title {:topic/course [:db/id]}]}]) <!
                  (select [:app/topics FIRST]))
             [{:db/id 2, :topic/title "Getting Started" :topic/course {:db/id 7}}]))
      (done))))

(deftest test-parse-read-lesson-by-slug
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "percussion"] [:db/id :lesson/title]}]) <!)
             {[:lesson/by-slug "percussion"] {:db/id 9, :lesson/title "Percussion"}}))
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "invalid"] [:db/id :lesson/title]}]) <!)
             {[:lesson/by-slug "invalid"] [:error :row-not-found]}))
      (done))))
