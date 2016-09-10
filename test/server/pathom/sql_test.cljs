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
                           :person/group-id "group_id"}}

         {::ps/table      :group
          ::ps/table-name "grupos"
          ::ps/fields     {:db/id      "uuid"
                           :group/name "name"}}])

      (ps/row-getter :person/group
        #(ps/has-one % :group :person/group-id))

      (ps/row-getter :group/people
        #(ps/has-many % :person :person/group-id))))

(def env
  {::ps/db     (first dbs)
   ::ps/schema schema})

(use-fixtures :each
  {:before (fn []
             (async done
               (go
                 (try
                   (doseq [[_ db] dbs]
                     (<? (knex/clear-table db "galera"))
                     (<? (knex/clear-table db "grupos")))
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
                            :person/group-id "group_id"}
           ::ps/fields'    {"uuid"     :db/id
                            "name"     :person/name
                            "group_id" :person/group-id}}

          :group
          {::ps/table      :group
           ::ps/table-name "grupos"
           ::ps/fields     {:db/id      "uuid"
                            :group/name "name"}
           ::ps/fields'    {"uuid" :db/id
                            "name" :group/name}}})))

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

      (is (= (-> (ps/sql-first-node {::ps/db          db
                                     ::ps/schema      schema
                                     ::ps/query-cache (atom {})
                                     ::ps/table       :person
                                     :parser          parser
                                     :ast             {:query [:person/name]}}
                                    [[:where {:person/name "Guy"}]])
                 <! elide-ids)
             {:person/name "Guy" :db/table :person}))

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
              :db/table     :group})))))

(deftest test-find-by
  (db-test [db dbs]
    (let [env (assoc env ::ps/db db)]
      (let [person (<? (ps/save env {:db/table    :person
                                     :person/name "Wilker"}))
            person-found (<? (ps/find-by env {:db/table    :person
                                              :person/name "Wilker"}))]
        (is (= (:db/id person)
               (:db/id person-found)))))))

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
