(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [daveconservatoire.server.parser-tests]
            [knex.core-test]))

(doo-tests 'daveconservatoire.server.parser-tests
           'knex.core-test)
