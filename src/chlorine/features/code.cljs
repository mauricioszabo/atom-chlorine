(ns chlorine.features.code
  (:require [chlorine.ui.atom :as atom]
            [chlorine.repl :as repl]
            [chlorine.state :as state]
            [repl-tooling.features.definition :as definition]))

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
        namespace (repl/ns-for editor)]
    (if (repl/need-cljs? editor)
      (atom/warn "Can't go to definition on a CLJS file" "")
      (.. (some-> @state/state :repls :clj-aux
                  (definition/find-var-definition namespace var))
          (then (fn [info]
                  (if info
                    (open-editor info)
                    (atom/error "Could not find definition for var" ""))))))))
