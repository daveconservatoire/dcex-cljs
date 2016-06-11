(ns daveconservatorie.audio.core
  (:refer-clojure :exclude [create-node])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [daveconservatorie.audio.media.metronome :refer [metro]]
            [daveconservatorie.audio.media.piano :refer [piano]]
            [clojure.test.check]
            [clojure.test.check.generators]
            [clojure.test.check.properties]
            [goog.crypt.base64 :as g64]
            [goog.object :as gobj]
            [cljs.spec :as s]
            [cljs.core.async :as async :refer [chan promise-chan put! close! <! >! alts!]]
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
(s/def ::node-gen (s/fspec :args (s/cat) :ret ::node))
(s/def ::buffer #(instance? js/AudioBuffer %))
(s/def ::time (s/and number? pos?))
(s/def ::sound-point
  (s/and (s/keys :req [::time] :opt [::node ::node-gen])
         #(some #{::node ::node-gen} (keys %))))
(s/def ::sound-point-chan (ss/chan-of ::sound-point))
(s/def ::items-list
  (s/+ (s/cat :node-gens (s/+ ::node-gen)
              :interval ::time)))

(defn decode-audio-data [buffer]
  {:pre [(s/valid? ::ss/array-buffer buffer)]}
  (promise->chan (.decodeAudioData *audio-context* buffer)))

(s/fdef decode-audio-data
  :args (s/cat :buffer ::ss/array-buffer)
  :ret (ss/chan-of ::buffer))

(def NOTES [::C ::D ::E ::F ::G ::A ::B])

(defn current-time []
  "Return the current time from the Audio Context."
  (.-currentTime *audio-context*))

(s/fdef current-time
  :ret ::time)

(defn base64->audio-data [str]
  {:pre [(s/valid? ::ss/base64-string str)]}
  "Converts a base64 string into an AudioBuffer."
  (let [buffer (-> (.substr str 22) g64/decodeStringToUint8Array .-buffer)]
    (decode-audio-data buffer)))

(s/fdef base64->audio-data
  :args (s/cat :str ::ss/base64-string)
  :ret (ss/chan-of ::buffer))

(defn buffer-node [buffer]
  {:pre [(s/valid? ::buffer buffer)]}
  "Creates a buffer node from an AudioBuffer."
  (doto (.createBufferSource *audio-context*)
    (gobj/set "buffer" buffer)))

(s/fdef buffer-node
  :args (s/cat :buffer ::buffer)
  :ret ::node)

(defn gain-node [value]
  "Creates a gain with given value."
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

;(defn debounce [in ms]
;  (let [out (chan)]
;    (go-loop [last-val nil]
;             (let [val (if (nil? last-val) (<! in) last-val)
;                   timer (async/timeout ms)
;                   [new-val ch] (alts! [in timer])]
;               (condp = ch
;                 timer (do (>! out val) (recur nil))
;                 in (recur new-val))))
;    out))

(defn preload-sounds [sounds]
  (let [out (chan)
        in (async/to-chan sounds)
        process (fn [[name str] out']
                  (go
                    (let [data (<! (base64->audio-data str))
                          gen #(buffer-node data)]
                      (>! out' [name gen])
                      (close! out'))))]
    (async/pipeline-async 10 out process in true)
    (async/into {} out)))

(defonce ^:dynamic *sound-library*
  (let [a (atom {})]
    (go
      (reset! a (<! (preload-sounds (merge piano metro)))))
    a))

(def global-sound-manager
  (atom {::nodes {}}))

(defn play
  ([sound] (play sound global-sound-manager))
  ([{:keys [::node ::node-gen ::time]} tracker]
   (let [node (or node (node-gen))]
     (swap! tracker update ::nodes assoc node time)
     (doto node
       (.addEventListener "ended" #(swap! tracker update ::nodes dissoc node))
       (.connect (output))
       (.start time)))))

(s/fdef play
  :args (s/cat :sound ::sound-point))

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

(defn loop-chan [items start chan]
  {:pre [(s/valid? ::items-list items)
         (s/valid? ::time start)
         (s/valid? ::sound-point-chan chan)]}
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
            (recur (inc i) t))))))
  chan)

(s/fdef loop-chan
  :args (s/cat :items ::items-list
               :start ::time
               :chan ::sound-point-chan)
  :ret ::sound-point-chan)

(defn stop-all [nodes]
  (doseq [node nodes]
    (.stop node)))

(s/fdef stop-all
  :args (s/coll-of ::node []))

(defn consume-loop [interval chan]
  (let [control (async/chan)
        active (atom {::nodes {}})]
    (go
      (loop []
        (when-let [{:keys [::time] :as sound} (<! chan)]
          (play sound active)
          (let [cur-time (current-time)]
            (if-not (< (- time cur-time) (* interval 2))
              (let [timer (async/timeout (* interval 1000))
                    [v c] (alts! [control timer])]
                (condp = c
                  timer (recur)
                  control (case v
                            :stop-hard (->> (::nodes @active)
                                            (keys)
                                            (stop-all))
                            :stop (let [t (current-time)]
                                    (->> (::nodes @active)
                                         (keep (fn [[k v]] (if (> v t) k)))
                                         (stop-all))))))
              (recur))))))
    control))

(s/fdef consume-loop
  :args (s/cat :interval ::time
               :chan ::sound-point-chan)
  :ret (ss/chan-of #{:stop :stop-hard}))

(defn play-loop [items start]
  (let [lc (loop-chan items start (chan 8))]
    (consume-loop 10 lc)))
