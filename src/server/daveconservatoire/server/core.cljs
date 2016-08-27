(ns daveconservatoire.server.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [cljs.reader :refer [read-string]]
            [cljs.pprint]
            [cljs.spec :as s]
            [cognitect.transit :as ct]
            [common.async :refer-macros [<? go-catch]]
            [daveconservatoire.server.data :as d]
            [daveconservatoire.server.parser :as parser]
            [nodejs.express :as ex]
            [nodejs.knex :as knex]
            [nodejs.passport :as passport]
            [goog.object :as gobj]
            [om.transit :as t]))

(nodejs/enable-util-print!)

(defonce fs (nodejs/require "fs"))

(defn slurp [path]
  (.. fs (readFileSync path #js {:encoding "utf8"})))

(def settings (read-string (slurp "./server.edn")))
(def facebook (get settings :facebook))
(def google (get settings :google))

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))
(defonce compression (nodejs/require "compression"))
(defonce session (nodejs/require "express-session"))
(defonce RedisStore ((nodejs/require "connect-redis") session))

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

(ex/use app (compression))
(ex/use app (.static express "resources/public"))
(ex/use app (session #js {:secret            (get settings :session-secret)
                          :store             (RedisStore. #js {})
                          :resave            true
                          :saveUninitialized false
                          :cookie            #js {:maxAge 600000}}))
(ex/use app (.initialize passport/passport))
(ex/use app (.session passport/passport))

(defn read-input [s] (ct/read (t/reader) s))
(defn spit-out [s] (ct/write (t/writer) s))

(def transform-io
  {"application/transit+json" {:read read-input :write spit-out}
   :default                   {:read read-string :write pr-str}})

(defn req-io [req] (transform-io (or (.is req "application/transit+json") :default)))

(s/fdef read-input
  :args (s/cat :s string?)
  :ret any?)

(defn current-user [req] (gobj/get req "user"))

(ex/post app "/api"
  (fn [req res]
    (go
      (try
        (let [{:keys [read write]} (req-io req)
              tx (-> (read-stream req) <!
                     read)
              out (<! (parser/parse {:db              connection
                                     :http-request    req
                                     :current-user-id (current-user req)}
                                    tx))]
          (js/console.log "in")
          (cljs.pprint/pprint tx)
          (js/console.log "out")
          (cljs.pprint/pprint out)
          (.send res (write out)))
        (catch :default e
          (.send res (str "Error: " e)))))))

(ex/post app "/api-pretty"
  (fn [req res]
    (go
      (try
        (let [tx (-> (read-stream req) <!
                     (read-string))]
          (.send res (with-out-str
                       (cljs.pprint/pprint (<! (parser/parse {:db              connection
                                                              :http-request    req
                                                              :current-user-id (current-user req)} tx))))))
        (catch :default e
          (.send res (str "Error: " e)))))))

(defonce GoogleStrategy (.-OAuth2Strategy (nodejs/require "passport-google-oauth")))
(defonce FacebookStrategy (.-Strategy (nodejs/require "passport-facebook")))

(defonce passport-setup
  (do
    (passport/setup-serialize-simple)

    (passport/use
      (GoogleStrategy.
        (clj->js google)
        (d/passport-sign-callback connection)))

    (passport/use
      (FacebookStrategy.
        (clj->js facebook)
        (d/passport-sign-callback connection)))))

(def auth-redirects {:successRedirect "/profile"
                     :failureRedirect "/login"})

(ex/get app "/google-login"
  (passport/authenticate "google" {:scope ["openid profile email"]}))

(ex/get app "/google-return"
  (passport/authenticate "google" auth-redirects))

(ex/get app "/facebook-login"
  (passport/authenticate "facebook" {:scope ["public_profile" "email"]}))

(ex/get app "/facebook-return"
  (passport/authenticate "facebook" auth-redirects))

(ex/get app #".+"
  (fn [_ res]
    (.sendFile res (str (.cwd nodejs/process) "/resources/public/index.html"))))

(defn -main []
  (doto (.createServer http #(app %1 %2))
    (.listen 3000)))

(set! *main-cli-fn* -main)
