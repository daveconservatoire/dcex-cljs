(ns daveconservatoire.devcards
  (:require [daveconservatoire.audio.core-cards]
            [daveconservatoire.site.ui-cards]
            [devtools.core :as devtools]
            [cljs.spec :as s]))

(defonce cljs-build-tools
  (devtools.core/install! [:custom-formatters :sanity-hints]))

(defn reload-cycle []
  (s/instrument-all))
