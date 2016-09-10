(ns pathom.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [common.async :refer-macros [go-catch <?]]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs.spec :as s]))

;; SUPPORT FUNCTIONS

(defn union-children? [ast]
  (= :union (some-> ast :children first :type)))

(defn ast-key-id [ast] (some-> ast :key second))

(defn chan? [v] (satisfies? Channel v))

(defn resolved-chan [v]
  (let [c (async/promise-chan)]
    (put! c v)
    c))

(defn read-chan-values [m]
  (if (first (filter #(or (chan? %)
                          (chan? (:result %))) (vals m)))
    (let [c (async/promise-chan)
          in (async/to-chan m)]
      (go-loop [out {}]
        (if-let [[k v] (<! in)]
          (recur (assoc out k (cond
                                (chan? v) (<! v)
                                (chan? (:result v)) (assoc v :result (<! (:result v)))
                                :else v)))
          (>! c out)))
      c)
    (resolved-chan m)))

(defn read-chan-seq [f s]
  (go
    (let [out (async/chan 64)]
      (async/pipeline-async 10 out
                            (fn [in c]
                              (go
                                (let [in (if (chan? in) (<! in) in)
                                      out (f in)]
                                  (>! c (if (chan? out) (<! out) out)))
                                (close! c)))
                            (async/to-chan s))
      (<! (async/into [] out)))))

(s/def ::reader (s/or :fn (s/and fn?
                                 (s/fspec :args (s/cat :env any?)
                                          :ret any?))
                      :map (s/map-of keyword? ::reader)
                      :list (s/coll-of ::reader)))

(defn read-from* [{:keys [ast] :as env} reader]
  (let [k (:dispatch-key ast)]
    (cond
      (fn? reader) (reader env)
      (map? reader) (if-let [[_ v] (find reader k)]
                      (read-from* env v)
                      ::continue)
      (vector? reader) (let [res (into [] (comp (map #(read-from* env %))
                                                (remove #{::continue})
                                                (take 1))
                                          reader)]
                         (if (seq res)
                           (first res)
                           ::continue)))))

(defn read-from [env reader]
  (let [res (read-from* env reader)]
    (if (= res ::continue) nil res)))

;; NODE HELPERS

(defn placeholder-node [{:keys [ast parser] :as env}]
  (if (= "ph" (namespace (:dispatch-key ast)))
    (read-chan-values (parser env (:query ast)))
    ::continue))

;; PARSER READER

(defn read [{:keys [::readers] :as env} _ _]
  {:value
   (read-from env readers)})
