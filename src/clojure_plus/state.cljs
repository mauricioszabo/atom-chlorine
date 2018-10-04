(ns clojure-plus.state
  (:require [reagent.core :as r]))

; Eval-mode is: discover, clj, cljs
(defonce state
  (r/atom {:repls {:clj-eval nil
                   :cljs-eval nil
                   :clj-aux nil}
           :eval-mode :discover
           :refresh {:mode :full
                     :needs-clear? false
                     :on-save? true}}))
