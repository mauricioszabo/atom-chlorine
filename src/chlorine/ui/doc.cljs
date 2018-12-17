(ns chlorine.ui.doc
  (:require [chlorine.repl :as repl]
            [repl-tooling.editor-helpers :as editor-helpers]
            [chlorine.ui.atom :as atom]))

(defn doc-for [editor range var-name]
  (let [ns-name (repl/ns-for editor)
        var-name (symbol (str "#'" var-name))
        code `(clojure.core/let [m# (clojure.core/meta ~var-name)]
                (clojure.core/symbol
                  (clojure.core/str "#repl-tooling/literal-render "
                    (clojure.core/pr-str
                      (clojure.core/str "-------------------------\n"
                        (:ns m#) "/" (:name m#) "\n"
                        (:arglists m#) "\n  "
                        (:doc m#))))))]
    (repl/eval-and-present editor ns-name (.getFileName editor) range code)))

(defn doc []
  (let [editor ^js (atom/current-editor)
        pos (.getCursorBufferPosition editor)]
    (doc-for editor #js {:start pos :end pos} (atom/current-var editor))))
