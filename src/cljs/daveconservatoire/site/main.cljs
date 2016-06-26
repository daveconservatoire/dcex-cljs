(ns daveconservatoire.site.main
  (:require [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [daveconservatoire.site.core :refer [app]]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [pushy.core :as pushy]
            [daveconservatoire.site.mutations]
            [om.next :as om]))

(defn set-page! [match]
  (when (:mounted? @app)
    (om/transact! (-> @app :reconciler) `[(app/set-route ~match)])))

(defonce history
  (pushy/pushy set-page! r/match-route))

(pushy/start! history)

(reset! app (uc/mount @app ui/Root "app-container"))
