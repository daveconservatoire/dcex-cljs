(ns nodejs.express
  (:refer-clojure :exclude [get])
  (:require [cljs.spec :as s]
            [cljs.reader :refer [read-string]]
            [goog.object :as gobj]))

(defn get [app pattern & f] (apply js-invoke app "get" pattern f))
(defn post [app pattern f] (.post app pattern f))
(defn use [app middleware] (.use app middleware))

(defn session-get [req k]
  (some-> (.-session req)
          (gobj/get (pr-str k))
          (str)
          (read-string)))

(s/fdef session-get
  :args (s/cat :req any? :key any? :value any?)
  :ret any?)

(defn session-set! [req k v]
  (gobj/set (gobj/get req "session") (pr-str k) (pr-str v))
  v)

(s/fdef session-set!
  :args (s/cat :req any? :key string? :value any?)
  :ret any?)

(defn session-update! [req k f]
  (let [old (session-get req k)
        new (f old)]
    (session-set! req k new)
    new))

(s/fdef session-update!
  :args (s/cat :req any? :key string? :fn fn?)
  :ret any?)
