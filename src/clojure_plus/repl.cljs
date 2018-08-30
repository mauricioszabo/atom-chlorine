(ns clojure-plus.repl
  (:require [cljs.reader :as edn]
            [repl-tooling.eval :as eval]
            [repl-tooling.repl-client.clojure :as clj-repl]
            [clojure-plus.state :refer [state]]
            [clojure-plus.ui.inline-results :as inline]))

(defn connect! [host port]
  (let [r1 (clj-repl/repl :clj-eval host port println)
        r2 (clj-repl/repl :clj-aux host port println)]
    (swap! state #(-> %
                      (assoc-in [:repls :clj-eval] r1)
                      (assoc-in [:repls :clj-aux] r2)))))

(defn evaluate [editor ns-name row col code callback]
  (some-> @state :repls :clj-eval
          (eval/evaluate code
                         {:ns ns-name :row row :col col}
                         callback)))

(defn set-inline-result [inline-result eval-result]
  (let [tree (inline/parse (:result eval-result))]
    (inline/set-content! inline-result tree)))

(defn eval-and-present [editor ns-name filename row col code]
  (let [result (inline/new-result editor row)]
    (some-> @state :repls :clj-eval
            (eval/evaluate code
                           {:ns ns-name :row row :col col :filename filename}
                           #(set-inline-result result %)))))

; (defonce repl (-> (js/require "../clojure-plus")
;                   .getCommands .-promisedRepl))
;
; (defn execute-cmd
;   ([cmd callback] (execute-cmd cmd {} callback))
;   ([cmd ns-or-opts callback]
;    (-> repl
;        (.syncRun (str cmd) ns-or-opts)
;        (.then #(if-let [val (.-value %)]
;                  (callback {:value (edn/read-string val)})
;                  (callback {:error (or (.-error %) %)}))))))
;
; (defn- evaluate-cljs [cmd ns-name opts callback]
;   (-> repl
;       (.syncRun cmd ns-name (clj->js opts))
;       (.then #(if-let [val (.-value %)]
;                 (callback {:value val})
;                 (callback {:error {:cause (or (.-error %) %)}})))))
;
; (defn- evaluate-clj [cmd ns-name opts callback]
;   (let [cmd (str "(try " cmd "\n(catch Throwable e"
;                  "{:error {:cause (str e)"
;                  "         :trace (map clj.--check-deps--/prettify-stack (.getStackTrace e))}}))")]
;     (-> repl
;         (.syncRun cmd ns-name (clj->js opts))
;         (.then #(if-let [val (.-value %)]
;                   (callback {:value val})
;                   (callback {:error {:cause (or (.-error %) %)}}))))))
;
; (defn evaluate [cmd ns-name opts callback]
;   (if (= "cljs" (:session opts))
;     (evaluate-cljs cmd ns-name opts callback)
;     (evaluate-clj cmd ns-name opts callback)))
;
; (defn- get-range [editor which]
;   (case which
;     :top-level (-> js/protoRepl .-EditorUtils
;                   (.getCursorInBlockRange editor #js {:topLevel true}))
;     :selection (.getSelectedBufferRange editor)
;     (-> js/protoRepl .-EditorUtils (.getCursorInBlockRange editor))))
;
; (defn- editor-and-range [opts]
;   (let [editor (-> js/atom .-workspace .getActiveTextEditor)]
;     [editor (get-range editor (:scope opts))]))
;
; (defn- create-inline-result [value result]
;   (inline/set-content! result (inline/parse value)))
;
; ; @(delay clojure-plus.repl-test/e)
; ; (defn run-code-on-editor [opts]
; ;   (let [[editor range] (editor-and-range opts)
; ;         line (-> range .-end .-row)
; ;         command (. editor getTextInBufferRange range)
; ;         result (inline/new-result editor line)]
; ;     (evaluate command (-> js/protoRepl .-EditorUtils (.findNsDeclaration editor)) opts
; ;               (fn [{:keys [value error]}]
; ;                 (if value
; ;                   (create-inline-result value result))))))
;
; (def active-repls (atom nil))
;
; (defn- command-and-result [opts]
;   (let [[editor range] (editor-and-range opts)
;         line (-> range .-end .-row)
;         command (. editor getTextInBufferRange range)
;         result (inline/new-result editor line)]
;     [command result (-> js/protoRepl .-EditorUtils (.findNsDeclaration editor))]))
;
; (defn- eval-in-socket [ns-name command result opts]
;   (let [repl (:repl @active-repls)]
;     (eval/evaluate repl command opts
;                    (fn [res]
;                      (if (:result res)
;                        (create-inline-result (:result res) result))))))
;
; (defn run-code-on-editor [opts]
;   (let [[command result ns-name] (command-and-result opts)]
;     (if (:repl @active-repls)
;       (eval-in-socket ns-name command result opts)
;       (evaluate command ns-name opts
;                 (fn [{:keys [value error]}]
;                   (prn value error)
;                   (if value
;                     (create-inline-result value result)))))))
; ;
; ; (js/setTimeout
; ;  #(run-code-on-editor {:scope :top-level})
; ;  2000)
; ;
; ; (eval-in-socket "user" "(/ 1 0)" nil {})
; ; (evaluate "(+ 1 2)" "user" {}
; ;                  (fn [{:keys [value error]}]
; ;                    (prn value error)))
; ;                    ; (if value
; ;                    ;   (create-inline-result value result))))
; ;
; ; (+ 2 3)
; ; ; (eval-in-socket 'user "(+ 1 2)" nil {})
