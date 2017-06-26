(ns clojure-plus.test-helpers)

(def ^:private tmp (.tmpdir (js/require "os")))
(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn create-text-editor* [f]
  (-> js/atom .-workspace (.open (str "test_" (gensym) ".clj"))
      (.then f)))

(defn with-text-editor* [f]
  (create-text-editor* (fn [editor]
                         (try
                           (f editor)
                           (finally
                             (.destroy editor))))))

(def set-text #(.setText %1 %2))
(def text #(.getText %))
