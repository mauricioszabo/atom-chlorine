(ns clojure-plus.modifications-test
 (:require [clojure.test :refer-macros [deftest testing is run-tests]]
           [clojure-plus.modifications :as mods]))

(def clj-text "
(let [a (+ b c)]
 (inc a))")

(deftest rewrite
  (testing "rewriting SEXP from some code"
    (is (= "
(let [a (+ (println b) (pp c))]
 (inc a))" (mods/rewrite-txt clj-text
                             [{:replace "(println __SEL__)" :start 12 :end 13}
                              {:replace "(pp __SEL__)" :start 14 :end 15}])))))

(run-tests)
