(ns clojure-plus.state
  (:require [reagent.core :as r]))

(defonce state (r/atom {:repls {:clj-eval nil
                                :cljs-eval nil
                                :clj-aux nil
                                :cljs-aux nil}}))

#_
(reset! state {:repls {:clj-eval nil
                       :cljs-eval nil
                       :clj-aux nil
                       :cljs-aux nil}})
