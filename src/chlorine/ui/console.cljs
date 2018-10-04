(ns chlorine.ui.console
  (:require [chlorine.ui.inline-results :as inline]
            [chlorine.aux :as aux]))

(def console-uri "atom://chlorine/console")

(def console
  (delay
   (some-> @inline/ink .-Console
           (.fromId "chlorine")
           (doto
            (.getTitle (fn [] "Clojure REPL"))
            (.activate)
            (.onEval (fn [ed] (prn [:INSERTED ed])))
            (.setModes (clj->js [{:name "chlorine"
                                  :default true
                                  :grammar "source.clojure"}]))))))

(defn register-console []
  (.add @aux/subscriptions
        (.. js/atom -workspace (addOpener (fn [uri]
                                            (when (= uri console-uri)
                                              @console))))))
