(ns daveconservatoire.site.ui-cards
  (:require [devcards.core :refer-macros [defcard deftest]]
            [daveconservatoire.site.ui :as ui]))

(defcard button-cards
  (fn [_ _]
    (ui/button {:color "red"} "Content")))


