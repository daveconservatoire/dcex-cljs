(ns daveconservatoire.site.core
  (:require [untangled.client.core :as uc]
            [untangled.client.data-fetch :as df]
            [daveconservatoire.site.ui :as ui]
            [om.next :as om]))

(defn start-callback [{:keys [reconciler]}]
  (df/load-data reconciler [{:route/data (om/get-query ui/HomePage)}]))

(defonce app
  (atom (uc/new-untangled-client :started-callback start-callback)))
