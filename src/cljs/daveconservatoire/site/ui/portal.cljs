(ns daveconservatoire.site.ui.portal
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.object :as gobj]
            [goog.dom :as gdom]
            [goog.style :as style]))

(defn render-subtree-into-container [parent c node]
  (js/ReactDOM.unstable_renderSubtreeIntoContainer parent c node))

(defn $ [s] (.querySelector js/document s))

(defn create-portal-node [props]
  (let [node (doto (gdom/createElement "div")
               (style/setStyle (clj->js (:style props))))]
    (cond
      (:append-to props) (gdom/append ($ (:append-to props)) node)
      (:insert-after props) (gdom/insertSiblingAfter node ($ (:insert-after props))))
    node))

(defn portal-render-children [children]
  (apply dom/div nil children))

(defui Portal
  Object
  (componentDidMount [this]
    (let [props (om/props this)
          node (create-portal-node props)]
      (gobj/set this "node" node)
      (render-subtree-into-container this (portal-render-children (om/children this)) node)))

  (componentWillUnmount [this]
    (when-let [node (gobj/get this "node")]
      (js/ReactDOM.unmountComponentAtNode node)
      (gdom/removeNode node)))

  (componentWillReceiveProps [this props]
    (let [node (gobj/get this "node")]
      (render-subtree-into-container this (portal-render-children (om/children this)) node)))

  (componentDidUpdate [this _ _]
    (let [node (gobj/get this "node")]
      (render-subtree-into-container this (portal-render-children (om/children this)) node)))

  (render [this] (js/React.DOM.noscript)))

(def portal (om/factory Portal))
