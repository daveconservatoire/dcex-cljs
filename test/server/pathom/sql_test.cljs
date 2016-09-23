(ns pathom.sql-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are async testing deftest use-fixtures]]
            [cljs.core.async :refer [<!]]
            [pathom.core :as p]
            [pathom.sql :as ps]
            [pathom.test :refer [db-test dbs]]
            [daveconservatoire.server.test-shared :as ts]
            [om.next :as om]
            [common.async :refer [go-catch <?]]
            [clojure.data]
            [clojure.walk :as walk]
            [nodejs.knex :as knex]))

(def schema
  (-> (ps/prepare-schema
        [{::ps/table      :person
          ::ps/table-name "galera"
          ::ps/fields     {:db/id           "uuid"
                           :person/name     "name"
                           :person/group-id "group_id"
                           :url/slug        "url"}}

         {::ps/table      :group
          ::ps/table-name "grupos"
          ::ps/fields     {:db/id      "uuid"
                           :group/name "name"
                           :url/slug   "slug"}}])

      (ps/row-getter :person/group
        (ps/has-one :group :person/group-id))

      (ps/row-getter :group/people
        (ps/has-many :person :person/group-id))))

(def env
  {::ps/db     (first dbs)
   ::ps/schema schema})

(use-fixtures :each
  {:before (fn []
             (async done
               (go
                 (try
                   (doseq [[_ db] dbs]
                     (<? (knex/truncate db "galera"))
                     (<? (knex/truncate db "grupos")))
                   (catch :default e
                     (do-report
                       {:type :error, :message (.-message e) :actual e})))
                 (done))))})

(deftest test-prepare-schema
  (is (= (-> schema
             (update :person dissoc ::ps/reader-map)
             (update :group dissoc ::ps/reader-map))
         {:person
          {::ps/table      :person
           ::ps/table-name "galera"
           ::ps/fields     {:db/id           "uuid"
                            :person/name     "name"
                            :person/group-id "group_id"
                            :url/slug        "url"}
           ::ps/fields'    {"uuid"     :db/id
                            "name"     :person/name
                            "group_id" :person/group-id
                            "url"      :url/slug}}

          :group
          {::ps/table      :group
           ::ps/table-name "grupos"
           ::ps/fields     {:db/id      "uuid"
                            :group/name "name"
                            :url/slug   "slug"}
           ::ps/fields'    {"uuid" :db/id
                            "name" :group/name
                            "slug" :url/slug}}

          ::ps/translate-index
          {:person "galera"
           :group "grupos"

           :db/id           "uuid"
           :person/name     "name"
           :person/group-id "group_id"
           :group/name      "name"
           :url/slug        ::ps/translate-multiple}})))

(defn elide-ids [x]
  (walk/prewalk
    (fn [x]
      (if (and (map? x) (contains? x :db/id))
        (dissoc x :db/id)
        x))
    x))

(deftest test-sql-first-node
  (db-test [db dbs]
    (let [env (assoc env ::ps/db db)
          parser (om/parser {:read p/read})]
      (let [{:keys [db/id]} (<! (ps/save env {:db/table   :group
                                              :group/name "Company"}))]
        (<! (ps/save env {:db/table        :person
                          :person/name     "Guy"
                          :person/group-id id})))

      (testing "simple query"
        (is (= (-> (ps/sql-first-node {::ps/db          db
                                       ::ps/schema      schema
                                       ::ps/query-cache (atom {})
                                       ::ps/table       :person
                                       :parser          parser
                                       :ast             {:query [:person/name]}}
                                      [[:where {:person/name "Guy"}]])
                   <! elide-ids)
               {:person/name "Guy" :db/table :person})))

      (testing "has many relation"
        (is (= (-> (ps/sql-first-node {::ps/db          db
                                       ::ps/schema      schema
                                       ::ps/query-cache (atom {})
                                       ::ps/table       :group
                                       :parser          parser
                                       :ast             {:query [{:group/people [:person/name]}
                                                                 :group/name]}}
                                      [])
                   <! elide-ids)
               {:group/people [{:db/table :person :person/name "Guy"}]
                :group/name   "Company"
                :db/table     :group})))

      (testing "has one relation"
        (is (= (-> (ps/sql-first-node {::ps/db          db
                                       ::ps/schema      schema
                                       ::ps/query-cache (atom {})
                                       ::ps/table       :person
                                       :parser          parser
                                       :ast             {:query [{:person/group [:group/name]}
                                                                 :person/name]}}
                                      [])
                   <! elide-ids)
               {:db/table     :person
                :person/name  "Guy"
                :person/group {:group/name "Company"
                               :db/table   :group}}))))))

(deftest test-translate-args
  (testing "blank cmd list"
    (is (= (ps/translate-args env [])
           [])))
  (testing "translating table names"
    (is (= (ps/translate-args env [[:from :person]])
           [[:from "galera"]])))
  (testing "translating field names"
    (is (= (ps/translate-args env [[:where {:person/name "Megan"}]])
           [[:where {"name" "Megan"}]])))
  (testing "translating long field names"
    (is (= (ps/translate-args env [[:where {[::ps/f :person/name] "Megan"}]])
           [[:where {"galera.name" "Megan"}]]))
    (is (= (ps/translate-args env [[:where {[::ps/f :person :db/id] "Megan"}]])
           [[:where {"galera.uuid" "Megan"}]])))
  (testing "translating field names multiple"
    (is (thrown-with-msg? js/Error #"Multiple possibilities for key :url/slug"
                          (ps/translate-args env [[:where {:url/slug "Megan"}]])))))

(deftest test-find-by
  (db-test [db dbs]
    (let [env (assoc env ::ps/db db)]
      (let [person (<? (ps/save env {:db/table    :person
                                     :person/name "Wilker"}))
            person-found (<? (ps/find-by env {:db/table    :person
                                              :person/name "Wilker"}))]
        (is (= (:db/id person)
               (:db/id person-found)))

        (<? (ps/save env {:db/table :person :person/name "Agata"}))

        (is (= (-> (<? (ps/find-by env {:db/table  :person
                                        ::ps/query [[:orderBy :person/name]]}))
                   (dissoc :db/id))
               {:db/table        :person
                :url/slug        nil
                :person/group-id nil
                :person/name     "Agata"}))

        (testing "unavailable result"
          (is (= (-> (<? (ps/find-by env {:db/table    :person
                                          :person/name "Invalid"}))
                     (dissoc :db/id))
                 nil)))))))

(deftest test-save
  (db-test [db dbs]
    (let [env (assoc env ::ps/db db)]
      (let [person (<? (ps/save env {:db/table    :person
                                     :person/name "Wilker"}))]
        (is (not (nil? (:db/id person))))
        (is (not (sequential? (:db/id person))))
        (let [{:keys [db/id]} person
              person (<? (ps/save env person))]
          (is (= (:db/id person)
                 id)))))))

(deftest test-count
  (db-test [db dbs]
    (let [env (assoc env ::ps/db db)]
      (<? (ps/save env {:db/table    :person
                        :person/name "Wilker"}))
      (is (= (<? (ps/count env :person))
             1))

      (is (= (<? (ps/count env :person [[:where {:person/name "Wilker"}]]))
             1))

      (is (= (<? (ps/count env :person [[:where {:person/name "Other"}]]))
             0)))))
