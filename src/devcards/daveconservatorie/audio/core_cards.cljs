(ns daveconservatorie.audio.core-cards
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [devcards.core :refer-macros [defcard deftest]]
            [cljs.core.async :as async :refer [promise-chan chan <! >! close! put!]]
            [cljs.test :refer-macros [is testing async]]
            [daveconservatorie.audio.core :as audio]))

(deftest test-play-piano
  (is (= 1 1)))


