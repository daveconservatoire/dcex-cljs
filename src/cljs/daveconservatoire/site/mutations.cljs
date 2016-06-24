(ns daveconservatoire.site.mutations
  (:require [untangled.client.mutations :as m]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.data-fetch :as df]
            [om.next :as om]))

(defmethod m/mutate 'app/set-route
  [{:keys [state reconciler] :as env} _ route]
  {:action
   (fn []
     (let [comp (r/route->component route)
           root (-> env :reconciler :config :indexer deref
                    :class->components (get ui/Root) first)
           data-query (om/get-query comp)]
       (if data-query
         (df/load-data reconciler [{:route/data data-query}]))
       (om/set-query! root {:params {:route/data (conj (or data-query [])
                                                       {:ui/fetch-state ['*]})}})
       (js/setTimeout
         #(swap! state assoc :app/route route)
         10)))})
