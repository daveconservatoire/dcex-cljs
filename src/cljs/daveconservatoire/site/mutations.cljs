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
  (df/load-data reconciler {[:topic/by-slug (::r/slug route-params)] (om/get-query (r/route->component route))}
                :post-mutation (fn [& args] (js/console.log "post mutation" args)))
  #_ (swap! state assoc-in [:topic/by-id 123]
         {:db/id 123
          :topic/title "Sample Topic"
          :topic/slug (::r/slug route-params)})
  #_ (swap! state assoc-in [:route/data] [:topic/by-id 123])
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
