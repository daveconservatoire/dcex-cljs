(ns daveconservatoire.server.test-shared
  (:require [nodejs.knex :as knex]
            [daveconservatoire.server.parser :as p]
            [pathom.core :as l]))

(defonce connection
  (knex/create-connection
    {:client     "mysql"
     :connection {:host     "localhost"
                  :user     "root"
                  :password "root"
                  :database "dcsite-pre"
                  :port     8889}}))

(def env
  {::l/db       connection
   ::l/db-specs p/schema})
