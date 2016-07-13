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
             {:app/courses [{:db/id 4 :db/table :course} {:db/id 7 :db/table :course}]}))

      (is (= (<! (p/parse {:db ts/connection} [{:app/courses [:db/id :course/title]}]))
             {:app/courses [{:db/id 4 :course/title "Reading Music" :db/table :course}
                            {:db/id 7 :course/title "Music:  A Beginner's Guide" :db/table :course}]}))

      (done))))

(deftest test-query-row
  (async done
    (go
      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :query-cache (atom {})
                                     :table :course
                                     :ast   (om/query->ast [:db/id :course/title])}
                                    [[:where {:url/slug "reading-music"}]]))
             {:db/id 4 :course/title "Reading Music" :db/table :course}))

      (is (= (<! (p/query-sql-first {:db    ts/connection
                                     :query-cache (atom {})
                                     :table :course
                                     :ast   (om/query->ast [:db/id (list
                                                                     {:course/topics [:db/id :topic/title]}
                                                                     {:limit 2})])}
                                    [[:where {:db/id 4}]]))
             {:db/id    4 :course/topics [{:db/id 18 :db/table :topic :topic/title "Getting Started"}
                                          {:db/id 19 :db/table :topic :topic/title "Staff and Clefs"}]
              :db/table :course}))
      (done))))

(deftest parser-read-topics
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{:app/topics [:db/id :topic/title {:topic/course [:db/id]}]}]) <!
                  (select [:app/topics FIRST]))
             [{:db/id 2 :db/table :topic :topic/title "Getting Started" :topic/course {:db/id 7 :db/table :course}}]))
      (done))))

(deftest test-parse-read-lesson-by-slug
  (async done
    (go
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "percussion"] [:db/id :lesson/title]}]) <!)
             {[:lesson/by-slug "percussion"] {:db/id 9 :db/table :lesson :lesson/title "Percussion"}}))
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "percussion"] [:db/id :lesson/type]}]) <!)
             {[:lesson/by-slug "percussion"] {:db/id 9 :db/table :lesson :lesson/type :lesson.type/video}}))
      (is (= (->> (p/parse {:db ts/connection} [{[:lesson/by-slug "invalid"] [:db/id :lesson/title]}]) <!)
             {[:lesson/by-slug "invalid"] [:error :row-not-found]}))
      (done))))

(deftest parser-read-route-data
  (async done
    (go
      (is (= (<! (p/parse {:db ts/connection} [{:route/data [:app/courses]}]))
             {:route/data {:app/courses [{:db/id 4 :db/table :course} {:db/id 7 :db/table :course}]}}))
      (is (= (<! (p/parse {:db ts/connection} [{:placeholder/anything [:app/courses]}]))
             {:placeholder/anything {:app/courses [{:db/id 4 :db/table :course} {:db/id 7 :db/table :course}]}}))
      (done))))

(deftest test-read-lesson-union
  (let [lesson-union {:lesson.type/video    [:lesson/type :lesson/title]
                      :lesson.type/playlist [:lesson/type :lesson/description]
                      :lesson.type/exercise [:lesson/type :lesson/title :url/slug]}]
    (async done
      (go
        (is (= (->> (p/parse {:db ts/connection}
                             [{[:lesson/by-slug "percussion"]
                               lesson-union}]) <!)
               {[:lesson/by-slug "percussion"]
                {:db/id 9 :db/table :lesson :lesson/title "Percussion" :lesson/type :lesson.type/video}}))
        (is (= (->> (p/parse {:db ts/connection}
                             [{[:lesson/by-slug "percussion-playlist"]
                               lesson-union}]) <!)
               {[:lesson/by-slug "percussion-playlist"]
                {:db/id 11 :db/table :lesson :lesson/description "" :lesson/type :lesson.type/playlist}}))
        (is (= (->> (p/parse {:db ts/connection}
                             [{[:lesson/by-slug "tempo-markings"]
                               lesson-union}]) <!)
               {[:lesson/by-slug "tempo-markings"]
                {:db/id 67 :db/table :lesson :lesson/title "Exercise: Tempo Markings Quiz" :lesson/type :lesson.type/exercise
                 :url/slug "tempo-markings"}}))
        (done)))))

(defn q [q] (-> (om/query->ast q) :children first))

(deftest test-read-key
  (is (= (p/read-from {:ast (q [:name])} (fn [_] "value"))
         "value"))
  (is (= (p/read-from {:ast (q [:name])} (fn [_] nil))
         nil))
  (is (= (p/read-from {:ast (q [:name]) :x 42} (fn [env] (:x env)))
         42))
  (is (= (p/read-from {:ast (q [:name]) :x 42} [(fn [env] (:x env))])
         42))
  (is (= (p/read-from {:ast (q [:name]) :x 42} [{} {:name (fn [env] (:x env))}])
         42))
  (is (= (p/read-from {:ast (q [:name])} {})
         nil))
  (is (= (p/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (p/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (p/read-from {:ast (q [:name])} [])
         nil))
  (let [c (fn [_] ::p/continue)
        m (fn [_] 42)]
    (is (= (p/read-from {:ast (q [:name])} [c])
           nil))
    (is (= (p/read-from {:ast (q [:name])} [m])
           42))
    (is (= (p/read-from {:ast (q [:name])} [c m])
           42))
    (is (= (p/read-from {:ast (q [:name])} [c {:no #(str "value")} [c c] {:name #(str "Bil")}])
           "Bil"))
    (is (= (p/read-from {:ast (q [:name])} [(fn [_] nil)])
           nil))))
