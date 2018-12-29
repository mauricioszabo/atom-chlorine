(ns chlorine.ui.console
  (:require [chlorine.ui.inline-results :as inline]
            [chlorine.aux :as aux]))

(def console-uri "atom://chlorine/console")

(defn- from-console-id [^js ink]
  (-> ink .-Console
      (.fromId "chlorine")
      (doto
       (.getTitle (fn [] "Clojure REPL"))
       (.activate)
       (.onEval (fn [ed] (prn [:INSERTED ed])))
       (.setModes (clj->js [{:name "chlorine"
                             :default true
                             :grammar "source.clojure"}])))))

(def console (delay (some-> @inline/ink from-console-id)))

(defn register-console []
  (.add @aux/subscriptions
        (.. js/atom -workspace (addOpener (fn [uri]
                                            (when (= uri console-uri)
                                              @console))))))

(defn clear []
  (some-> @console .reset))
