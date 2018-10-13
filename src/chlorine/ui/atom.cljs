(ns chlorine.ui.atom)

(defn warn [title text]
  (.. js/atom -notifications (addWarning title #js {:detail text})))

(defn error [title text]
  (.. js/atom -notifications (addError title #js {:detail text})))

(defn info [title text]
  (.. js/atom -notifications (addInfo title #js {:detail text})))

(defn current-editor []
  (.. js/atom -workspace getActiveTextEditor))

(defn current-pos [^js editor]
  (let [point (.getCursorBufferPosition editor)]
    [(.-row point) (.-column point)]))

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=_:]+")

(defn current-var [^js editor]
  (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex})))
