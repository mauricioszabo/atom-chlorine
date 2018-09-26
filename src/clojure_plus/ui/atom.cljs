(ns clojure-plus.ui.atom)

(defn warn [title text]
  (.. js/atom -notifications (addWarning title #js {:detail text})))

(defn error [title text]
  (.. js/atom -notifications (addError title #js {:detail text})))

(defn info [title text]
  (.. js/atom -notifications (addInfo title #js {:detail text})))
