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
  (-> (.install (js/require "atom-package-deps") "chlorine")
      (.then #(atom/info "All dependencies installed." ""))))

(defn activate [s]
  (install-dependencies-maybe)
  (aux/reload-subscriptions!)
  (observe-editors)
  (console/register-console)
  (configs/observe-configs!)

  (aux/command-for "connect-clojure-socket-repl" conn/connect!)
  (aux/command-for "connect-clojurescript-socket-repl" conn/connect-cljs!)
  (aux/command-for "connect-embeded-clojurescript-repl" conn/connect-self-hosted!)
  (aux/command-for "disconnect" conn/disconnect!)

  (aux/command-for "evaluate-block" #(repl/evaluate-block!))
  (aux/command-for "evaluate-top-block" #(repl/evaluate-top-block!))
  (aux/command-for "evaluate-selection" #(repl/evaluate-selection!))
  (aux/command-for "doc-for-var" doc/doc)
  (aux/command-for "source-for-var" #(repl/source-for-var!))
  (aux/command-for "clear-console" console/clear)

  (aux/command-for "load-file" #(repl/load-file!))

  (aux/command-for "run-tests-in-ns" #(repl/run-tests-in-ns!))
  (aux/command-for "run-test-for-var" #(repl/run-test-at-cursor!))

  (aux/command-for "inspect-block" #(repl/inspect-block!))
  (aux/command-for "inspect-top-block" #(repl/inspect-top-block!))

  (aux/command-for "refresh-namespaces" refresh/run-refresh!)
  (aux/command-for "toggle-refresh-mode" refresh/toggle-refresh)

  (aux/command-for "go-to-var-definition" code/goto-var))

(defn deactivate [s]
  (.dispose ^js @aux/subscriptions))
  ; (some-> @sbar/status-bar-tile .destroy))

(defn before [done]
  (deactivate nil)
  (done)
  (activate nil)
  (.. js/atom -notifications (addSuccess "Reloaded Chlorine"))
  (println "Reloaded"))
