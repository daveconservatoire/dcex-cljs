(ns daveconservatoire.devcards
  (:require [daveconservatoire.audio.core-cards]
            [daveconservatoire.site.ui-cards]
            [devtools.core :as devtools]))

(defonce cljs-build-tools
  (do (devtools/enable-feature! :sanity-hints)
      (devtools.core/install!)))
