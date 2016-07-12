(ns daveconservatoire.site.ui.exercises-cards
  (:require [devcards.core :as dc :refer-macros [dom-node defcard deftest]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.ui.exercises :as ex]
            [untangled.client.core :as uc]
            [om.next :as om]
            [om.dom :as dom]))

(defn ex-container [{:keys [::ex/class ::ex/props]}]
  (om/ui
    static uc/InitialAppState
    (initial-state [_ _] {:exercice (uc/initial-state class props)})

    static om/IQuery
    (query [_] [{:exercice (om/get-query class)}])

    Object
    (render [this]
            (let [{:keys [exercice]} (om/props this)]
              ((om/factory class) exercice)))))

(def pitch-1-app (atom (uc/new-untangled-test-client)))

(defcard pitch-1
  (dom-node
    (fn [_ node]
      (as-> (ex/slug->exercise "pitch-1") it
        (ex-container it)
        (uc/mount @pitch-1-app it node)
        (reset! pitch-1-app it)))))

(def pitch-2-app (atom (uc/new-untangled-test-client)))

(defcard pitch-2
  (dom-node
    (fn [_ node]
      (as-> (ex/slug->exercise "pitch-2") it
        (ex-container it)
        (uc/mount @pitch-2-app it node)
        (reset! pitch-2-app it)))))

(def pitch-3-app (atom (uc/new-untangled-test-client)))

(defcard pitch-3
  (dom-node
    (fn [_ node]
      (as-> (ex/slug->exercise "pitch-3") it
        (ex-container it)
        (uc/mount @pitch-3-app it node)
        (reset! pitch-3-app it)))))

(def identifying-octaves-app (atom (uc/new-untangled-test-client)))

(defcard identifying-octaves
  (dom-node
    (fn [_ node]
      (as-> (ex/slug->exercise "identifying-octaves") it
        (ex-container it)
        (uc/mount @identifying-octaves-app it node)
        (reset! identifying-octaves-app it)))))
