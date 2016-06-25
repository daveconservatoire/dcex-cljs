(ns daveconservatoire.server.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [chan <! put! promise-chan]]
            [daveconservatoire.server.parser :as parser]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [knex.core :as knex]
            [cognitect.transit :as ct]
            [om.transit :as t]
            [cljs.spec :as s]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))

(defn express-get [app pattern f] (.get app pattern f))
(defn express-post [app pattern f] (.post app pattern f))
(defn express-use [app middleware] (.use app middleware))

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

(express-use app (.static express "resources/public"))

(defn read-input [s] (ct/read (t/reader) s))
(defn spit-out [s] (ct/write (t/writer) s))

(def transform-io
  {"application/transit+json" {:read read-input :write spit-out}
   :default {:read read-string :write pr-str}})

(defn req-io [req] (transform-io (or (.is req "application/transit+json") :default)))

(s/fdef read-input
  :args (s/cat :s string?)
  :ret ::s/any)

(express-post app "/api"
  (fn [req res]
    (go
      (try
        (let [{:keys [read write]} (req-io req)
              tx (-> (read-stream req) <!
                     read)
              out (<! (parser/parse {:db connection} tx))]
          (js/console.log "in")
          (cljs.pprint/pprint tx)
          (js/console.log "out")
          (cljs.pprint/pprint out)
          (.send res (write out)))
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

(express-get app #".+"
  (fn [_ res]
    (.sendFile res (str (.cwd nodejs/process) "/resources/public/index.html"))))

(defn -main []
  (doto (.createServer http #(app %1 %2))
    (.listen 3000)))

(set! *main-cli-fn* -main)
