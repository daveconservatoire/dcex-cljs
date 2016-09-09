(ns utgn.lib-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are async testing deftest use-fixtures]]
            [cljs.core.async :refer [<!]]
            [daveconservatoire.server.parser :as p]
            [utgn.lib :as l]
            [daveconservatoire.server.test-shared :as ts]
            [om.next :as om]
            [common.async :refer [go-catch <?]]
            [nodejs.knex :as knex]))

(def db-specs
  (l/prepare-schema
    [{:key    :person
      :name   "people"
      :fields {:db/id       "uuid"
               :person/name "name"}}]))

(defonce db
  (knex/create-connection
    {:client     "pg"
     :connection {:host     "localhost"
                  :user     "wilkerlucio"
                  :database "utgn"}}))

(def env
  #::l {:db       db
        :db-specs db-specs})

(use-fixtures :each
  {:before (fn []
             (async done
               (go
                 (<! (knex/clear-table db "people"))
                 (done))))})

(deftest test-sql-first-node
  (async done
    (go
      (is (= (<! (l/sql-first-node {::l/db          ts/connection
                                    :parser         l/parser
                                    ::l/db-specs    p/db-specs
                                    ::l/query-cache (atom {})
                                    :table          :course
                                    :ast            {:query [:course/title]}}
                                   [[:where {:url/slug "reading-music"}]]))
             {:db/id 4 :course/title "Reading Music" :db/table :course}))

      (is (= (<! (l/sql-first-node {::l/db          ts/connection
                                    :parser         l/parser
                                    ::l/db-specs    p/db-specs
                                    ::l/query-cache (atom {})
                                    :table          :course
                                    :ast            {:query [(list
                                                               {:course/topics [:db/id :topic/title]}
                                                               {:limit 2})]}}
                                   [[:where {:db/id 4}]]))
             {:db/id    4 :course/topics [{:db/id 18 :db/table :topic :topic/title "Getting Started"}
                                          {:db/id 19 :db/table :topic :topic/title "Staff and Clefs"}]
              :db/table :course}))
      (done))))

(defn q [q] (-> (om/query->ast q) :children first))

(deftest test-read-key
  (is (= (l/read-from {:ast (q [:name])} (fn [_] "value"))
         "value"))
  (is (= (l/read-from {:ast (q [:name])} (fn [_] nil))
         nil))
  (is (= (l/read-from {:ast (q [:name]) :x 42} (fn [env] (:x env)))
         42))
  (is (= (l/read-from {:ast (q [:name]) :x 42} [(fn [env] (:x env))])
         42))
  (is (= (l/read-from {:ast (q [:name]) :x 42} [{} {:name (fn [env] (:x env))}])
         42))
  (is (= (l/read-from {:ast (q [:name])} {})
         nil))
  (is (= (l/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (l/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (l/read-from {:ast (q [:name])} [])
         nil))
  (let [c (fn [_] ::l/continue)
        m (fn [_] 42)]
    (is (= (l/read-from {:ast (q [:name])} [c])
           nil))
    (is (= (l/read-from {:ast (q [:name])} [m])
           42))
    (is (= (l/read-from {:ast (q [:name])} [c m])
           42))
    (is (= (l/read-from {:ast (q [:name])} [c {:no #(str "value")} [c c] {:name #(str "Bil")}])
           "Bil"))
    (is (= (l/read-from {:ast (q [:name])} [(fn [_] nil)])
           nil))))

(deftest test-find-by
  (async done
    (go
      (try
        (let [person (<? (l/save env {:db/table    :person
                                      :person/name "Wilker"}))
              person-found (<? (l/find-by env {:db/table    :person
                                               :person/name "Wilker"}))]
          (is (= (:db/id person)
                 (:db/id person-found))))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))

(deftest test-save
  (async done
    (go
      (try
        (let [person (<? (l/save env {:db/table    :person
                                      :person/name "Wilker"}))]
          (is (not (nil? (:db/id person))))
          (let [{:keys [db/id]} person
                person (<? (l/save env person))]
            (is (= (:db/id person)
                   id))))
        (catch :default e
          (do-report
            {:type :error, :message (.-message e) :actual e})))
      (done))))
