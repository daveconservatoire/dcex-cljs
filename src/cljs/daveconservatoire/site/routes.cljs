(ns daveconservatoire.site.routes
  (:require [cljs.spec :as s]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]
            [untangled.client.core :as uc]))

(defprotocol IRouteMiddleware
  (remote-query [this route]))

(defmulti route->component ::handler)

(defn route->component* [route]
  (let [comp' (route->component route)
        comp (js/Object.create (.-prototype comp'))
        _ (set! (.-om$isComponent comp) false)]
    (if (implements? IRouteMiddleware comp)
      comp
      comp')))

(defn route->initial-state [route]
  (let [c (route->component route)]
    (if (implements? uc/InitialAppState c)
      (uc/initial-state c route)
      {})))

(def routes
  ["/" {""                 ::home
        "login"            ::login
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

(s/def ::path string?)
(s/def ::params map?)
(s/def ::route (s/keys :req [::handler] :opt [::params]))

(defn path-for [{:keys [::handler ::params] :as route
                 :or {::params {}}}]
  {:pre [(s/valid? ::route route)]}
  (apply bidi/path-for routes handler (flatten1 params)))

(s/fdef path-for
  :args (s/cat :route ::route)
  :ret ::path)

(defn match-route [path]
  (if-let [{:keys [handler route-params]} (bidi/match-route routes path)]
    {::handler handler
     ::params route-params}))

(s/fdef match-route
  :args (s/cat :path ::path)
  :ret (s/nilable ::route))

(defn current-handler []
  (or (match-route (.getToken (pushy/new-history)))
      {::handler ::home}))

(s/fdef current-handler
  :args (s/cat)
  :ret ::route)
