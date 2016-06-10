(ns daveconservatorie.audio.core
  (:refer-clojure :exclude [create-node])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [daveconservatorie.audio.media.metronome :as metro]
            [daveconservatorie.audio.media.piano :refer [piano]]
            [clojure.test.check]
            [clojure.test.check.generators]
            [clojure.test.check.properties]
            [goog.crypt.base64 :as g64]
            [goog.object :as gobj]
            [cljs.spec :as s]
            [cljs.core.async :as async :refer [chan promise-chan put! close! <! >!]]
            [daveconservatorie.support.specs :as ss]))

(defonce AudioContext (or js/AudioContext js/webkitAudioContext))
(defonce ^:dynamic *audio-context* (AudioContext.))

(defn output [] (.-destination *audio-context*))

(defn promise->chan [promise]
  (let [c (promise-chan)]
    (.then promise #(put! c %) #(put! c %))
    c))

(s/def ::context #(instance? AudioContext %))
(s/def ::node #(instance? js/AudioNode %))
(s/def ::node-gen (s/fspec :args #{[]} :ret ::node))
(s/def ::buffer #(instance? js/AudioBuffer %))
(s/def ::time (s/and number? pos?))

(defn decode-audio-data [buffer]
  {:pre [(s/valid? ::ss/array-buffer buffer)]}
  (promise->chan (.decodeAudioData *audio-context* buffer)))

(s/fdef decode-audio-data
  :args (s/cat :buffer ::ss/array-buffer)
  :ret (ss/chan-of ::buffer))

(def NOTES [::C ::D ::E ::F ::G ::A ::B])

(defn current-time [] (.-currentTime *audio-context*))

(s/fdef current-time
  :ret ::time)

(defn base64->audio-data [str]
  {:pre [(s/valid? ::ss/base64-string str)]}
  (let [buffer (-> (.substr str 22) g64/decodeStringToUint8Array .-buffer)]
    (decode-audio-data buffer)))

(defn buffer-node [buffer]
  {:pre [(s/valid? ::buffer buffer)]}
  (doto (.createBufferSource *audio-context*)
    (gobj/set "buffer" buffer)))

(s/fdef buffer-node
  :args (s/cat :buffer ::buffer)
  :ret ::node)

(defn gain-node [value]
  (let [node (.createGain *audio-context*)]
    (-> (gobj/get node "gain") (gobj/set "value" value))
    node))

(s/fdef gain-node
  :args (s/cat :value (s/and number? pos?))
  :ret ::node)

(defn node-chain [nodes]
  "Connects nodes in order.
   Returns the first node on the list."
  {:pre [(s/valid? (s/+ ::node) nodes)]}
  (reduce (fn [cur next]
            (.connect cur next)
            next) nodes)
  (first nodes))

(s/fdef node-chain
  :args (s/+ ::node))

(defn play-sample [str t]
  {:pre [(s/valid? ::ss/base64-string str)
         (s/valid? ::time t)]}
  (go
    (doto (node-chain [(buffer-node (<! (base64->audio-data str)))
                       (gain-node 2)
                       (output)])
      (.start t))))

(s/fdef play-sample
  :args (s/cat :str ::ss/base64-string
               :t ::time)
  :ret (ss/chan-of ::node))

(def global-sound-manager
  (atom {:nodes #{}}))

(defn play [{:keys [::node ::node-gen ::time]}]
  (let [node (or node (node-gen))]
    (swap! global-sound-manager update :nodes conj node)
    (doto node
      (.addEventListener "ended" #(swap! global-sound-manager update :nodes disj node))
      (.start time))))

(s/fdef play
  :args (s/cat :sound ::sound-point))

(s/def ::sound-point
  (s/and (s/keys :req [::time] :opt [::node ::node-gen])
         #(some #{::node ::node-gen} (keys %))))

;; node-spec

;(defmulti node-spec-type first)
;(defmulti spec->node first)
;
;(defmethod node-spec-type :buffer [_]
;  (s/cat :name keyword?
;         :buffer ::buffer))
;
;(defmethod spec->node :buffer [s]
;  (let [{:keys [buffer]} (s/conform ::node-spec s)]
;    (buffer-node buffer)))
;
;(defmethod node-spec-type :out [_]
;  (s/cat :name keyword?))
;
;(defmethod spec->node :out [_] (output))
;
;(defmethod node-spec-type :gain [_]
;  (s/cat :name keyword? :value number?))
;
;(defmethod spec->node :gain [s]
;  (let [{:keys [value]} (s/conform ::node-spec s)]
;    (gain-node value)))
;
;(s/def ::node-spec (s/multi-spec node-spec-type first))

(s/def ::items-list
  (s/+ (s/cat :node-gens (s/+ ::node-gen)
              :interval ::time)))

(defn loop-chan [items start chan]
  {:pre [(s/valid? ::items-list items)
         (s/valid? ::time start)
         (s/valid? (ss/chan-of ::sound-point) chan)]}
  (go
    (loop [i 0
           t start]
      (let [i (mod i (count items))
            val (get items i)]
        (if (number? val)
          (recur (inc i)
                 (+ t val))
          (do
            (>! chan {::node-gen val ::time t})
            (recur (inc i)
                   t))))))
  chan)

(s/fdef loop-chan
  :args (s/cat :items ::items-list
               :start ::time
               :chan (ss/chan-of ::sound-point))
  :ret (ss/chan-of ::sound-point))

(defn consume-loop [interval chan]
  (go
    (loop []
      (when-let [{:keys [::time] :as sound} (<! chan)]
        (play sound)
        (let [cur-time (current-time)]
          (if-not (< (- time cur-time) (* interval 2))
            (<! (async/timeout (* interval 1000)))))
        (recur)))))
