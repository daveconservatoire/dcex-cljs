(ns daveconservatoire.site.routes
  (:require [bidi.bidi :as b]
            [cljs.spec :as s]
            [pushy.core :as pushy]))

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
(s/def ::handler (->> (b/route-seq routes)
                      (map :handler)
                      (set)))

(s/def ::route-params map?)
(s/def ::route (s/keys :req-un [::handler] :opt-un [::route-params]))

(defn path-for [{:keys [handler route-params] :as route}]
  {:pre [(s/valid? ::route route)]}
  (apply b/path-for routes handler (flatten1 route-params)))

(defn current-handler []
  (or (b/match-route routes (.getToken (pushy/new-history)))
      {:handler ::home}))
