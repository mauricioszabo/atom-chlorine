(ns clojure-plus.core
  (:require [clojure-plus.aux :as aux]
            [clojure-plus.ui.connection :as conn]
            [clojure-plus.providers-consumers.status-bar :as sbar]
            [clojure-plus.repl :as repl]
            [clojure-plus.features.refresh :as refresh]
            [clojure-plus.ui.doc :as doc]))

(def config #js {})

(defn- subscribe-editor-events [editor]
  (when (-> editor .getGrammar .-scopeName (= "source.clojure"))
    (.add @aux/subscriptions (.onDidSave editor #(refresh/run-refresh!)))))
      ; editor.onDidChangeGrammar (e) =>
      ;   grammarCode(editor, e)
      ;   @updateStatusbar(atom.config.get("clojure-plus.simpleRefresh"), editor)
      ;
      ; editor.onDidSave =>
      ;   if atom.config.get('clojure-plus.refreshAfterSave') && editor.getGrammar().scopeName == 'source.clojure'
      ;     @getCommands().runRefresh()))

(defn- observe-editors []
  (.add @aux/subscriptions
        (.. js/atom -workspace
            (observeTextEditors subscribe-editor-events))))

(defn activate [s]
  (aux/reload-subscriptions!)
  (observe-editors)

  (aux/command-for "connect-clojure-socket-repl" conn/connect!)
  (aux/command-for "connect-clojurescript-socket-repl" identity)
  (aux/command-for "connect-self-hosted-clojurescript-repl" conn/connect-self-hosted!)
  (aux/command-for "disconnect" conn/disconnect!)

  (aux/command-for "evaluate-block" #(repl/evaluate-block!))
  (aux/command-for "evaluate-top-block" #(repl/evaluate-top-block!))
  (aux/command-for "evaluate-selection" #(repl/evaluate-selection!))
  (aux/command-for "doc-for-var" doc/doc))

(defn deactivate [s]
  (.dispose @aux/subscriptions))
  ; (some-> @sbar/status-bar-tile .destroy))

(defn before [done]
  (deactivate nil)
  (done)
  (activate nil)
  (println "Reloaded"))
