(ns chlorine.features.code
  (:require [chlorine.ui.atom :as atom]
            [chlorine.repl :as repl]
            [chlorine.state :as state]
            [repl-tooling.features.definition :as definition]
            [repl-tooling.editor-integration.evaluation :as e-eval]))

(defn- open-ro-editor [file-name line contents]
  (.. js/atom
      -workspace
      (open file-name #js {:initialLine line})
      (then #(doto ^js %
                   (aset "isModified" (constantly false))
                   (aset "save" (fn [ & _] (atom/warn "Can't save readonly editor" "")))
                   (.setText contents)
                   (.setReadOnly true)
                   (.setCursorBufferPosition #js [line 0])))))

(defn- open-editor [{:keys [file-name line contents]}]
  (if contents
    (open-ro-editor file-name line contents)
    (.. js/atom -workspace (open file-name #js {:initialLine line}))))

(defn goto-var []
  (let [editor (atom/current-editor)
        var (atom/current-var editor)
        namespace (repl/ns-for editor)
        st (:tooling-state @state/state)
        aux (:clj/aux @st)
        repl (e-eval/repl-for (:editor/callbacks @st)
                              st
                              (.getPath editor)
                              true)]
    (when-not
      (some-> repl (definition/find-var-definition aux namespace var)
              (.then (fn [info]
                       (if info
                         (open-editor info)
                         (atom/error "Could not find definition for var" "")))))
      (atom/error "Could not find definition for var" ""))))
