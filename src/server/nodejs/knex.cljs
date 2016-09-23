(ns nodejs.knex
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:refer-clojure :exclude [count])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [chan <! put! promise-chan]]
            [clojure.walk :as walk]
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

(declare call-chain)

(defn create-call-this [[_ & args]]
  (fn [] (call-chain (js-this) args)))

(defn call-chain [object methods]
  (reduce (fn [o [cmd & args]]
            (let [name (if (keyword? cmd)
                         (gstr/toCamelCase (name cmd))
                         cmd)
                  args (walk/prewalk (fn [x]
                                       (if (and (vector? x)
                                                (= (first x) ::call-this))
                                         (create-call-this x)
                                         x)) args)]
              (js-call o name args)))
          object methods))

(defonce knex (nodejs/require "knex"))

(defn create-connection [options]
  (knex (clj->js options)))

(defn promise->chan [promise]
  (let [c (promise-chan)]
    (.then promise #(put! c (js->clj %)) #(put! c %))
    c))

(defn run [db cmds]
  (promise->chan (call-chain db cmds)))

(defn run' [db table cmds]
  (promise->chan (call-chain (db table) cmds)))

(defn query [db cmds]
  (go-catch
    (->> (run db cmds) <?
         (map convert-object))))

(defn query-first [db cmds]
  (go-catch
    (-> (query db cmds) <? first)))

(defn query-count [db cmds]
  (go-catch
    (some->
      (query db (cons [:count "*"] cmds))
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
     (let [res (<? (run' db table [[:insert data returning]]))]
       (if (sequential? res)
         (first res)
         res)))))

(defn truncate [db table]
  (raw db "truncate table ??" [table]))
