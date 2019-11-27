(ns chlorine.ui.doc
  (:require [chlorine.repl :as repl]
            [chlorine.ui.inline-results :as inline]
            [chlorine.ui.atom :as atom]))

(defn doc-for [editor ^js range str-var-name]
  (let [ns-name (repl/ns-for editor)
        var-name (symbol (str "(clojure.core/resolve '" str-var-name ")"))
        in-result (inline/new-result editor (.. range -end -row))
        code `(~'clojure.core/let [v# (~'clojure.core/or ~var-name
                                                     (throw
                                                       (~'clojure.core/ex-info
                                                        (~'clojure.core/str "Unable to resolve var: " ~str-var-name
                                                                            " in this context in file " ~(.getFileName editor))
                                                        {})))
                                   m# (~'clojure.core/meta v#)]
                (~'clojure.core/str "-------------------------\n"
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
                       #(inline/render-inline! in-result %))))

(defn doc []
  (let [editor ^js (atom/current-editor)
        pos (.getCursorBufferPosition editor)]
    (doc-for editor #js {:start pos :end pos} (atom/current-var editor))))
