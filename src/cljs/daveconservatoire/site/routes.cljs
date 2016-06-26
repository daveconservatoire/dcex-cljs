(ns daveconservatoire.site.routes
  (:require [cljs.spec :as s]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]))

(defprotocol IRouteMiddleware
  (remote-query [this route]))

(defmulti route->component :handler)

(def routes
  ["/" {""                 ::home
        "about"            ::about
        "donate"           ::donate
        "tuition"          ::tuition
        "contact"          ::contact
        ["topic/" ::slug]  ::topic
        ["lesson/" ::slug] ::lesson}])

(s/def ::slug string?)
(s/def ::handler (->> (bidi/route-seq routes)
                      (map :handler)
                      (set)))

(s/def ::route-params map?)
(s/def ::route (s/keys :req-un [::handler] :opt-un [::route-params]))

(defn path-for [{:keys [handler route-params] :as route}]
  {:pre [(s/valid? ::route route)]}
  (apply bidi/path-for routes handler (flatten1 route-params)))

(defn current-handler []
  (or (bidi/match-route routes (.getToken (pushy/new-history)))
      {:handler ::home}))

(def match-route (partial bidi/match-route routes))
