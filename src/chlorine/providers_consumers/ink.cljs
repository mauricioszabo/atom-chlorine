(ns chlorine.providers-consumers.ink
  (:require [chlorine.ui.inline-results :as inline]))

(defn activate [s]
  (reset! inline/ink s))
