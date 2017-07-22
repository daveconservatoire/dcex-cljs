(ns daveconservatoire.site.ui.cursor
  (:require [om.next :as om]
            [om.dom :as dom]
            [daveconservatoire.site.ui.listeners :as l]
            [untangled.client.core :as uc]
            [untangled.client.mutations :as m]))

(defmethod m/mutate 'cursor/move-up
  [{:keys [state ref]} _ {:keys [ui/selected-index item-count]}]
  (swap! state assoc-in
         (conj ref :ui/selected-index)
         (if (= 0 selected-index)
           (dec item-count)
           (dec selected-index))))

(defmethod m/mutate 'cursor/move-down
  [{:keys [state ref]} _ {:keys [ui/selected-index item-count]}]
  (swap! state assoc-in
         (conj ref :ui/selected-index)
         (if (= selected-index (dec item-count))
           0
           (inc selected-index))))

(om/defui ^:once VerticalCursor
  static uc/InitialAppState
  (initial-state [_ props]
    (merge {:ui/selected-index 0
            :ui/cursor-name    "default"}
           props))

  static om/IQuery
  (query [_] [:ui/selected-index :ui/cursor-name])

  static om/Ident
  (ident [_ props] [:ui/cursor-by-name (:ui/cursor-name props)])

  Object
  (render [this]
    (let [{:ui/keys [selected-index] :as props} (om/props this)
          {::keys [children factory] :as comp} (om/get-computed props)]
      (dom/div nil
        (l/simple-listener
          (assoc comp
            ::l/event "keydown"
            ::l/on-trigger (l/handle-key-event
                             {:down (fn [e]
                                      (.preventDefault e)
                                      (om/transact! this `[(cursor/move-down ~(assoc props :item-count (count children)))]))
                              :up   (fn [e]
                                      (.preventDefault e)
                                      (om/transact! this `[(cursor/move-up ~(assoc props :item-count (count children)))]))})))
        (->> children
             (map-indexed (fn [i x]
                            (if (= i selected-index)
                              (om/computed x {::selected? true})
                              x)))
             (map factory))))))

(def vertical-cursor (om/factory VerticalCursor))
