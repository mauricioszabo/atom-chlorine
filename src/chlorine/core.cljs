(ns chlorine.core
  (:require [chlorine.aux :as aux]
            [chlorine.ui.connection :as conn]
            [chlorine.providers-consumers.status-bar :as sbar]
            [chlorine.repl :as repl]
            [chlorine.features.refresh :as refresh]
            [chlorine.ui.doc :as doc]
            [chlorine.ui.console :as console]))

(def config #js {})

(defn- subscribe-editor-events [^js editor]
  (when (-> editor .getGrammar .-scopeName (= "source.clojure"))
    (.add ^js @aux/subscriptions (.onDidSave editor #(refresh/run-refresh!)))))

(defn- observe-editors []
  (.add @aux/subscriptions
        (.. js/atom -workspace
            (observeTextEditors subscribe-editor-events))))

(defn activate [s]
  (aux/reload-subscriptions!)
  (observe-editors)
  (console/register-console)

  (aux/command-for "connect-clojure-socket-repl" conn/connect!)
  (aux/command-for "connect-clojurescript-socket-repl" identity)
  (aux/command-for "connect-self-hosted-clojurescript-repl" conn/connect-self-hosted!)
  (aux/command-for "disconnect" conn/disconnect!)

  (aux/command-for "evaluate-block" #(repl/evaluate-block!))
  (aux/command-for "evaluate-top-block" #(repl/evaluate-top-block!))
  (aux/command-for "evaluate-selection" #(repl/evaluate-selection!))
  (aux/command-for "doc-for-var" doc/doc)
  (aux/command-for "clear-console" console/clear))

(defn deactivate [s]
  (.dispose ^js @aux/subscriptions))
  ; (some-> @sbar/status-bar-tile .destroy))

(defn before [done]
  (deactivate nil)
  (done)
  (activate nil)
  (.. js/atom -notifications (addSuccess "Reloaded Chlorine"))
  (println "Reloaded"))
