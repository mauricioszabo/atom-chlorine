(ns chlorine.features.code
  (:require [chlorine.ui.atom :as atom]
            [chlorine.repl :as repl]
            [repl-tooling.editor-helpers :as helpers]
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
  (let [{:keys [contents range editor filename]} (repl/get-editor-data)
        [_ var] (helpers/current-var contents (first range))
        [_ namespace] (helpers/ns-range-for contents (first range))
        st (:tooling-state @state/state)
        aux (:clj/aux @st)
        repl (e-eval/repl-for (:editor/callbacks @st) st filename false)]
    (when-not
      (some-> repl (definition/find-var-definition aux namespace var)
              (.then (fn [info]
                       (if info
                         (open-editor info)
                         (atom/error "Could not find definition for var" "")))))
      (atom/error "Could not find definition for var" ""))))
