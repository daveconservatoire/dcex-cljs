(ns daveconservatoire.site.core
  (:require [untangled.client.core :as uc]
            [daveconservatoire.site.routes :as r]
            [om.next :as om]
            [untangled.client.data-fetch :as df]
            [daveconservatoire.site.ui :as ui]))

(defn start-callback [{:keys [reconciler]}]
  (df/load reconciler :app/me ui/DesktopMenu)
  (om/transact! reconciler `[(app/set-route ~(r/current-handler))]))

(defonce app
  (atom (uc/new-untangled-client :started-callback start-callback)))
