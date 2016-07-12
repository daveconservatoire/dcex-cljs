(ns daveconservatoire.server.test-shared
  (:require [knex.core :as knex]))

(defonce connection
  (knex/create-connection
    {:client     "mysql"
     :connection {:host     "localhost"
                  :user     "root"
                  :password "root"
                  :database "dcsite-pre"
                  :port     8889}}))
