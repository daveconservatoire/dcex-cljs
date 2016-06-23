(ns daveconservatoire.site.core
  (:require [untangled.client.core :as uc]))

(defonce app
  (atom (uc/new-untangled-client)))
