(ns chlorine.core
  (:require [chlorine.aux :as aux]
            [chlorine.ui.connection :as conn]
            [chlorine.providers-consumers.status-bar :as sbar]
            [chlorine.repl :as repl]
            [chlorine.features.refresh :as refresh]
            [chlorine.ui.doc :as doc]
            [chlorine.configs :as configs]
            [chlorine.ui.atom :as atom]
            [chlorine.ui.console :as console]
            [chlorine.features.code :as code]))

(def config (configs/get-configs))

(defn- subscribe-editor-events [^js editor]
  (when (-> editor .getGrammar .-scopeName (= "source.clojure"))
    (.add ^js @aux/subscriptions (.onDidSave editor #(refresh/run-editor-refresh!)))))

(defn- observe-editors []
  (.add @aux/subscriptions
        (.. js/atom -workspace
            (observeTextEditors subscribe-editor-events))))

(defn- install-dependencies-maybe []
  (-> (.install (js/require "atom-package-deps") "chlorine")))

(def commands
  (fn []
    (clj->js {:connect-clojure-socket-repl conn/connect!
              :connect-clojurescript-socket-repl conn/connect-cljs!
              :connect-embeded-clojurescript-repl conn/connect-self-hosted!
              ; :disconnect conn/disconnect!

              :evaluate-block repl/evaluate-block!
              ; :evaluate-top-block repl/evaluate-top-block!
              ; :evaluate-selection repl/evaluate-selection!
              :doc-for-var doc/doc
              :source-for-var repl/source-for-var!
              :clear-console console/clear

              ; :load-file repl/load-file!

              :run-tests-in-ns repl/run-tests-in-ns!
              :run-test-for-var repl/run-test-at-cursor!

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
