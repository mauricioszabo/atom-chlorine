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

(deftest inline-result
  (testing "will show an inline result at specific line"
    (is (= 1 2))))

(run-tests)

(defn- to-clj-array [element]
  (if-let [nodes (.-childNodes element)]
    [(.-nodeName element) (-> js/Array .-prototype .-slice (.call nodes) js->clj)]
    element))

(comment
 (def r
   (doto (.createElement js/document "div")
         (aset "innerHTML"
               "<div class\"ink tree\"><span class=\"icon icon-chevron-right\"></span><div class=\"header gutted closed\">foo</div><div class=\"body gutted\" style=\"display: none;\"></div></div>")))
 (walk/prewalk to-clj-array r)
 (mapv to-clj-array (to-clj-array r))
 (.log js/console r)
 (-> js/Array .-prototype .-slice (.call (.-childNodes r)) js->clj)
 (concat [] (.-childNodes r))
 (.-children r)
 (.-innerText r))
 ; (.createElement js/document "div"))
