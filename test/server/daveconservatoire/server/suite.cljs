(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [daveconservatoire.server.parser-tests]
            [knex.core-test]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(doo-tests 'daveconservatoire.server.parser-tests
           'knex.core-test)
