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

(om/defui Home
  static om/IQuery
  (query [_] [:app/topics])

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Home"))))

(defmethod r/route->component ::r/home [_] Home)

(om/defui About
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "About"))))

(defmethod r/route->component ::r/about [_] About)

(om/defui Topic
  static om/IQuery
  (query [_] [:topic/slug])

  Object
  (render [this]
    (let [{:keys [topic/slug]} (om/props this)]
      (dom/div nil
        "Topic" slug))))

(defmethod r/route->component ::r/topic [_] Topic)

(om/defui NotFound
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod r/route->component :default [_] NotFound)

(defn route->factory [route] (om/factory (r/route->component route)))

(om/defui Link
  Object
  (render [this]
    (let [{:keys [to params]
           :or {params {}}} (om/props this)]
      (dom/a #js {:href (r/path-for to params)}
        (om/children this)))))

(def link (om/factory Link))

(om/defui Root
  static om/IQueryParams
  (params [this]
    {:route/data []})

  static om/IQuery
  (query [this]
    '[:app/route :ui/react-key
      {:route/data ?route/data}])

  Object
  (componentWillMount [this]
    (let [{:keys [app/route]} (om/props this)
          initial-query (om/get-query (r/route->component route))]
      (om/set-query! this {:params {:route/data initial-query}})))

  (render [this]
    (let [{:keys [app/route route/data ui/react-key]} (om/props this)]
      (dom/div #js {:key react-key}
        (dom/h1 nil "Header")
        (dom/ul nil
          (dom/li nil (link {:to ::r/home} "Home"))
          (dom/li nil (link {:to ::r/about} "About"))
          (dom/li nil (link {:to ::r/topic :params {::r/slug "getting-started"}} "Topic Getting Started")))
        ((route->factory route) (assoc data :ref :page))))))
