(ns chlorine.core
  (:require [chlorine.utils :as aux]
            [schema.core :as s]
            [chlorine.ui.connection :as conn]
            [chlorine.repl :as repl]
            [chlorine.features.refresh :as refresh]
            [chlorine.configs :as configs]
            [clojure.string :as str]
            [repl-tooling.editor-integration.renderer.console :as console]))

(def config (configs/get-configs))

(defn- subscribe-editor-events [^js editor]
  (when (and (-> editor .getGrammar .-scopeName (= "source.clojure"))
             (not (str/ends-with? (.getPath editor) "edn")))
    (.add ^js @aux/subscriptions (.onDidSave editor #(refresh/run-editor-refresh!)))))

(defn- observe-editors []
  (.add @aux/subscriptions
        (.. js/atom -workspace
            (observeTextEditors subscribe-editor-events))))

(def commands
  (fn []
    (clj->js {:connect-socket-repl conn/connect-socket!
              :clear-inline-results repl/clear-inline!
              :clear-console console/clear

              :inspect-block repl/inspect-block!
              :inspect-top-block repl/inspect-top-block!

              :refresh-namespaces refresh/run-refresh!
              :toggle-refresh-mode refresh/toggle-refresh})))

(def aux #js {:reload aux/reload-subscriptions!
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

(s/set-fn-validation! (.-DEBUG js/goog))
