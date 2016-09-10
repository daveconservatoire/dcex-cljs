(ns daveconservatoire.server.suite
  (:require [doo.runner :refer-macros [doo-tests]]
            [pathom.core-test]
            [pathom.sql-test]
            [daveconservatoire.server.data-test]
            [daveconservatoire.server.parser-tests]
            [nodejs.knex-test]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(doo-tests
  'daveconservatoire.server.parser-tests
  'daveconservatoire.server.data-test
  'pathom.core-test
  'pathom.sql-test
  'nodejs.knex-test)
