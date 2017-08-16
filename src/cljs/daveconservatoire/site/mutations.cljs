(ns daveconservatoire.site.mutations
  (:require [fulcro.client.mutations :as m]
            [fulcro.client.data-fetch :as df]
            [cljsjs.nprogress]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [daveconservatoire.site.ui.util :as uiu]
            [daveconservatoire.site.core :as dc]
            [om.next :as om]
            [om.util :as omu]
            [goog.object :as gobj]))

(js/NProgress.configure #js {:minimum      0.4
                             :trickleSpeed 100
                             :showSpinner  false})

(defn update-page [{:keys [state] :as env} {:keys [route route/data] :as params}]
  (if-let [reconciler (some-> dc/app deref :reconciler)]
    (let [root (om/class->any reconciler ui/Root)]
      (om/set-query! root {:params {:route/data data}})
      (swap! state assoc :app/route route)
      (swap! state assoc-in (conj (get @state :app/me) :ui/expanded?) false)
      (gobj/set js/document.body "scrollTop" 0))
    (js/setTimeout #(update-page env params) 10)))

(defmethod m/mutate 'app/set-route-data
  [{:keys [state]} _ data]
  {:action
   (fn []
     (swap! state update :route/data merge data))})

(defmethod m/mutate 'app/set-route
  [{:keys [state reconciler] :as env} _ route]
  {:action
   (fn []
     (let [comp       (r/route->component route)
           data-query (if (implements? r/IRouteMiddleware comp)
                        (r/remote-query comp route)
                        (om/get-query comp))
           norm-query (uiu/normalize-route-data-query data-query)
           page-data  {:route      route
                       :route/data norm-query}]
       (if data-query
         (do
           (js/NProgress.start)
           (swap! state assoc :app/route-swap page-data)
           (om/transact! reconciler [`(fulcro/load {:query         [{:route/data ~data-query}]
                                                    :target        [:route/next-data]
                                                    :post-mutation fetch/complete-set-route})])
           (df/load reconciler :app/me ui/DesktopMenu {:marker false}))
         (update-page env page-data))))})

(defmethod m/mutate 'fetch/complete-set-route
  [{:keys [state] :as env} _ _]
  {:action
   (fn []
     (swap! state assoc :route/data (get @state :route/next-data))
     (update-page env (get @state :app/route-swap))
     (js/NProgress.done)
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

(defmethod m/mutate 'lesson/save-view
  [_ _ _]
  {:action (fn [])
   :remote true})

(defn update-ref [{:keys [state ref]} data]
  (swap! state update-in ref merge data))

(defmethod m/mutate 'search/swap
  [{:keys [state]} _ {:keys [ref]}]
  (swap! state assoc-in
         (conj ref :lesson/search)
         (get-in @state (conj ref :lesson/search-swap))))

(defmethod m/mutate 'user/update
  [env _ data]
  {:action #(update-ref env data)
   :remote true})
