(defproject dcex-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/devcards" "src/cljs" "src/site" "script"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha7" :scope "provided"]
                 [org.clojure/clojurescript "1.9.89" :scope "provided"]
                 [org.omcljs/om "1.0.0-alpha36"]
                 [figwheel-sidecar "0.5.4-3" :exclusions [clj-time joda-time org.clojure/tools.reader] :scope "test"]
                 [bidi "2.0.9"]
                 [kibu/pushy "0.3.6"]
                 [binaryage/devtools "0.7.0"]
                 [devcards "0.2.1-4" :exclusions [org.omcljs/om cljsjs/react-dom org.clojure/tools.reader cljsjs/react]]
                 [org.clojure/core.async "0.2.374"]
                 [navis/untangled-client "0.5.0" :exclusions [org.omcljs/om cljsjs/react-dom org.clojure/tools.reader cljsjs/react]]
                 [org.clojure/test.check "0.9.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :figwheel {:open-file-command "open-in-intellij"
             :validate-config false}

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
                :source-paths ["src/site" "src/cljs"]
                :compiler     {:main                 daveconservatoire.site
                               :source-map-timestamp true
                               :asset-path           "site"
                               :output-to            "resources/public/site/site.js"
                               :output-dir           "resources/public/site"
                               :preloads             [daveconservatoire.support.dev]
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false}}]})
