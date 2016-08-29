(ns daveconservatoire.site.mutations
  (:require [untangled.client.mutations :as m]
            [untangled.client.data-fetch :as df]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [daveconservatoire.site.ui.util :as uiu]
            [om.next :as om]
            [om.util :as omu]))

(defn update-page [{:keys [state]} {:keys [route route/data]}]
  (om/set-query! (-> @state ::om/queries ffirst) {:params {:route/data data}})
  (swap! state assoc :app/route route))

(defmethod m/mutate 'app/set-route
  [{:keys [state reconciler] :as env} _ route]
  {:action
   (fn []
     (let [comp (r/route->component* route)
           data-query (if (implements? r/IRouteMiddleware comp)
                        (r/remote-query comp route)
                        (om/get-query comp))
           norm-query (uiu/normalize-route-data-query data-query)
           page-data {:route      route
                      :route/data norm-query}]
       (if data-query
         (do
           (swap! state assoc :app/route-swap page-data)
           (df/load-data reconciler [{:route/data data-query}
                                     {:app/me (om/get-query ui/DesktopMenu)}] :post-mutation 'fetch/complete-set-route))
         (update-page env page-data))))})

(defmethod m/mutate 'fetch/complete-set-route
  [{:keys [state] :as env} _ _]
  {:action
   (fn []
     (update-page env (get @state :app/route-swap))
     (if (map? (get @state :route/data))
       (let [pairs (filter (fn [[k _]] (omu/ident? k)) (get @state :route/data))]
         (if (seq pairs)
           (swap! state (fn [st]
                          (reduce (fn [s [k v]] (assoc-in s k v))
                                  st pairs)))))))})

(defmethod m/mutate 'app/logout
  [{:keys [state]} _ _]
  {:action #(swap! state dissoc :app/me)
   :remote true})
