(ns pathom.test
  (:require-macros [cljs.core.async.macros]
                   [pathom.test])
  (:require [nodejs.knex :as knex]))

(defonce dbs
  {:mysql
   (knex/create-connection
     {:client     "mysql"
      :connection {:host     "localhost"
                   :user     "root"
                   :password "root"
                   :database "pathom-test"
                   :port     8889}})

   :pg
   (knex/create-connection
     {:client     "pg"
      :connection {:host     "localhost"
                   :user     "wilkerlucio"
                   :database "pathom-test"}})})
