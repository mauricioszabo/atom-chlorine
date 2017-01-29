(ns clj.check-deps-test
  (:require [clojure.test :refer :all]
            [clj.--check-deps-- :refer :all]))

(testing "tracing a clojure command"
  (is (= {:fn "clojure.core/conj"
          :file "clojure/core.clj"
          :line 20}
         (dissoc (clj-trace "clojure.core$conj__1234" 20) :link))))

(testing "traces a function with strange name"
  (is (= "clojure.core/conj"
         (:fn (clj-trace "clojure.core$eval1020$conj__1234" 20))))

  (is (= {:fn ""
          :file "clj/check_deps_test"
          :line 19}
        (clj-trace "clj.check_deps_test$eval39348$fn__39349$fn__39350$fn__39351" 19))))

(try
  ((fn []
     ((fn [a b]
        (/ a b))
      10 0)))
  (catch Exception e
    (->> (.getStackTrace e) (into []))))
