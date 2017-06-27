(ns daveconservatoire.support.dev
  (:require [devtools.core]))

(defonce cljs-build-tools
  (devtools.core/install! [:custom-formatters :sanity-hints]))

(defn reload-cycle []
  #_ (s/instrument-all))
