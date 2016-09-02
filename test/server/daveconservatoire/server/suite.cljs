(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [daveconservatoire.server.parser-tests]
            [daveconservatoire.server.lib-test]
            [daveconservatoire.server.data-test]
            [nodejs.knex-test]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(doo-tests
  'daveconservatoire.server.parser-tests
  'daveconservatoire.server.lib-test
  'daveconservatoire.server.data-test
  'nodejs.knex-test)
