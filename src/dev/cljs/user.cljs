 (ns cljs.user
   (:require [untangled.client.core :as uc]
             [daveconservatoire.site.core :refer [app]]
             [untangled.client.util :as util]))

(def log-app-state (partial util/log-app-state app))
