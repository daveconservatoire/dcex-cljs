(ns daveconservatoire.site.routes
  (:require [bidi.bidi :as b]))

(def routes
  ["/" {""                :home
        "about"           :about
        "donate"          :donate
        "tuition"         :tuition
        "contact"         :contact
        ["topic/" :slug]  :topic
        ["lesson/" :slug] :lesson}])
