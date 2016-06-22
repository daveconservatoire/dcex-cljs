(ns daveconservatoire.site.routes
  (:require [bidi.bidi :as b]))

(def routes
  ["" {""  :home
       "/" {""             :home
            ["topic/" :id] :topic}}])
