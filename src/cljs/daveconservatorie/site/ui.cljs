(ns daveconservatorie.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]))

(om/defui Button
  Object
  (render [this]
    (dom/a #js {:href      "#"
                :className (str "btn dc-btn-" (:color (om/props this)))}
      (om/children this))))

(def button (om/factory Button))
