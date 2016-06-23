(ns daveconservatoire.site.routes
  (:require [bidi.bidi :as b]
            [cljs.spec :as s]))

(defmulti route->component :handler)

(s/def ::slug string?)

(def routes
  ["/" {""                 ::home
        "about"            ::about
        "donate"           ::donate
        "tuition"          ::tuition
        "contact"          ::contact
        ["topic/" ::slug]  ::topic
        ["lesson/" ::slug] ::lesson}])

(s/def ::handler (->> (b/route-seq routes)
                      (map :handler)
                      (set)))

(defn path-for [handler params]
  {:pre [(s/valid? ::handler handler)
         (s/valid? map? params)]}
  (apply b/path-for routes handler (flatten1 params)))
