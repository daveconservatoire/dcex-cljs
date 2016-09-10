(ns daveconservatoire.server.test-shared
  (:require [nodejs.knex :as knex]
            [daveconservatoire.server.data :as d]
            [pathom.sql :as ps]))

(defonce connection
  (knex/create-connection
    {:client     "mysql"
     :connection {:host     "localhost"
                  :user     "root"
                  :password "root"
                  :database "dcsite-pre"
                  :port     8889}}))

(def env
  {::ps/db     connection
   ::ps/schema d/schema})
