(ns clojure-plus.watches.modifications-test
  (:require [midje.sweet :refer :all]
            [clojure-plus.watches.modifications :as mods]))

(def clj-text "
(let [a (+ b c)]
  (inc a))
")

(facts "about SEXP rewriting"
  (fact "rewrites with some replace rules"
    (mods/rewrite-txt clj-text
                      [{:replace "(println __SEL__)" :start 12 :end 13}
                       {:replace "(pp __SEL__)" :start 14 :end 15}])
    => "
(let [a (+ (println b) (pp c))]
  (inc a))
"))
