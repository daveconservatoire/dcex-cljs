(ns daveconservatoire.site.mutations
  (:require [untangled.client.mutations :as m]))

(defmethod m/mutate 'app/set-route
  [{:keys [state]} _ route]
  {:action
   (fn []
     (swap! state assoc :app/route route))})
