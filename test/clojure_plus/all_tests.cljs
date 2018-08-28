(ns ^:figwheel-always clojure-plus.all-tests)
;   (:require [clojure.test :refer-macros [run-tests]]
;             ; [figwheel.client.utils :as fig.utils]
;             [clojure-plus.helpers-test]
;             [clojure-plus.modifications-test]
;             [clojure-plus.repl-test]
;             [clojure-plus.refactor-nrepl-test]
;             [clojure-plus.ui.inline-results-test :as in]))
;
; (def vm (js/require "vm"))
;
; (defn eval-helper [code {:keys [eval-fn] :as opts}]
;     ((fn [src] (.runInThisContext vm src)) code))
;
; ; (set! (-> js/figwheel .-client .-utils .-eval_helper) eval-helper)
;
; (set! js/__dirname (str (-> js/atom .-packages .getPackageDirPaths)
;                         "/clojure-plus/lib/js/foo/bar"))
;
; (defn main [ & args]
;   (println "I rise, again"))
;
; (defn run-all []
;   (println "I rise! Yeah!"))
;
(defn before []
  (js/console.warn "start called?"))

(defn after []
  (js/console.warn "DONE called"))
;
; (defn activate [state]
;   (prn state))
