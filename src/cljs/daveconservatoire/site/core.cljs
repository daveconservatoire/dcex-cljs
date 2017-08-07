(ns daveconservatoire.site.core
  (:require [fulcro.client.core :as uc]
            [daveconservatoire.site.routes :as r]
            [om.next :as om]))

(defn start-callback [{:keys [reconciler]}]
  (om/transact! reconciler `[(app/set-route ~(r/current-handler))]))

(defonce app
  (atom (uc/new-fulcro-client :started-callback start-callback)))
