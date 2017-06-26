(ns ^:figwheel-always clojure-plus.helpers-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [clojure-plus.helpers :as helpers]
            [clojure-plus.core :as core]))

(def foo-element (.createElement js/document "foo"))

(defn find-commands [name]
  (-> js/atom
      .-commands
      (.findCommands #js {:target foo-element})
      js->clj
      (->> (filter #(= (% "displayName") name)))))

(deftest commands
  (helpers/add-command "foo" "Sample Test" #(identity 10))
  (testing "command is on global"
    (is (not-empty (find-commands "Sample Test"))))

  (helpers/remove-all-commands)
  (testing "removes command"
    (is (empty? (find-commands "Sample Test")))))

(run-tests)
