(ns chlorine.core
  (:require [chlorine.utils :as aux]
            [chlorine.ui.connection :as conn]
            [chlorine.repl :as repl]
            [chlorine.features.refresh :as refresh]
            [chlorine.configs :as configs]
            [chlorine.ui.console :as console]
            [chlorine.features.code :as code]
            [schema.core :as s]))

(def config (configs/get-configs))

(defn- subscribe-editor-events [^js editor]
  (when (-> editor .getGrammar .-scopeName (= "source.clojure"))
    (.add ^js @aux/subscriptions (.onDidSave editor #(refresh/run-editor-refresh!)))))

(defn- observe-editors []
  (.add @aux/subscriptions
        (.. js/atom -workspace
            (observeTextEditors subscribe-editor-events))))

(defn- install-dependencies-maybe []
  (.install (js/require "atom-package-deps") "chlorine"))

(def commands
  (fn []
    (clj->js {:connect-clojure-socket-repl conn/connect-socket!
              :connect-socket-repl conn/connect-socket!
              :clear-inline-results repl/clear-inline!
              :clear-console console/clear

              :inspect-block repl/inspect-block!
              :inspect-top-block repl/inspect-top-block!

              :refresh-namespaces refresh/run-refresh!
              :toggle-refresh-mode refresh/toggle-refresh

              :go-to-var-definition code/goto-var})))

(def aux #js {:deps install-dependencies-maybe
              :reload aux/reload-subscriptions!
              :observe_editor observe-editors
              :observe_config configs/observe-configs!
              :get_disposable (fn [] @aux/subscriptions)})

(defn before [done]
  (let [main (.. js/atom -packages (getActivePackage "chlorine") -mainModule)]
    (.deactivate main)
    (done)))

(defn after []
  (let [main (.. js/atom -packages (getActivePackage "chlorine") -mainModule)]
    (.activate main)
    (.. js/atom -notifications (addSuccess "Reloaded Chlorine"))
    (println "Reloaded")))

#_
(s/set-fn-validation! js/goog.DEBUG)
