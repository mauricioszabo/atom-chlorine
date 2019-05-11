(ns chlorine.ui.console
  (:require [chlorine.ui.inline-results :as inline]
            [chlorine.aux :as aux]
            [clojure.core.async :as async :include-macros true]))

(defn- from-console-id [^js ink]
  (-> ink .-Console
      (.fromId "chlorine-console")
      (doto
       (.setTitle "Chlorine REPL")
       (.onEval (fn [ed] (prn [:INSERTED ed])))
       (.setModes (clj->js [{:name "chlorine"
                             :default true
                             :grammar "source.clojure"}])))))

(def console (delay (some-> @inline/ink from-console-id)))

(defn clear []
  (some-> @console .reset))

(declare register-destroy)
(defn- destroy-fn [^js console callback]
  (async/go
   (async/<! (async/timeout 100))
   (if (-> console .-view .-parentElement .-parentElement)
     (register-destroy console callback)
     (callback))))

;; Infer JS problems...
(defn- on-did-destroy [^js pane callback]
  (.onDidDestroy pane callback))
(defn- register-destroy [^js console callback]
  (async/go-loop []
    (if-let [pane (.currentPane console)]
      (on-did-destroy pane #(destroy-fn console callback))
      (do
        (async/<! (async/timeout 100))
        (recur)))))

(defn open-console [split destroy-fn]
  (.. @console
      (open #js {:split split :searchAllPanes true})
      (then #(register-destroy @console destroy-fn))))
