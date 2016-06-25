(ns daveconservatoire.site.core
  (:require [untangled.client.core :as uc]
            [untangled.client.data-fetch :as df]
            [daveconservatoire.site.ui :as ui]
            [daveconservatoire.site.routes :as r]
            [om.next :as om]))

(defn start-callback [{:keys [reconciler]}]
  (om/transact! reconciler `[(app/set-route ~(r/current-handler))]))

(defonce app
  (atom (uc/new-untangled-client :started-callback start-callback)))
