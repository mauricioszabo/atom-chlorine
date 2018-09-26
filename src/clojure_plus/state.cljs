(ns clojure-plus.state
  (:require [reagent.core :as r]))

; Eval-mode is: discover, clj, cljs
(defonce state (r/atom {:repls {:clj-eval nil
                                :cljs-eval nil
                                :clj-aux nil
                                :cljs-aux nil}
                        :eval-mode :discover}))
