(defproject clojure-plus "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/core.async "0.4.474"]
                 [repl-tooling "0.0.1-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :jvm-opts ["-Dclojure.server.repl={:port 5556 :accept clojure.core.server/repl}"]

  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [check "0.0.1-SNAPSHOT"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.4-6"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  :source-paths ["lib" "src"]
  :clean-targets ^{:protect false} ["lib/js"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :figwheel true
                        :compiler {:main clojure-plus.all-tests
                                   :output-to "lib/js/main.js"
                                   :output-dir "lib/js"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "release"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main clojure-plus.core
                                   :output-to "lib/js/main.js"
                                  ;  :output-dir "lib/js"
                                   :target :nodejs
                                   :optimizations :simple
                                   :output-wrapper true
                                   :warnings {:single-segment-namespace false}}}]}
  :figwheel {})
