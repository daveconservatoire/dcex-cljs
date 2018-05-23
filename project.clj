(defproject dcex-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :clean-targets ^{:protect false} ["resources/public/devcards" "resources/public/site-min" "resources/public/site" "target"]

  :test-paths ["test/server"]
  :source-paths ["src/cljs" "src/site" "src/server"]

  :dependencies [[org.clojure/clojure "1.9.0-beta3" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [org.clojure/core.async "0.4.474"]
                 [org.omcljs/om "1.0.0-beta1"]
                 [fulcrologic/fulcro "1.2.0-SNAPSHOT"]
                 [fulcrologic/fulcro-inspect "0.1.0-SNAPSHOT"]
                 [bidi "2.0.9"]
                 [kibu/pushy "0.3.6"]
                 [org.clojure/test.check "0.9.0"]
                 [com.rpl/specter "0.9.3"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [cljsjs/nprogress "0.2.0-1"]

                 [figwheel-sidecar "0.5.10"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-doo "0.1.6"]]

  :min-lein-version "2.6.0"

  :figwheel {:open-file-command "open-in-intellij"
             :css-dirs          ["resources/public/css"]}

  :prep-tasks [["clean"] ["cljsbuild" "once" "server" "site-min"]]

  :profiles {:dev {:source-paths ["src/devcards" "src/cljs" "src/site" "script" "src/server" "src/dev"]
                   :dependencies [[fulcrologic/fulcro-spec "1.0.0-beta5"]
                                  [binaryage/devtools "0.7.0" :exclusions [cljsjs/react]]
                                  [devcards "0.2.1-4" :exclusions [org.omcljs/om cljsjs/react cljsjs/react-dom]]
                                  [lein-doo "0.1.6" :scope "test"]]}}

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
                               :preloads             [fulcro.inspect.preload daveconservatoire.support.dev]
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

               {:id           "server"
                :source-paths ["src/cljs" "src/server"]
                :compiler     {:main           daveconservatoire.server.core
                               :output-to      "target/server/dcserver.js"
                               :output-dir     "target/server"
                               :asset-path     "target/server"
                               :parallel-build true
                               :language-in    :ecmascript5
                               :target         :nodejs
                               :optimizations  :simple
                               :static-fns     true
                               :source-map     "target/server/dcserver.js.map"}}

               {:id           "server-test"
                :source-paths ["src/cljs" "src/server" "test/server"]
                :figwheel     true
                :compiler     {:main          daveconservatoire.server.suite
                               :output-to     "target/server_test/dctest.js"
                               :output-dir    "target/server_test"
                               :target        :nodejs
                               :optimizations :none
                               :source-map    true}}]})
