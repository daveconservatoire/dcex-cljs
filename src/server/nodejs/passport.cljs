(ns nodejs.passport
  (:require [cljs.nodejs :as nodejs]
            [cljs.spec :as s]))

(defonce passport (nodejs/require "passport"))

(defn use [strategy]
  (.use passport strategy))

(defn authenticate [strategy settings]
  (.authenticate passport strategy (clj->js settings)))

(s/fdef authenticate
  :args (s/cat :strategy string? :settings map?))

(s/def :nodejs.passport.google/client-id string?)
(s/def :nodejs.passport.google/client-secret string?)

(s/def ::google-settings
  (s/keys :req [:nodejs.passport.google/client-id
                :nodejs.passport.google/client-secret]))

(defn serialize-user [f]
  (.serializeUser passport f))

(defn deserialize-user [f]
  (.deserializeUser passport f))

(defn setup-serialize-simple []
  (serialize-user
    (fn [user done]
      (done nil user)))

  (deserialize-user
    (fn [id done]
      (done nil id))))
