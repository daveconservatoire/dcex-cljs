(ns daveconservatoire.site.mutations
  (:require [untangled.client.mutations :as m]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.data-fetch :as df]
            [om.next :as om]))

(defmulti load-route :handler)

(defmethod load-route ::r/home [_ env]
  (js/console.log "load home data" env))

(defmethod load-route ::r/topic [{:keys [route-params] :as route} {:keys [state reconciler]}]
  #_ (df/load-data reconciler [{[:topic/by-slug (::r/slug route-params)] (om/get-query (r/route->component route))}])
  (js/console.log "load topic" {[:topic/by-slug (::r/slug route-params)] (om/get-query (r/route->component route))}))

(defmethod load-route :default [_ _])

(defmethod m/mutate 'app/set-route
  [{:keys [state] :as env} _ route]
  {:action
   (fn []
     (load-route route env)
     (let [root (-> env :reconciler :config :indexer deref
                    :class->components (get ui/Root) first)
           data-query (or (om/get-query (r/route->component route)) [])]
       (om/set-query! root {:params {:route/data data-query}}))
     (swap! state assoc :app/route route))})
