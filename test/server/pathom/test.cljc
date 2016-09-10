(ns pathom.test
  #?(:cljs
     (:require [nodejs.knex :as knex])))

(defmacro db-test [binding & body]
  (let [[name list] binding]
    `(cljs.test/async done#
       (cljs.core.async.macros/go
         (try
           (doseq [[name# db#] ~list]
             (cljs.test/testing name#
               (let [~name db#]
                 ~@body)))
           (catch :default e#
             (cljs.test/do-report
               {:type :error, :message (.-stack e#) :actual e#})))
         (done#)))))

#?(:cljs
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
                      :database "pathom-test"}})}))
