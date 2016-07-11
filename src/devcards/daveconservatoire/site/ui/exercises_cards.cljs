(ns daveconservatoire.site.ui.exercises-cards
  (:require [devcards.core :as dc :refer-macros [dom-node defcard deftest]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.ui.exercises :as ex]
            [untangled.client.core :as uc]
            [om.next :as om]
            [om.dom :as dom]))

(defn ex-container [ex]
  (om/ui
    static uc/InitialAppState
    (initial-state [_ _] {:exercice (uc/initial-state ex nil)})

    static om/IQuery
    (query [_] [{:exercice (om/get-query ex)}])

    Object
    (render [this]
            (let [{:keys [exercice]} (om/props this)]
              (ex/pitch-detection exercice)))))

(def pitch-ex-app (atom (uc/new-untangled-test-client)))

(defcard pitch-ex
  (dom-node
    (fn [_ node]
      (reset! pitch-ex-app (uc/mount @pitch-ex-app (ex-container ex/PitchDetection) node)))))
