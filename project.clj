(defproject dcex-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :clean-targets ^{:protect false} ["resources/public/devcards" "resources/public/site" "target"]

  :test-paths ["test/server"]
  :source-paths ["src/devcards" "src/cljs" "src/site" "script" "src/server" "src/dev"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha11" :scope "provided"]
                 [org.clojure/clojurescript "1.9.229" :scope "provided"]
                 [cljsjs/react-with-addons "15.2.1-0"]
                 [cljsjs/react-dom "15.2.1-0" :exclusions [cljsjs/react]]
                 [org.omcljs/om "1.0.0-alpha46" :exclusions [cljsjs/react-dom  cljsjs/react]]
                 [figwheel-sidecar "0.5.4-4" :exclusions [clj-time joda-time org.clojure/tools.reader] :scope "test"]
                 [bidi "2.0.9"]
                 [kibu/pushy "0.3.6"]
                 [binaryage/devtools "0.7.0" :exclusions [cljsjs/react]]
                 [devcards "0.2.1-4" :exclusions [org.omcljs/om cljsjs/react cljsjs/react-dom]]
                 [org.clojure/core.async "0.2.374"]
                 [navis/untangled-client "0.5.4-SNAPSHOT" :exclusions [org.omcljs/om com.cognitect/transit-cljs cljsjs/react-dom org.clojure/tools.reader cljsjs/react]]
                 [org.clojure/test.check "0.9.0"]
                 [com.rpl/specter "0.9.3"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [lein-doo "0.1.6" :scope "test"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]

  :figwheel {:open-file-command "open-in-intellij"}

  :cljsbuild {:builds
              [{:id           "devcards"
                :figwheel     {:devcards  true
                               :on-jsload "daveconservatoire.support.dev/reload-cycle"}
                :source-paths ["src/devcards" "src/cljs"]
                :compiler     {:main                 daveconservatoire.devcards
                               :source-map-timestamp true
                               :asset-path           "devcards"
                               :output-to            "resources/public/devcards/devcards.js"
                               :output-dir           "resources/public/devcards"
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false
                               :preloads             [daveconservatoire.support.dev]}}

               {:id           "site"
                :figwheel     {:on-jsload "daveconservatoire.support.dev/reload-cycle"}
                :source-paths ["src/cljs" "src/dev"]
                :compiler     {:main                 daveconservatoire.site.main
                               :source-map-timestamp true
                               :asset-path           "/site"
                               :output-to            "resources/public/site/site.js"
                               :output-dir           "resources/public/site"
                               :preloads             [daveconservatoire.support.dev]
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false}}

               ;; prod
               {:id           "site-min"
                :source-paths ["src/cljs"]
                :compiler     {:main                 daveconservatoire.site.main
                               :source-map-timestamp true
                               :pseudo-names         true
                               :optimizations        :advanced
                               :asset-path           "/site-min"
                               :output-to            "resources/public/site-min/site-min.js"
                               :output-dir           "resources/public/site-min"
                               :parallel-build       true
                               :recompile-dependents true
                               :source-map           "resources/public/site-min/site-min.js.map"
                               :verbose              false}}

               ;; server builds
               {:id           "server-dev"
                :source-paths ["src/cljs" "src/server"]
                :figwheel     true
                :compiler     {:main          daveconservatoire.server.core
                               :output-to     "target/server_dev/dcserver.js"
                               :output-dir    "target/server_dev"
                               :target        :nodejs
                               :optimizations :none
                               :source-map    true}}

               {:id           "server-test"
                :source-paths ["src/cljs" "src/server" "test/server"]
                :figwheel     true
                :compiler     {:main          daveconservatoire.server.suite
                               :output-to     "target/server_test/dctest.js"
                               :output-dir    "target/server_test"
                               :target        :nodejs
                               :optimizations :none
                               :source-map    true}}]})
