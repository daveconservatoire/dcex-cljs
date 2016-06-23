(ns daveconservatoire.site.routes
  (:require [bidi.bidi :as b]))

(def routes
  ["/" {""             :home
        "about"        :about
        ["topic/" :id] :topic}])
