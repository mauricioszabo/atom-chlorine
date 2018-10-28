(defproject fixture-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojurescript "1.10.339"]
                 [org.clojure/clojure "1.9.0"]
                 [thheller/shadow-cljs "2.6.6"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
