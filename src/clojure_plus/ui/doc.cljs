(ns clojure-plus.ui.doc
  (:require [clojure-plus.repl :as repl]
            [repl-tooling.editor-helpers :as editor-helpers]
            [clojure-plus.ui.atom :as atom]))

(defn doc-for [editor row var-name]
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
    (repl/eval-and-present editor ns-name (.getFileName editor) row 0 code)))

(defn doc []
  (let [editor (atom/current-editor)
        [row] (atom/current-pos editor)]
    (doc-for editor row (atom/current-var editor))))

; (-> js/atom .-workspace .getActiveTextEditor
;     (doc-for 11 "defrecord"))
