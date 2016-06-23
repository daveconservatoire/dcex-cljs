(ns knex.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [goog.object :as gobj]
            [goog.string :as gstr]))

(defn convert-object [obj]
  (let [keys (array-seq (gobj/getKeys obj))]
    (reduce #(assoc % (keyword %2) (gobj/get obj %2)) {} keys)))

(defn js-call [obj method args]
  (if-let [f (gobj/get obj method)]
    (.apply f obj (clj->js args))
    (throw (js/Error (str "Method " method " could not be found in " obj)))))

(defn call-chain [object methods]
  (reduce (fn [o [cmd & args]] (js-call o (gstr/toCamelCase (name cmd)) args))
          object methods))

(defonce knex (nodejs/require "knex"))

(defn create-connection [options]
  (knex (clj->js options)))

(defn promise->chan [promise]
  (let [c (promise-chan)]
    (.then promise #(put! c (js->clj % :keywordize-keys true)) #(put! c %))
    c))

(defn query
  ([connection table] (query connection table []))
  ([connection table cmds]
   (promise->chan (call-chain (connection table) cmds))))

(defn raw
  ([connection sql args]
   (go
     (let [res (<! (promise->chan (.raw connection sql (clj->js args))))]
       (->> (first res)
            (map convert-object))))))
