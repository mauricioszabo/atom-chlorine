(ns clojure-plus.test-helpers)

(defmacro create-text-editor [editor-var & forms]
  `(create-text-editor* (fn [~editor-var] ~@forms)))

(defmacro with-text-editor [editor-var & forms]
  `(with-text-editor* (fn [~editor-var] ~@forms)))
