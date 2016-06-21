(ns daveconservatoire.site
  (:require [om.next :as om]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]))

(def app (uc/new-untangled-client {}))

(let [node (js/document.getElementById "app-container")]
  (uc/mount app ui/Root node))
