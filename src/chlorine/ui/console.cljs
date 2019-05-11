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

(defn open-console [split destroy-fn]
  (.. @console
      (open #js {:split split :searchAllPanes true :activatePane false
                 :activateItem false})
      (then #(aset % "destroy" destroy-fn))))
