(ns clojure-plus.repl-test
  (:require [clojure.test :refer-macros [run-tests testing is deftest async]]
            [clojure-plus.repl :as repl]))

(deftest running-code-in-clj
  (async done
    (repl/execute-cmd '(+ 2 1)
                      "user"
                      (fn [res]
                        (is (= {:value 3} res))
                        (done)))))

(deftest exceptions-in-clj
  (async done
    (repl/evaluate "(/ 10 0)"
                   "user"
                   {}
                   (fn [res]
                     (is (contains? (:error res) :cause))
                     (is (contains? (:error res) :trace))
                     (done)))))

(deftest running-code-in-cljs
  (async done
    (repl/evaluate "(+ 2 1)"
                   "user"
                   {:session "cljs"}
                   (fn [res]
                     (is (= {:value 3} res))
                     (done)))))

(deftest exceptions-in-cljs
  (async done
    (repl/evaluate "(throw (js/Error. \"Some Error\"))"
                   "user"
                   {:session "cljs"}
                   (fn [res]
                     (def res res)
                     (is (contains? (:error res) :cause))
                    ;  (is (contains? (:error res) :trace))
                     (done)))))

(run-tests)
