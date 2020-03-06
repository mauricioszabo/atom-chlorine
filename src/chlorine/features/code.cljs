(ns chlorine.features.code
  (:require [chlorine.ui.atom :as atom]))

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

(defn open-editor [{:keys [file-name line contents]}]
  (if contents
    (open-ro-editor file-name line contents)
    (.. js/atom -workspace (open file-name #js {:initialLine line}))))
