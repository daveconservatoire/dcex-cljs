(ns daveconservatoire.site.core
  (:require [fulcro.client.core :as uc]
            [daveconservatoire.site.routes :as r]
            [om.next :as om]
            [fulcro.client.data-fetch :as df]))

(defn start-callback [{:keys [reconciler]}]
  (df/load reconciler :app/me daveconservatoire.site.ui/DesktopMenu {:marker false})
  (om/transact! reconciler `[(app/set-route ~(r/current-handler))]))

(defonce app
  (atom (uc/new-fulcro-client :started-callback start-callback)))
