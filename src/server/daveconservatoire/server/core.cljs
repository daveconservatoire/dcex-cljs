(ns daveconservatoire.server.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [chan <! put! promise-chan]]
            [cljs.reader :refer [read-string]]
            [cljs.pprint]
            [cljs.spec :as s]
            [cognitect.transit :as ct]
            [com.rpl.specter :as st :include-macros true]
            [common.async :refer-macros [<? go-catch]]
            [daveconservatoire.server.data :as d]
            [daveconservatoire.server.parser :as parser]
            [daveconservatoire.server.facebook :as fb]
            [goog.object :as gobj]
            [knex.core :as knex]
            [om.transit :as t]))

(nodejs/enable-util-print!)

(defonce fs (nodejs/require "fs"))

(defn slurp [path]
  (.. fs (readFileSync path #js {:encoding "utf8"})))

(def settings (read-string (slurp "./server.edn")))
(def facebook (get settings :facebook))

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))
(defonce compression (nodejs/require "compression"))
(defonce session (nodejs/require "express-session"))
(defonce RedisStore ((nodejs/require "connect-redis") session))

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

(express-use app (compression))
(express-use app (.static express "resources/public"))
(express-use app (session #js {:secret "keyboard caaaat"
                               :store (RedisStore. #js {})
                               :resave false
                               :saveUninitialized false
                               :cookie #js {:maxAge 60000}}))

(defn read-input [s] (ct/read (t/reader) s))
(defn spit-out [s] (ct/write (t/writer) s))

(def transform-io
  {"application/transit+json" {:read read-input :write spit-out}
   :default                   {:read read-string :write pr-str}})

(defn req-io [req] (transform-io (or (.is req "application/transit+json") :default)))

(s/fdef read-input
  :args (s/cat :s string?)
  :ret any?)

(defn current-user [req]
  (or (gobj/getValueByKeys req "session" "user") nil))

(express-post app "/api"
  (fn [req res]
    (go
      (try
        (let [{:keys [read write]} (req-io req)
              tx (-> (read-stream req) <!
                     read)
              out (<! (parser/parse {:db connection
                                     :current-user-id (current-user req)}
                                    tx))]
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
                       (cljs.pprint/pprint (<! (parser/parse {:db connection
                                                              :current-user-id (current-user req)} tx))))))
        (catch :default e
          (.send res (str "Error: " e)))))))

(express-get app "/facebook-login"
  (fn [req res]
    (.redirect res (fb/login-url (assoc facebook ::fb/scope [:public-profile :email])))))

(defn to-facebook-keys [query]
  (st/transform [st/ALL st/FIRST]
    #(keyword "daveconservatoire.server.facebook"
              (.replace % (js/RegExp. "_" "g") "-"))
    query))

(defn process-facebook-return [query]
  (go-catch
    (let [conformed (s/conform ::fb/auth-response query)]
      (cond
        (= conformed ::s/invalid) "Invalid input"

        (= :success (first conformed))
        (let [access-token (<? (fb/exchange-token (merge facebook (second conformed))))]
          (->> (fb/user-info access-token) <?
               (d/facebook-sign-in connection) <?))

        (= :error (first conformed))
        (str "Error: " (-> conformed second ::fb/error-description))))))

(s/def ::db-user (s/map-of keyword? any?))

(s/fdef process-facebook-return
  :args (s/cat :response ::fb/auth-response)
  :ret ::db-user)

(defn session-set! [req k v]
  (gobj/set (.-session req) k v))

(s/fdef session-set!
  :args (s/cat :req any? :key string? :value string?)
  :ret any?)

(express-get app "/facebook-return"
  (fn [req res]
    (go
      (try
        (let [query (->> (.. req -query) js->clj
                         (to-facebook-keys))
              user (<? (process-facebook-return query))]
          (session-set! req "user" (:id user))
          (.redirect res "/"))
        (catch :default e
          (.send res (str "Error: " e)))))))

(express-get app #".+"
  (fn [_ res]
    (.sendFile res (str (.cwd nodejs/process) "/resources/public/index.html"))))

(defn -main []
  (doto (.createServer http #(app %1 %2))
    (.listen 3000)))

(set! *main-cli-fn* -main)
