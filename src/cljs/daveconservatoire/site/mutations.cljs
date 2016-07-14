(ns daveconservatoire.site.mutations
  (:require [untangled.client.core :as uc]
            [untangled.client.mutations :as m]
            [untangled.client.data-fetch :as df]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [daveconservatoire.site.ui.util :as uiu]
            [om.next :as om]
            [om.util :as omu]))

(defmethod m/mutate 'app/set-route
  [{:keys [state reconciler] :as env} _ route]
  {:action
   (fn []
     (let [comp (r/route->component* route)
           root (-> env :reconciler :config :indexer deref
                    :class->components (get ui/Root) first)
           data-query (if (implements? r/IRouteMiddleware comp)
                        (r/remote-query comp route)
                        (om/get-query comp))]
       (if data-query
         (df/load-data reconciler [{:route/data data-query}] :post-mutation 'fetch/export-idents))
       (om/set-query! root {:params {:route/data (uiu/normalize-route-data-query data-query)}})
       (js/setTimeout
         #(swap! state assoc :app/route route)
         10)))})

(defmethod m/mutate 'fetch/export-idents
  [{:keys [state]} _ _]
  {:action
   (fn []
     (let [pairs (filter (fn [[k _]] (omu/ident? k)) (get @state :route/data))]
       (if (seq pairs)
         (swap! state (fn [st]
                        (reduce (fn [s [k v]] (assoc-in s k v))
                                st pairs))))))})
