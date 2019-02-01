(defproject fixture-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[thheller/shadow-cljs "2.7.17"]
                 [org.clojure/clojure "1.10.0"]] 
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
