(ns daveconservatorie.devcards
  (:require [daveconservatorie.audio.core-cards]
            [daveconservatorie.site.ui-cards]
            [devtools.core :as devtools]))

(defonce cljs-build-tools
  (do (devtools/enable-feature! :sanity-hints)
      (devtools.core/install!)))
