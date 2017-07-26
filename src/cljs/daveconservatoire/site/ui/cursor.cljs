(ns daveconservatoire.site.ui.cursor
  (:require [om.next :as om]
            [om.dom :as dom]
            [daveconservatoire.site.ui.listeners :as l]
            [goog.object :as gobj]
            [untangled.client.core :as uc]
            [untangled.client.mutations :as m]))

(defn next-up [selected-index item-count]
  (-> (if (= 0 selected-index)
        (dec item-count)
        (dec selected-index))
      (mod item-count)))

(defn next-down [selected-index item-count]
  (-> (if (= selected-index (dec item-count))
        0
        (inc selected-index))
      (mod item-count)))

(defmethod m/mutate 'cursor/move-up
  [{:keys [state ref]} _ {:keys [ui/selected-index item-count]}]
  (swap! state assoc-in
         (conj ref :ui/selected-index)
         (next-up selected-index item-count)))

(defmethod m/mutate 'cursor/move-down
  [{:keys [state ref]} _ {:keys [ui/selected-index item-count]}]
  (swap! state assoc-in
         (conj ref :ui/selected-index)
         (next-down selected-index item-count)))

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
      (dom/div #js {:style #js {:overflow "auto"
                                :flex     "1"}
                    :ref   #(gobj/set this "container" %)}
        (l/simple-listener
          (assoc comp
            ::l/event "keydown"
            ::l/on-trigger (l/handle-key-events
                             {:down (fn [e]
                                      (.preventDefault e)
                                      (let [next        (next-down selected-index (count children))
                                            container   (gobj/get this "container")
                                            item        (gobj/get this (str "item-" next))
                                            item-bottom (+ (gobj/get item "offsetTop")
                                                           (gobj/get item "scrollHeight"))]
                                        (if-not (<= (gobj/get container "scrollTop")
                                                    item-bottom
                                                    (+ (gobj/get container "scrollTop") (gobj/get container "offsetHeight")))
                                          (if (= 0 next)
                                            (gobj/set container "scrollTop" 0)
                                            (gobj/set container "scrollTop" (-> item-bottom
                                                                                (- (gobj/get container "offsetHeight")))))))
                                      (om/transact! this `[(cursor/move-down ~(assoc props :item-count (count children)))]))
                              :up   (fn [e]
                                      (.preventDefault e)
                                      (let [next      (next-up selected-index (count children))
                                            container (gobj/get this "container")
                                            item      (gobj/get this (str "item-" next))]
                                        (if-not (<= (gobj/get container "scrollTop")
                                                    (gobj/get item "offsetTop")
                                                    (+ (gobj/get container "scrollTop") (gobj/get container "offsetHeight")))
                                          (if (= (dec (count children)) next)
                                            (gobj/set container "scrollTop" (-> (gobj/get container "scrollHeight")))
                                            (gobj/set container "scrollTop" (-> (gobj/get item "offsetTop"))))))
                                      (om/transact! this `[(cursor/move-up ~(assoc props :item-count (count children)))]))})))
        (->> children
             (map-indexed (fn [i x]
                            (if (= i selected-index)
                              (om/computed x {::selected? true})
                              x)))
             (map-indexed (fn [i x] (dom/div #js {:key         i
                                                  :ref         #(gobj/set this (str "item-" i) %)
                                                  :onMouseOver #(m/set-value! this :ui/selected-index i)} (factory x)))))))))

(def vertical-cursor (om/factory VerticalCursor))
