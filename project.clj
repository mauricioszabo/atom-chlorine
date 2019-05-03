(defproject chlorine "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [org.clojure/core.async "0.4.474"]
                 [lein-cljfmt "0.6.1"]
                 [reagent "0.8.1"]]

  :source-paths ["lib" "src" "repl-tooling/src" "repl-tooling/test"]
  :resource-paths ["repl-tooling/resources"]

  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [check "0.0.2-SNAPSHOT"]
                                  [org.clojure/core.async "0.4.474"]
                                  [thheller/shadow-cljs "2.6.6"]]}}

  :clean-targets ^{:protect false} ["lib/js"])
