(ns daveconservatoire.site
  (:require [om.next :as om]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]
            [cljs.spec :as s]))

(def app (uc/new-untangled-client {}))

(let [node (js/document.getElementById "app-container")]
  (uc/mount app ui/Root node))

(defn reload-cycle []
  (s/instrument-all))
