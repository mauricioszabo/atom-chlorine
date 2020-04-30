(ns chlorine.ui.console
  (:require [reagent.dom :as rdom]
            [repl-tooling.editor-integration.renderer.console :as console]
            [chlorine.utils :as aux]
            [chlorine.state :refer [state]]))

(defonce ^:private console-pair
  (do
    (deftype ^js ConsoleClass []
      Object
      (getTitle [_] "Chlorine REPL")
      (destroy [this]
        (-> (filter #(.. ^js % getItems (includes this))
                    (.. js/atom -workspace getPanes))
            first
            (some-> (.removeItem this)))))
    [ConsoleClass  (ConsoleClass.)]))
(def ^:private Console (first console-pair))
(def ^:private console (second console-pair))

(defn open-console [split destroy-fn]
  (let [active (. js/document -activeElement)]
    (aset console "destroy" destroy-fn)
    (.. js/atom
        -workspace
        (open "atom://chlorine-terminal" #js {:split split
                                              :searchAllPanes true
                                              :activatePane false
                                              :activateItem false})
        (then #(.focus active)))))

(defn register-console! [^js subs]
  (let [scrolled? (atom true)
        con (with-meta console/console-view
              {:get-snapshot-before-update #(reset! scrolled? (console/all-scrolled?))
               :component-did-update #(console/scroll-to-end! scrolled?)})]
    (rdom/render [con "native-key-bindings"] console/div)
    (.add subs
          (.. js/atom -workspace
              (addOpener (fn [uri] (when (= uri "atom://chlorine-terminal") console)))))
    (.add subs (.. js/atom -views (addViewProvider Console (constantly console/div))))))

(defonce registered
  (register-console! @aux/subscriptions))

(defn result [parsed-result]
  (console/result parsed-result (:parse @state)))
