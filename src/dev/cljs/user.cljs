 (ns cljs.user
   (:require [fulcro.client.core :as uc]
             [daveconservatoire.site.core :refer [app]]
             [fulcro.client.util :as util]))

(def log-app-state (partial util/log-app-state app))
