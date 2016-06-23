(ns daveconservatoire.site
  (:require [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [daveconservatoire.site.routes :refer [routes]]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]))

(defonce app (atom (uc/new-untangled-client :initial-state {:app/route {:handler :home}})))

(defn set-page! [match]
  (js/console.log "set page" match))

(defonce history
  (pushy/pushy set-page! (partial bidi/match-route routes)))

(pushy/start! history)

(reset! app (uc/mount @app ui/Root "app-container"))
