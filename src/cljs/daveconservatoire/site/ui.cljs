(ns daveconservatoire.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [bidi.bidi :as bidi]))

(om/defui Button
  Object
  (render [this]
    (dom/a #js {:href      "#"
                :className (str "btn dc-btn-" (:color (om/props this)))}
      (om/children this))))

(def button (om/factory Button))

(defmulti route->component :handler)

(om/defui Home
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Home"))))

(defmethod route->component ::r/home []
  Home)

(om/defui About
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "About"))))

(defmethod route->component ::r/about []
  About)

(om/defui NotFound
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod route->component :default []
  NotFound)

(defn route->factory [route]
  (om/factory (route->component route)))

(om/defui Link
  Object
  (render [this]
    (let [{:keys [to]} (om/props this)]
      (dom/a #js {:href (bidi/path-for routes to)}
        (om/children this)))))

(def link (om/factory Link))

(om/defui Root
  static om/IQuery
  (query [this] [:app/route :ui/react-key])

  Object
  (render [this]
    (let [{:keys [app/route ui/react-key]} (om/props this)]
      (dom/div #js {:key react-key}
        (dom/h1 nil "Header")
        (dom/ul nil
          (dom/li nil (link {:to ::r/home} "Home"))
          (dom/li nil (link {:to ::r/about} "About")))
        ((route->factory route) nil)))))
