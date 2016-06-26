(ns daveconservatoire.site.ui-cards
  (:require [devcards.core :refer-macros [defcard deftest]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]))

(defcard button-cards
  (fn [_ _]
    (ui/button {:color "red"} "Content")))
