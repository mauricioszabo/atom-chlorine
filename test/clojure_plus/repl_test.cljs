(ns clojure-plus.repl-test
  (:require [clojure.test :refer-macros [run-tests testing is deftest async]]
            [clojure-plus.repl :as repl]))

(deftest running-code
  (async done
   (testing "runs code on NREPL connection"
     (repl/execute-cmd '(+ 2 1)
                       "user"
                       (fn [res]
                         (is (= {:value 3} res))
                         (done))))))
(run-tests)
