(ns daveconservatoire.audio.core
  (:refer-clojure :exclude [create-node])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [daveconservatoire.audio.media.metronome :refer [metro]]
            [daveconservatoire.audio.media.piano :refer [piano]]
            [daveconservatoire.support.specs :as ss]
            [goog.crypt.base64 :as g64]
            [goog.object :as gobj]
            [cljs.spec.alpha :as s]
            [cljs.core.async :as async :refer [chan promise-chan put! close! <! >! alts!]]))

(defonce AudioContext (or (gobj/get js/window "AudioContext")
                          (gobj/get js/window "webkitAudioContext")))

(defonce ^:dynamic *audio-context* (AudioContext.))

(defn output [] (.-destination *audio-context*))

(s/def ::context #(instance? AudioContext %))
(s/def ::node #(instance? js/AudioNode %))
(s/def ::node-gen (s/fspec :args (s/cat) :ret ::node))
(s/def ::buffer #(instance? js/AudioBuffer %))
(s/def ::time number?)
(s/def ::sound-point
  (s/and (s/keys :req [::time] :opt [::node ::node-gen])
         #(some #{::node ::node-gen} (keys %))))
(s/def ::sound-point-chan (ss/chan-of ::sound-point))
(s/def ::items-list
  (s/+ (s/cat :node-gens (s/+ ::node-gen)
              :interval ::time)))
(s/def ::interval ::time)
(s/def ::duration nat-int?)

(defn decode-audio-data [buffer]
  {:pre [(s/valid? ::ss/array-buffer buffer)]}
  (let [c (promise-chan)]
    (.decodeAudioData *audio-context* buffer #(put! c %))
    c))

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

(defn buffer-node
  "Creates a buffer node from an AudioBuffer."
  [buffer]
  {:pre [(s/valid? ::buffer buffer)]}
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

(defn load-sound-file [url]
  (let [c (promise-chan)]
    (let [xhr (js/XMLHttpRequest.)]
      (.open xhr "GET" url true)
      (gobj/set xhr "responseType" "arraybuffer")
      (gobj/set xhr "onload"
        #(let [response (gobj/get xhr "response")]
           (go (put! c (<! (decode-audio-data response))))))
      (.send xhr))
    c))

(defonce ^:dynamic *sound-library*
  (let [a (atom {})]
    (go
      (reset! a (<! (preload-sounds (merge piano metro)))))
    a))

(defonce global-sound-manager
  (atom {::nodes {}}))

(defn play
  ([sound] (play sound global-sound-manager))
  ([{::keys [node node-gen time] :as sound} tracker]
   (let [node (or node (node-gen))]
     (swap! tracker update ::nodes assoc node (assoc sound ::node node))
     (doto node
       (.addEventListener "ended" #(swap! tracker update ::nodes dissoc node))
       (.connect (output))
       (.start time)))))

(s/fdef play
  :args (s/cat :sound ::sound-point
               :tracker (s/? #(instance? Atom %))))

(defn play-sequence [nodes {:keys [::time]}]
  (-> (reduce (fn [[anodes t] {:keys [::duration] :as node}]
                [(conj anodes (play (assoc node ::time t)))
                 (+ t duration)])
              [[] time]
              nodes)
      (first)))

(defn play-regular-sequence [nodes {:keys [::interval] :as options}]
  (play-sequence (map #(assoc % ::duration interval) nodes) options))

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
            (>! chan {::node-gen val ::time t ::node-index i})
            (recur (inc i) t))))))
  chan)

(s/fdef loop-chan
  :args (s/cat :items (s/spec ::items-list)
               :start ::time
               :chan ::sound-point-chan)
  :ret ::sound-point-chan)

(defn stop [node] (.stop node))

(defn global-stop-all []
  (run! stop (->> @global-sound-manager
                  ::nodes
                  (vals)
                  (map ::node))))

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
                                            (run! stop))
                            :stop (let [t (current-time)]
                                    (->> (::nodes @active)
                                         (keep (fn [[k {:keys [::time]}]] (if (> time t) k)))
                                         (run! stop))))))
              (recur))))))
    control))

(s/fdef consume-loop
  :args (s/cat :interval ::time
               :chan ::sound-point-chan)
  :ret (ss/chan-of #{:stop :stop-hard}))

(defn play-loop [items start]
  (let [lc (loop-chan items start (chan 8))]
    (consume-loop 10 lc)))

(def TONE->VALUE {"C" 0 "D" 2 "E" 4 "F" 5 "G" 7 "A" 9 "B" 11})
(def ACCENT->VALUE {"b" -1 "#" 1 "" 0})
(def SEMITONE->NOTE {0 "C" 1 "Db" 2 "D" 3 "Eb" 4 "E" 5 "F" 6 "Gb" 7 "G" 8 "Ab" 9 "A" 10 "Bb" 11 "B"})

(def MAJOR-TRIAD [0 4 7])
(def MINOR-TRIAD [0 3 7])
(def DOMINANT-SEVENTH [0 4 7 10])

(def MAJOR-STEPS {0 0, 1 2, 2 4, 3 5, 4 7, 5 9, 6 11})
(def MAJOR-ARRANGEMENTS {0 MAJOR-TRIAD 1 MINOR-TRIAD 2 MINOR-TRIAD 3 MAJOR-TRIAD 4 MAJOR-TRIAD 5 MINOR-TRIAD 6 [0, 3, 6]})

(s/def ::semitone-interval integer?)
(s/def ::chord-intervals (s/coll-of ::semitone-interval))

(def NOTE-PATTERN #"^([A-G])([b#]?)([0-8])$")

(def ALL-NOTES
  #{"A0" "Bb0" "B0" "C1" "Db1" "D1" "Eb1" "E1" "F1" "Gb1" "G1" "Ab1" "A1" "Bb1" "B1"
    "C2" "Db2" "D2" "Eb2" "E2" "F2" "Gb2" "G2" "Ab2" "A2" "Bb2" "B2" "C3" "Db3" "D3"
    "Eb3" "E3" "F3" "Gb3" "G3" "Ab3" "A3" "Bb3" "B3" "C4" "Db4" "D4" "Eb4" "E4" "F4"
    "Gb4" "G4" "Ab4" "A4" "Bb4" "B4" "C5" "Db5" "D5" "Eb5" "E5" "F5" "Gb5" "G5" "Ab5"
    "A5" "Bb5" "B5" "C6" "Db6" "D6" "Eb6" "E6" "F6" "Gb6" "G6" "Ab6" "A6" "Bb6" "B6"
    "C7" "Db7" "D7" "Eb7" "E7" "F7" "Gb7" "G7" "Ab7" "A7" "Bb7" "B7" "C8"})

(s/def ::note (s/with-gen
                (s/and string? #(re-matches NOTE-PATTERN %))
                #(s/gen ALL-NOTES)))
(s/def ::semitone int?)

(s/def ::sound (s/or :note ::note :semitone ::semitone))

(declare semitone->note)

(defn note->semitone [note]
  (if (s/valid? ::semitone note)
    note
    (let [[_ tone accent octive] (re-matches NOTE-PATTERN note)]
      (-> (* octive 12)
          (- 9) (+ (TONE->VALUE tone)) (+ (ACCENT->VALUE accent))))))

(s/fdef note->semitone
  :args (s/cat :note (s/or :note ::note
                           :semitone ::semitone))
  :ret ::semitone
  :fn #(= (semitone->note (:ret %))
          (semitone->note (-> % :args :note second))))

(defn semitone->note [semitone]
  (if-not (s/valid? ::semitone semitone)
    semitone
    (let [s (- semitone 3)
          octive (-> (js/Math.floor (/ s 12)) (+ 1))
          tone (-> (+ s 12) (mod 12))]
      (str (SEMITONE->NOTE tone) octive))))

(s/fdef semitone->note
  :args (s/cat :semitone (s/or :note ::note
                               :semitone ::semitone))
  :ret ::note
  :fn #(= (note->semitone (:ret %))
          (note->semitone (-> % :args :semitone second))))

(defn chord [base arrange]
  (let [base (note->semitone base)]
    (mapv (comp semitone->note #(+ base %)) arrange)))

(s/fdef chord
  :args (s/cat :base ::sound :arrange ::chord-intervals)
  :ret (s/coll-of ::note))

(defn major-chord-progression [base progression]
  (let [base (note->semitone base)]
    (for [p progression
          :let [st (+ base (MAJOR-STEPS p))]]
      (chord st (MAJOR-ARRANGEMENTS p)))))

(s/def ::scale-position (s/int-in 0 7))

(s/fdef major-chord-progression
  :args (s/cat :base ::sound :progression (s/coll-of ::scale-position)))
