(ns pathom.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [common.async :refer [go-catch <?]]
            [cljs.core.async :as async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs.spec :as s]))

(s/def ::reader-map (s/map-of keyword? ::reader))
(s/def ::reader-seq (s/coll-of ::reader :kind vector?))
(s/def ::reader-fn (s/fspec :args (s/cat :env any?)
                            :ret any?))

(s/def ::reader
  (s/or :fn ::reader-fn
        :map ::reader-map
        :list ::reader-seq))

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
          (let [value (cond
                        (chan? v) (<! v)
                        (chan? (:result v)) (assoc v :result (<! (:result v)))
                        :else v)]
            (recur (assoc out k value)))
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

(defn continue-with-reader [{:keys [parser ast] :as env} reader]
  (parser (assoc env ::reader reader) (:query ast)))

;; NODE HELPERS

(defn placeholder-node [{:keys [ast parser] :as env}]
  (if (= "ph" (namespace (:dispatch-key ast)))
    (read-chan-values (parser env (:query ast)))
    ::continue))

;; PARSER READER

(defn parser-error [env err]
  err
  (ex-info (str "Parser: " (.-message err)) {:path (pr-str (:path env))}))

(defn read [{:keys [::reader ast] :as env} _ _]
  {:value
   (let [env (update env :path conj (:key ast))]
     (try
       (let [value (read-from env reader)]
         (if (chan? value)
           (go
             (let [v (<! value)]
               (if (instance? js/Error v)
                 (parser-error env v)
                 v)))
           value))
       (catch :default e
         (parser-error env e))))})
