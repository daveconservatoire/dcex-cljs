(defproject dcex-cljs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/devcards" "src/cljs" "script"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha7" :scope "provided"]
                 [org.clojure/clojurescript "1.9.76" :scope "provided" :exclusions [org.clojure/google-closure-library]]
                 [org.omcljs/om "1.0.0-alpha36"]

                 [figwheel-sidecar "0.5.4-3" :exclusions [clj-time joda-time org.clojure/tools.reader] :scope "test"]
                 [binaryage/devtools "0.7.0"]
                 [devcards "0.2.1-4" :exclusions [org.omcljs/om cljsjs/react-dom org.clojure/tools.reader cljsjs/react]]
                 [org.clojure/core.async "0.2.374"]

                 [navis/untangled-client "0.5.0" :exclusions [org.omcljs/om cljsjs/react-dom org.clojure/tools.reader cljsjs/react]]


                 [binaryage/devtools "0.7.0"]

                 [org.clojure/test.check "0.9.0"]

                 [org.clojure/google-closure-library "0.0-20160609-f42b4a24"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :figwheel {:open-file-command "open-in-intellij"}

  :cljsbuild {
              :builds
              [{:id           "devcards"
                :figwheel     {:devcards true}
                :source-paths ["src/devcards" "src/cljs"]
                :compiler     {
                               :main                 daveconservatorie.devcards
                               :source-map-timestamp true
                               :asset-path           "devcards"
                               :output-to            "resources/public/devcards/devcards.js"
                               :output-dir           "resources/public/devcards"
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false}}
               {:id           "main"
                :source-paths ["src/devcards" "src/cljs"]
                :compiler     {
                               :main                 daveconservatorie.main
                               :source-map-timestamp true
                               :asset-path           "devcards"
                               :output-to            "resources/public/devcards/devcards.js"
                               :output-dir           "resources/public/devcards"
                               :parallel-build       true
                               :recompile-dependents true
                               :verbose              false}}]})
