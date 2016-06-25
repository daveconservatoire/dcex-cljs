(ns daveconservatoire.site.main
  (:require [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [daveconservatoire.site.core :refer [app]]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]
            [daveconservatoire.site.mutations]
            [om.next :as om]))

(defn set-page! [match]
  (when (:mounted? @app)
    (om/transact! (-> @app :reconciler) `[(app/set-route ~match)])))

(defonce history
  (pushy/pushy set-page! (partial bidi/match-route routes)))

(pushy/start! history)

(reset! app (uc/mount @app ui/Root "app-container"))
