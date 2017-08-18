(ns clojure-plus.ui.inline-results-test
  (:require [clojure.test :refer-macros [deftest is testing run-tests]]
            [clojure-plus.ui.inline-results :as inline]
            [clojure.walk :as walk]))

(deftest parse-edns
  (testing "will parse strings and flat structures"
    (is (= ["\"foo\""] (inline/parse "\"foo\"")))
    (is (= [":bar"] (inline/parse ":bar"))))

  (testing "will parse maps into columns"
    (is (= ["{:a 20}" [[:row [["[:a" [[":a"]]]
                              ["20]" [["20"]]]]]]]
           (inline/parse "{:a 20}"))))

  (testing "will parse colls"
    (is (= ["[1 2 3]" [["1"]
                       ["2"]
                       ["3"]]]
           (inline/parse "[1 2 3]")))))

(run-tests)
