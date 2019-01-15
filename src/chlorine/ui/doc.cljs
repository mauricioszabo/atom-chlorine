(ns chlorine.ui.doc
  (:require [chlorine.repl :as repl]
            ; [repl-tooling.features.documentation :as doc]
            [chlorine.ui.inline-results :as inline]
            [repl-tooling.editor-helpers :as editor-helpers]
            [chlorine.ui.atom :as atom]))

(defn doc-for [editor ^js range var-name]
  (let [ns-name (repl/ns-for editor)
        var-name (symbol (str "#'" var-name))
        in-result (inline/new-result editor (.. range -end -row))
        code `(clojure.core/let [m# (clojure.core/meta ~var-name)]
                (clojure.core/str "-------------------------\n"
                                  (:ns m#) "/" (:name m#) "\n"
                                  (:arglists m#) "\n  "
                                  (:doc m#)))]
    (repl/evaluate-aux editor
                       ns-name
                       (.getFileName editor)
                       (.. range -start -row)
                       (.. range -start -column)
                       code
                       {:literal true}
                       #(repl/set-inline-result in-result %))))

(defn doc []
  (let [editor ^js (atom/current-editor)
        pos (.getCursorBufferPosition editor)]
    (doc-for editor #js {:start pos :end pos} (atom/current-var editor))))
