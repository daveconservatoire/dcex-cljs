(ns daveconservatoire.server.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [cljs.reader :refer [read-string]]
            [cljs.pprint]
            [cljs.spec.alpha :as s]
            [clojure.walk :as walk]
            [cognitect.transit :as ct]
            [common.async :refer-macros [<? go-catch]]
            [daveconservatoire.server.parser :as p]
            [daveconservatoire.server.settings :as ds]
            [pathom.sql :as ps]
            [nodejs.express :as ex]
            [nodejs.knex :as knex]
            [nodejs.passport :as passport]
            [nodejs.rollbar :as rollbar]
            [goog.object :as gobj]
            [om.transit :as t]))

(nodejs/enable-util-print!)

(defonce source-map-support (.install (nodejs/require "source-map-support")))

(def settings (ds/env-settings (ds/read-env "env.edn")))
(def facebook (get settings :facebook))
(def google (get settings :google))
(def rollbar (get settings :rollbar))

(defonce express (nodejs/require "express"))
(defonce http (nodejs/require "http"))
(defonce compression (nodejs/require "compression"))
(defonce session (nodejs/require "express-session"))
(defonce RedisStore ((nodejs/require "connect-redis") session))

(defonce rollbar-init (rollbar/init (::rollbar/access-token rollbar)))

(defonce connection (knex/create-connection (:database settings)))

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
                          :store             (RedisStore. #js {:url (gobj/getValueByKeys nodejs/process #js ["env" "REDIS_URL"])})
                          :resave            true
                          :saveUninitialized false
                          :cookie            #js {}}))
(ex/use app (.initialize passport/passport))
(ex/use app (.session passport/passport))
(ex/use app (rollbar/error-handler (::rollbar/access-token rollbar)))

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

(defn process-errors! [out {:keys [req tx]}]
  (walk/postwalk
    (fn [x]
      (if (instance? js/Error x)
        (let [data (.-data x)]
          (if data (set! (.-data x) (assoc data :tx (pr-str tx))))
          (if-let [user-id (current-user req)] (set! (.-rollbar_person req) #js {:id user-id}))
          (rollbar/handle-error x req)
          nil)
        x))
    out))

(defn current-time [] (.getTime (js/Date.)))

(defn api-env [req]
  {::ps/db          connection
   ::ps/schema      p/schema
   :http-request    req
   :current-user-id (current-user req)})

(ex/post app "/api"
  (fn [req res]
    (go
      (try
        (let [{:keys [read write]} (req-io req)
              tx (-> (read-stream req) <!
                     read)
              start (current-time)
              out (-> (p/parse (api-env req)
                               tx)
                      <! (process-errors! {:req req
                                           :tx  tx}))
              finish (current-time)]
          (js/console.log "in")
          (cljs.pprint/pprint tx)
          (js/console.log "out")
          (cljs.pprint/pprint out)
          (println "Done" (str (- finish start) "ms"))
          (.send res (write out)))
        (catch :default e
          (.send res (str "Error: " e)))))))

(ex/post app "/api-pretty"
  (fn [req res]
    (go
      (try
        (let [tx (-> (read-stream req) <!
                     (read-string))
              out (<! (p/parse (api-env req) tx))]
          (.send res (with-out-str
                       (cljs.pprint/pprint out))))
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
        (p/passport-sign-callback connection)))

    (passport/use
      (FacebookStrategy.
        (clj->js facebook)
        (p/passport-sign-callback connection)))))

(def auth-redirects {:successRedirect "/profile"
                     :failureRedirect "/login"})

(ex/get app "/auth/google"
  (passport/authenticate "google" {:scope ["openid profile email"]}))

(ex/get app "/auth/facebook"
  (passport/authenticate "facebook" {:scope ["public_profile" "email"]}))

(ex/get app "/auth/:provider/return"
  (fn [req res next]
    (let [strategy (.. req -params -provider)]
      (if (contains? #{"google" "facebook"} strategy)
        ((passport/authenticate strategy {}) req res next)
        (next (ex-info (str "Invalid strategy `" strategy "`") {:strategy strategy})))))
  (fn [req res]
    (if (current-user req)
      (go
        (try
          (<? (p/consume-guest-tx (api-env req)))
          (.redirect res "/profile")
          (catch :default e
            (println "Error" e)
            (.redirect res "/login"))))
      (.redirect res "/login"))))

(def moment (nodejs/require "moment"))

(defn mysql-now []
  (.format (moment.) "YYYY-MM-DD HH:mm:ss"))

(ex/get app "/thanks"
  (fn [req res]
    (go-catch
      (let [amount (.. req -query -amt)
            st (.. req -query -st)
            user-id (current-user req)
            user {:db/id                     user-id
                  :db/table                  :user
                  :user/subscription-amount  amount
                  :user/subscription-updated (mysql-now)}]
        (if (and user-id (= "Completed" st))
          (<? (ps/save (api-env req) user)))
        (.sendFile res (str (.cwd nodejs/process) "/resources/public/index.html"))))))

(ex/get app #".+"
  (fn [_ res]
    (.sendFile res (str (.cwd nodejs/process) "/resources/public/index.html"))))

(defn -main []
  (let [port (or (gobj/getValueByKeys nodejs/process #js ["env" "PORT"])
                 3000)]
    (doto (.createServer http #(app %1 %2))
      (.listen port))
    (js/console.log "Server started at port" port)))

(set! *main-cli-fn* -main)
