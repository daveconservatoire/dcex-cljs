(ns nodejs.rollbar
  (:require [cljs.nodejs :as nodejs]
            [cljs.spec :as s]))

(s/def ::access-token string?)

(defonce rollbar (nodejs/require "rollbar"))

(defn init [access-token]
  (.init rollbar access-token))

(defn report-message [message]
  (.reportMessage rollbar message))

(defn error-handler [access-token]
  (.errorHandler rollbar access-token))

(defn handle-error
  ([e]
   (if (.-data e)
     (.handleErrorWithPayloadData rollbar e (clj->js (.-data e)))
     (.handleError rollbar e)))
  ([e request] (.handleError rollbar e request))
  ([e request callback] (.handleError rollbar e request callback))
  ([e options request callback] (.handleError rollbar e (clj->js options) request callback)))
