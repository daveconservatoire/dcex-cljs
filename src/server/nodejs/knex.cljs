(ns nodejs.knex
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:refer-clojure :exclude [count])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [common.async :refer [<? go-catch]]
            [goog.object :as gobj]
            [goog.string :as gstr]))

(defn convert-object [obj]
  (if (map? obj)
    obj
    (let [keys (array-seq (gobj/getKeys obj))]
      (reduce #(assoc % %2 (gobj/get obj %2)) {} keys))))

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
    (.then promise #(put! c (js->clj %)) #(put! c %))
    c))

(defn run
  ([db table] (run db table []))
  ([db table cmds]
   (promise->chan (call-chain (db table) cmds))))

(defn query [db table cmds]
  (go-catch
    (->> (run db table cmds) <?
         (map convert-object))))

(defn query-first [db table cmds]
  (go-catch
    (-> (query db table cmds) <? first)))

(defn query-count [db table cmds]
  (go-catch
    (some->
      (query db table (cons [:count "*"] cmds))
      <? first vals first js/parseInt)))

(defn raw
  ([db sql args]
   (go-catch
     (let [res (<? (promise->chan (.raw db sql (clj->js args))))]
       (->> (if (sequential? res) (first res) res)
            (map convert-object))))))

(defn insert
  ([db table data] (insert db table data nil))
  ([db table data returning]
   (go-catch
     (let [res (<? (run db table [[:insert data returning]]))]
       (if (sequential? res)
         (first res)
         res)))))

(defn truncate [db table]
  (raw db "truncate table ??" [table]))
