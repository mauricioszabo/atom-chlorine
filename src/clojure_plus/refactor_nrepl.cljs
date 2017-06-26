(ns clojure-plus.refactor-nrepl
  (:require [clojure-plus.repl :as repl]
            [clojure-plus.ui.select-view :as sv]
            [cljs.reader :as edn]
            [clojure.string :as str]))

(def TextEditor (.-TextEditor (js/require "atom")))
(def ^:private tmp (.tmpdir (js/require "os")))
(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn range-and-var [editor]
  (let [regex #"[a-zA-Z0-9\-.$!?\/><*]+"
        range (-> editor .getLastCursor (.getCurrentWordBufferRange #js {:wordRegex regex}))]
    [range (.getTextInBufferRange editor range)]))

(defn- error-msg [title msg]
  (-> js/atom .-notifications (.addError title #js {:details msg})))

(defn- ns-range [editor]
  (let [top-levels (-> js/protoRepl .-EditorUtils (.getTopLevelRanges editor))]
    (first (filter #(re-find #"\(\s*ns" (.getTextInBufferRange editor %)) top-levels))))

(defn rewrite-ns [editor text]
  (let [range (ns-range editor)]
    (.setTextInBufferRange editor range text)))

(defn- format-require [[key & rest]]
  ; 2 spaces, 1 parenthesis, and one space
  (if (empty? rest)
    (str "  (" key ")")
    (let [indent (->> (+ 4 (count (str key)))
                      range
                      (map (constantly " "))
                      str/join
                      (str "\n"))]
      (str "  (" key " " (str/join indent rest) ")"))))


(defn format-ns [[_ ns-name & requires]]
  (str "(ns " ns-name "\n"
       (->> requires (map format-require) (str/join "\n"))
       ")"))

(defn organize-ns [editor]
  (let [temp-file (.join path tmp (str "tmp_" (gensym) ".clj"))
        cmd `(~'clojure.core/with-bindings
               {#'refactor-nrepl.config/*config* {:debug false,
                                                  :prune-ns-form true}}
               (refactor-nrepl.ns.clean-ns/clean-ns {:path ~temp-file}))]
    (.writeFileSync fs temp-file (.getText editor))
    (repl/execute-cmd cmd "user" (fn [res]
                                   (println "RESULT" res)
                                   (if-let [ns-res (:value res)]
                                       (try
                                         (rewrite-ns editor (format-ns ns-res))
                                         (finally
                                           (.unlink fs temp-file)))
                                       (error-msg "Error while rewriting import"
                                                  (or (:error res) res)))))))

(defn add-require [editor require-str]
  (let [range (ns-range editor)
        ns-txt (.getTextInBufferRange editor range)
        new-ns (if (re-find #"\(\s*:?require" ns-txt)
                 (str/replace-first ns-txt
                                    #"\(\s*:?require"
                                    (str "(:require " require-str))
                 (str/replace ns-txt #"\)" (str "(:require " require-str "))")))]
    (->> new-ns edn/read-string format-ns (rewrite-ns editor))))

(defn- change-editor-ns! [editor ns-string ns-alias range]
  (let [to-replace (-> editor
                       (.getTextInBufferRange range)
                       (str/replace #"^(.*/)?" (str ns-alias "/")))]
    (.transact editor #(do
                         (.setTextInBufferRange editor range to-replace)
                         (add-require editor ns-string)))))

(defn simple-popup [placeholder]
  (js/Promise.
   (fn [resolve]
     (let [editor (TextEditor. #js {:mini true :placeholderText placeholder})
           panel (-> js/atom .-workspace (.addModalPanel #js {:item editor}))
           cadd #(-> js/atom .-commands (.add (.-element editor) %1 %2))]
       (cadd "core:cancel" #(do (resolve nil) (.destroy panel)))
       (cadd "core:confirm" #(do (resolve (.getText editor)) (.destroy panel)))))))

(defn- define-alias! [editor range ns-name]
  (.then (simple-popup "type your namespace alias")
         #(when-let [alias-name %]
            (change-editor-ns! editor
                               (str "[" ns-name " :as " alias-name "]")
                               alias-name
                               range))))

(defn- show-view [editor range values]
  (let [ns->str (fn [[ns-name alias]]
                  (str "[" ns-name " :as " (or alias "[no alias]") "]"))
        ns->fn (fn [ns-spec]
                 #(if-let [alias (second ns-spec)]
                    (change-editor-ns! editor (ns->str ns-spec) alias range)
                    (define-alias! editor range (first ns-spec))))]
    (->> values
         (map (fn [ns-spec] {:label (ns->str ns-spec) :run (ns->fn ns-spec)}))
         sv/select-view)))

(defn missing-view [editor range symbol-name]
  (repl/execute-cmd `(clj.--check-deps--/resolve-missing ~symbol-name)
                    (fn [res]
                      (if-let [values (:value res)]
                        (show-view editor range (distinct values))
                        (error-msg "No value found for import" res)))))

(defn find-missing-symbol! [editor]
  (let [[range var] (range-and-var editor)]
    (missing-view editor range var)))
