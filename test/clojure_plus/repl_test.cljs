(ns clojure-plus.repl-test
  (:require [clojure.test :refer-macros [run-tests testing is deftest async]]
            [check.core :refer-macros [check]]
            [cljs.core.async :as async]
            [check.async-cljs :refer-macros [def-async-test await!]]
            [clojure-plus.repl :as repl]))

(def-async-test "Running code in Clojure" {}
  (let [c (async/chan)]
    (repl/execute-cmd '(+ 2 1) "user" #(async/put! c %))
    (check (await! c) => {:value 3})))

(def-async-test "Exceptions in CLJ" {}
  (let [c (async/chan)]
    (repl/evaluate "(/ 10 0)" "user" {} #(async/put! c (-> % :value :error)))
    (check (await! c) =includes=> [:cause :trace])))

(def-async-test "Running code in CLJS" {}
  (let [c (async/chan)]
    (repl/evaluate "(+ 2 1)" "user" {:session "cljs"} #(async/put! c %))
    (check (await! c) => {:value 3})))

(def-async-test "Exceptions in CLJS" {}
  (let [c (async/chan)]
    (repl/evaluate "(throw (js/Error. \"Some Error\"))" "user"
                   {:session "cljs"}
                   #(async/put! c (:error %)))
    (check (keys (await! c)) => [:cause])))

(run-tests)
