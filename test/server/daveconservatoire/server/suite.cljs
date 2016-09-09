(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [daveconservatoire.server.parser-tests]
            [utgn.lib-test]
            [daveconservatoire.server.data-test]
            [nodejs.knex-test]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(doo-tests
  'daveconservatoire.server.parser-tests
  'daveconservatoire.server.data-test
  'utgn.lib-test
  'nodejs.knex-test)
