(ns clojure-plus.modifications
 (:require [clojure.string :as str]))

(def AtomRange (.-Range (js/require "atom")))

(defn rewrite-once [text {:keys [replace start end]}]
  (str (subs text 0 start)
       (str/replace replace "__SEL__" (subs text start end))
       (subs text end)))

(defn rewrite-txt [original-txt replacements]
  (let [sorted (sort-by #(- (:start %)) replacements)]
    (reduce rewrite-once original-txt sorted)))

;(defn rewrite [original-txt replacements]
;  (let [range (AtomRange.)]))
