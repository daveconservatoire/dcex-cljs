(ns daveconservatoire.server.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [daveconservatoire.server.parser :as parser]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [knex.core :as knex]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))

(defn express-get [app pattern f] (.get app pattern f))
(defn express-post [app pattern f] (.post app pattern f))

(defonce connection
  (knex/create-connection
    {:client     "mysql"
     :connection {:host     "localhost"
                  :user     "root"
                  :password "root"
                  :database "dcsite"
                  :port     8889}}))

(def app (express))

(defn read-stream [s]
  (let [c (promise-chan)
        out (atom "")]
    (.setEncoding s "utf8")
    (.on s "data" (fn [chunk] (swap! out str chunk)))
    (.on s "end" (fn [] (put! c @out)))
    c))

(express-post app "/api"
  (fn [req res]
    (go
      (try
        (let [tx (-> (read-stream req) <!
                     (read-string))]
          (.send res (pr-str (<! (parser/parse {:db connection} tx)))))
        (catch :default e
          (.send res (str "Error: " e)))))))

(express-post app "/api-pretty"
  (fn [req res]
    (go
      (try
        (let [tx (-> (read-stream req) <!
                     (read-string))]
          (.send res (with-out-str
                       (cljs.pprint/pprint (<! (parser/parse {:db connection} tx))))))
        (catch :default e
          (.send res (str "Error: " e)))))))

(defn -main []
  (doto (.createServer http #(app %1 %2))
    (.listen 3000)))

(set! *main-cli-fn* -main)
