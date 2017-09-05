(ns clojure-plus.repl-test
  (:require [clojure.test :refer-macros [run-tests testing is deftest async]]
            [clojure-plus.repl :as repl]))

(deftest running-code
  (async done
    (repl/execute-cmd '(+ 2 1)
                      "user"
                      (fn [res]
                        (is (= {:value 3} res))
                        (done)))))

(deftest exceptions
  (async done
    (repl/evaluate "(/ 10 0)"
                   "user"
                   {}
                   (fn [res]
                     (def res res)
                     (is (contains? (:error res) :cause))
                     (is (contains? (:error res) :stack))
                     (done)))))
