(ns daveconservatorie.audio.core
  (:refer-clojure :exclude [create-node])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [daveconservatorie.support.base64 :as b64]
            [daveconservatorie.audio.media.metronome :as metro]
            [goog.object :as gobj]
            [cljs.spec :as s]
            [cljs.core.async :refer [chan promise-chan put! close! <!]]
            [daveconservatorie.support.specs :as ss]))

(defonce AudioContext (or js/AudioContext js/webkitAudioContext))
(defonce audio-context (AudioContext.))

(defn promise->chan [promise]
  (let [c (promise-chan)]
    (.then promise #(put! c %) #(put! c %))
    c))

(s/def ::context #(instance? AudioContext %))
(s/def ::node #(instance? js/AudioNode %))
(s/def ::buffer #(instance? js/AudioBuffer %))
(s/def ::time (s/and number? pos?))

(defn decode-audio-data [buffer]
  {:pre [(s/valid? ::ss/array-buffer buffer)]}
  (promise->chan (.decodeAudioData audio-context buffer)))

(s/fdef decode-audio-data
  :args (s/cat :buffer ::ss/array-buffer)
  :ret ::ss/channel)

(def NOTES [::C ::D ::E ::F ::G ::A ::B])

(defn current-time [] (.-currentTime audio-context))

(s/fdef current-time
  :ret ::time)

(defn base64->audio-data [str]
  {:pre [(s/valid? ::ss/base64-string str)]}
  (let [buffer (-> (.substr str 22) b64/decode .-buffer)]
    (decode-audio-data buffer)))

(defn buffer-node [buffer]
  {:pre [(s/valid? ::buffer buffer)]}
  (doto (.createBufferSource audio-context)
    (gobj/set "buffer" buffer)))

(s/fdef buffer-node
  :args (s/cat :buffer ::buffer)
  :ret ::node)

(defmulti node-spec-type first)
(defmulti spec->node first)

(defmethod node-spec-type :buffer [_]
  (s/cat :name keyword?
         :buffer ::buffer))

(defmethod spec->node :buffer [s]
  (let [{:keys [buffer]} (s/conform ::node-spec s)]
    (buffer-node buffer)))

(defmethod node-spec-type :out [_]
  (s/cat :name keyword?))

(defmethod spec->node :out [_] (.-destination audio-context))

(defmethod node-spec-type :gain [_]
  (s/cat :name keyword? :value number?))

(defmethod spec->node :gain [s]
  (let [{:keys [value]} (s/conform ::node-spec s)]
    (doto (.createGain audio-context)
      (gobj/set "value" value))))

(s/def ::node-spec (s/multi-spec node-spec-type first))

(defn create-node [specs]
  {:pre [(s/valid? (s/+ ::node-spec) specs)]}
  (let [nodes (map spec->node specs)]
    (reduce (fn [cur next]
              (js/console.log "composing" cur next)
              (.connect cur next)
              next) nodes)
    (first nodes)))

(s/fdef create-node
  :args (s/+ ::node-spec))

(defn play-sample [str t]
  {:pre [(s/valid? ::ss/base64-string str)
         (s/valid? ::time t)]}
  (go
    (doto (create-node [[:buffer (<! (base64->audio-data str))]
                        [:gain 2]
                        [:out]])
      (.start t))))

(s/fdef play-sample
  :args (s/cat :str ::ss/base64-string
               :t ::time)
  :ret ::ss/channel)
