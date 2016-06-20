(ns daveconservatorie.site.ui-cards
  (:require [devcards.core :refer-macros [defcard deftest]]
            [daveconservatorie.site.ui :as ui]))

(defcard button-cards
  (fn [_ _]
    (ui/button {:color "red"} "Content")))


