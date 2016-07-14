(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [daveconservatoire.server.parser-tests]
            [daveconservatoire.server.lib-test]
            [knex.core-test]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(doo-tests 'daveconservatoire.server.parser-tests
           'daveconservatoire.server.lib-test
           'knex.core-test)
