(ns clojure-plus.providers-consumers.ink
  (:require [clojure-plus.ui.inline-results :as inline]))

(defn activate [s]
  (reset! inline/ink s))
