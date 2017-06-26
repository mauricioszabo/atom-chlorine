(ns clojure-plus.watches.modifications
  (:require [clojure.string :as str]))

(defn rewrite-once [text {:keys [replace start end]}]
  (str (subs text 0 start)
       (str/replace replace "__SEL__" (subs text start end))
       (subs text end)))

(defn rewrite-txt [original-txt replacements]
  (let [sorted (sort-by #(- (:start %)) replacements)]
    (reduce rewrite-once original-txt sorted)))
