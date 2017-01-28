(ns clj.check-deps-test
  (:require [clojure.test :refer :all]
            [clj.--check-deps-- :refer :all]))

(testing "tracing a clojure command"
  (is (= {:fn "clojure.core/conj"
          :file "clojure/core.clj"
          :line 20}
         (dissoc (clj-trace "clojure.core$conj__1234" 20) :link))))

(testing "traces a function with strange name"
  (is (= "clojure.core/conj" (:fn (clj-trace "clojure.core$eval1020$conj__1234" 20)))))
