(ns daveconservatoire.site
  (:require [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]
            [daveconservatoire.site.mutations]
            [om.next :as om]))

(defonce app
  (atom (uc/new-untangled-client :initial-state {:app/route {:handler ::r/home}})))

(defn set-page! [match]
  (when (:mounted? @app)
    (om/transact! (-> @app :reconciler) `[(app/set-route ~match)])))

(defonce history
  (pushy/pushy set-page! (partial bidi/match-route routes)))

(pushy/start! history)

(reset! app (uc/mount @app ui/Root "app-container"))
