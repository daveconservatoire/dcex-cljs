(ns daveconservatoire.site.ui.exercises-cards
  (:require [devcards.core :as dc :include-macros true :refer-macros [dom-node defcard deftest]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.ui.exercises :as ex]
            [fulcro.client.core :as uc]
            [om.next :as om]
            [om.dom :as dom])
  (:require-macros [daveconservatoire.site.ui.exercises-cards :refer [defex]]))

(defn ex-container [{:keys [::ex/class ::ex/props ::ex/name] :as info}]
  (om/ui
    static uc/InitialAppState
    (initial-state [_ _]
                   {:exercice (uc/initial-state class (assoc props ::ex/name name))})

    static om/IQuery
    (query [_] [{:exercice (om/get-query class)}])

    Object
    (componentDidMount [this]
                       (let [state (-> (uc/initial-state class props)
                                       (merge info))
                             ident (om/ident class state)
                             r (om/get-reconciler this)]
                         (om/transact! r ident [`(fulcro.client.mutations/set-props ~state)])))

    (render [this]
            (let [{:keys [exercice]} (om/props this)]
              ((om/factory class) exercice)))))

(defex pitch-1)
(defex pitch-2)
(defex pitch-3)
(defex identifying-octaves)
(defex treble-clef-reading)
(defex bass-clef-reading)
(defex grand-staff-reading)
(defex rhythm-maths)
(defex rhythm-reading)
(defex chord-recognition)
(defex intervals-1)
(defex intervals-2)
(defex intervals-3)
(defex intervals-4)
(defex intervals-5)
(defex intervals-6)
(defex intervals-7)
(defex intervals-8)
(defex intervals-9)
(defex intervals-10)
(defex intervals-11)
(defex intervals-12)
(defex intervals-13)
(defex intervals-14)
(defex intervals-15)
(defex intervals-16)
(defex intervals-17)
(defex intervals-18)
(defex intervals-19)
(defex intervals-20)
(defex intervals-21)
(defex intervals-22)
(defex intervals-23)
(defex intervals-24)
