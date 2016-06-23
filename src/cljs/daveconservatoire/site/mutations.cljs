(ns daveconservatoire.site.mutations
  (:require [untangled.client.mutations :as m]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [om.next :as om]))

(defmulti load-route :handler)

(defmethod load-route ::r/home [_ env]
  (js/console.log "load home data" env))

(defmethod load-route ::r/topic [{:keys [route-params]} {:keys [state]}]
  (swap! state assoc-in [:route/data :topic/slug] (::r/slug route-params))
  (js/console.log "load topic" route-params))

(defmethod load-route :default [_ _])

(defmethod m/mutate 'app/set-route
  [{:keys [state] :as env} _ route]
  {:action
   (fn []
     (let [root (-> env :reconciler :config :indexer deref
                    :class->components (get ui/Root) first)]
       (om/set-query! root {:params {:route/data (or (om/get-query (r/route->component route))
                                                     [])}}))
     (load-route route env)
     (swap! state assoc :app/route route))})
