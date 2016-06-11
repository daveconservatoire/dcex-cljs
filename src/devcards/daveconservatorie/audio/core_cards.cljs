(ns daveconservatorie.audio.core-cards
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [devcards.core :refer-macros [defcard deftest]]
            [cljs.core.async :as async :refer [promise-chan chan <! >! close! put!]]
            [cljs.test :refer-macros [is testing async]]
            [om.dom :as dom]
            [om.next :as om :include-macros true]
            [daveconservatorie.audio.core :as audio]))

(deftest test-play-piano
  (is (= 1 1)))

(defn pattern-buffers [pattern]
  (let [library @audio/*sound-library*]
    (mapv (fn [x] (if (string? x)
                    (get library x)
                    x))
          pattern)))

(defn sp-stop [c]
  (if-let [process (om/get-state c :process)]
    (put! process :stop)))

(defn reset-play [c {:keys [pattern]}]
  (sp-stop c)
  (om/update-state! c assoc :process
                    (audio/consume-loop 10 (audio/loop-chan (pattern-buffers pattern)
                                                            (audio/current-time)
                                                            (chan 8)))))

(om/defui SoundPattern
  Object
  (componentDidMount [this]
    (reset-play this (om/props this)))

  (componentWillUnmount [this]
    (sp-stop this))

  (componentWillReceiveProps [this new-props]
    (reset-play this new-props))

  (render [_]
    (dom/noscript nil)))

(def sound-pattern (om/factory SoundPattern))

(defcard chord-metronome
  (fn [_ _]
    (dom/div nil
      "Hello"
      #_ (sound-pattern {:pattern ["C3" "E3" "G3" 1 "low" 1 "G3" "B3" "D4" 1 "high" 1]}))))
