(ns user
  (:require [figwheel-sidecar.repl-api :as ra]))

(ra/start-figwheel! "devcards" "site" "server-dev")
(ra/cljs-repl)
