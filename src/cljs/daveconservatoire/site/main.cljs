(ns daveconservatoire.site.main
  (:require [untangled.client.core :as uc]
            [daveconservatoire.site.ui :as ui]
            [daveconservatoire.site.core :refer [app]]
            [daveconservatoire.site.mutations]
            [daveconservatoire.site.routes :as r]
            [pushy.core :as pushy]
            [om.next :as om]))

(defn set-page! [match]
  (when (:mounted? @app)
    (om/transact! (-> @app :reconciler) `[(app/set-route ~match)])))

(defonce history
  (pushy/pushy set-page! r/match-route))

(pushy/start! history)

(swap! app uc/mount ui/Root "app-container")
