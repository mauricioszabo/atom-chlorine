(ns chlorine.aux
  (:require [clojure.core.async :as async]))

(defmacro in-channel [ & forms]
  `(let [~'chan (cljs.core.async/promise-chan)]
     ~@forms
     ~'chan))
