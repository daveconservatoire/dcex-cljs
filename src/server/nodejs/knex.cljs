(ns nodejs.knex
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:refer-clojure :exclude [count])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [common.async :as ca :refer-macros [<? go-catch]]
            [goog.object :as gobj]
            [goog.string :as gstr]))

(defn convert-object [obj]
  (let [keys (array-seq (gobj/getKeys obj))]
    (reduce #(assoc % (keyword %2) (gobj/get obj %2)) {} keys)))

(defn js-call [obj method args]
  (if-let [f (gobj/get obj method)]
    (.apply f obj (clj->js args))
    (throw (ex-info (str "Method `" method "` could not be found in " obj) {}))))

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

(defn run
  ([db table] (run db table []))
  ([db table cmds]
   (promise->chan (call-chain (db table) cmds))))

(defn run-first [db table cmds]
  (go
    (-> (run db table cmds) <? first)))

(defn query-count [db table cmds]
  (go-catch
    (some->
      (run db table (cons [:count "*"] cmds))
      <? first vals first)))

(defn raw
  ([db sql args]
   (go-catch
     (let [res (<? (promise->chan (.raw db sql (clj->js args))))]
       (->> (if (sequential? res) (first res) res)
            (map convert-object))))))

(defn insert [db table data]
  (-> (db table)
      (.insert (clj->js data))
      (promise->chan)))

(defn count [db table field]
  (go
    (-> (db table)
        (.count field)
        (promise->chan) <!
        ffirst second)))

(defn clear-table [db table]
  (raw db "delete from ??" [table]))
