(ns daveconservatoire.site
  (:require [om.next :as om]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [cljs.spec :as s]
            [daveconservatoire.site.routes :refer [routes]]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]))

(defn set-page! [match]
  (js/console.log "set page" match))

(defonce history
  (pushy/pushy set-page! (partial bidi/match-route routes)))

(pushy/start! history)

(def app (uc/new-untangled-client {}))

(let [node (js/document.getElementById "app-container")]
  (uc/mount app ui/Root node))
