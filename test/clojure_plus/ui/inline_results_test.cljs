(ns clojure-plus.ui.inline-results-test
  (:require [clojure.test :refer-macros [deftest testing run-tests]]
            [check.core :refer-macros [check]]
            [clojure-plus.ui.inline-results :as inline]))

(deftest parse-edns
  (testing "will parse strings and flat structures"
    (check (inline/parse "\"foo\"") => ["\"foo\""])
    (check (inline/parse ":bar") => [":bar"]))

  (testing "will parse maps into columns"
    (check (inline/parse "{:a 20}")
           =>
           ["{:a 20}" [[:row [["[:a" [[":a"]]]
                              ["20]" [["20"]]]]]]]))

  (testing "will parse colls"
    (check (inline/parse "[1 2 3]")
           =>
           ["[1 2 3]" [["1"]
                       ["2"]
                       ["3"]]]))

  (testing "will parse reader tags"
    (check (inline/parse "#array [1 2 3]")
           =>
           ["#array [1 2 3]" [["1"] ["2"] ["3"]]])

    (check (inline/parse "#map {:a 10}")
           =>
           ["#map {:a 10}" [[:row [["[:a" [[":a"]]]
                                   ["10]" [["10"]]]]]]])

    (check (inline/parse "(#foo :a)")
           =>
           ["(#foo :a)" [["#foo :a"]]]))

  (testing "will understand repl-tooling's more"
    (check (first (inline/parse "(0 1 2 {:repl-tooling/... (unrepl$blahblah)})"))
           => #"\(0 1 2 \.\.\.\)")))

(run-tests)
