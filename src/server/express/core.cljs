(ns express.core
  (:refer-clojure :exclude [get])
  (:require [cljs.spec :as s]
            [goog.object :as gobj]))

(defn get [app pattern f] (.get app pattern f))
(defn post [app pattern f] (.post app pattern f))
(defn use [app middleware] (.use app middleware))

(defn session-set! [req k v]
  (gobj/set (.-session req) k v))

(s/fdef session-set!
  :args (s/cat :req any? :key string? :value string?)
  :ret any?)
