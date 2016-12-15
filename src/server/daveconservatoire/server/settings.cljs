(ns daveconservatoire.server.settings
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as str]
            [cljs.reader :refer [read-string]]
            [goog.object :as gobj]))

(defonce fs (nodejs/require "fs"))
(defonce node-url (nodejs/require "url"))

(defn js->clj-one [js-obj]
  (let [keys (gobj/getKeys js-obj)]
    (zipmap keys (mapv #(gobj/get js-obj %) keys))))

(defn slurp [path]
  (try
    (.. fs (readFileSync path #js {:encoding "utf8"}))
    (catch :default _ nil)))

(defn read-env [path]
  (merge (js->clj-one (gobj/get nodejs/process "env"))
    (some-> (slurp path) read-string)))

(defn database-from-url [url]
  (let [url (.parse node-url url)
        [user pass] (str/split (.-auth url) ":")]
    {:client     (subs (.-protocol url) 0 (dec (count (.-protocol url))))
     :connection {:host     (.-hostname url)
                  :user     user
                  :password pass
                  :database (subs (.-pathname url) 1)
                  :port     (or (some-> (.-port url) js/parseInt) 3306)}}))

(defn env-settings [ENV]
  (if (contains? ENV "CLEARDB_DATABASE_URL")
    {:database       (database-from-url (get ENV "CLEARDB_DATABASE_URL"))
     :session-secret (get ENV "SESSION_SECRET")
     :rollbar        {:nodejs.rollbar/access-token (get ENV "ROLLBAR_TOKEN")}

     :google         #:nodejs.passport.google {:clientID     (get ENV "GOOGLE_CLIENT_ID")
                                               :clientSecret (get ENV "GOOGLE_CLIENT_SECRET")
                                               :callbackURL  (get ENV "GOOGLE_CALLBACK_URL")}

     :facebook       #:nodejs.passport.facebook {:clientID      (get ENV "FACEBOOK_CLIENT_ID")
                                                 :clientSecret  (get ENV "FACEBOOK_CLIENT_SECRET")
                                                 :callbackURL   (get ENV "FACEBOOK_CALLBACK_URL")
                                                 :profileFields ["id" "displayName" "name" "email" "picture"]}}))
