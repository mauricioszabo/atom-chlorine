(ns clojure-plus.socket-repl
  (:require [cljs.reader :as edn]
            [clojure-plus.ui.inline-results :as inline]
            [repl-tooling.repl-client.clojure :as repl]))

(defonce repl (-> (repl/repl :primary "localhost" 5556 println)
                  delay))

(defn execute-cmd
  ([cmd callback] (execute-cmd cmd {} callback))
  ([cmd ns-or-opts callback]
   (-> repl
       (.syncRun (str cmd) ns-or-opts)
       (.then #(if-let [val (.-value %)]
                 (callback {:value (edn/read-string val)})
                 (callback {:error (or (.-error %) %)}))))))

(defn- evaluate-cljs [cmd ns-name opts callback]
  (-> repl
      (.syncRun cmd ns-name (clj->js opts))
      (.then #(if-let [val (.-value %)]
                (callback {:value val})
                (callback {:error {:cause (or (.-error %) %)}})))))

(defn- evaluate-clj [cmd ns-name opts callback]
  (let [cmd (str "(try " cmd "\n(catch Throwable e"
                 "{:error {:cause (str e)"
                 "         :trace (map clj.--check-deps--/prettify-stack (.getStackTrace e))}}))")]
    (-> repl
        (.syncRun cmd ns-name (clj->js opts))
        (.then #(if-let [val (.-value %)]
                  (callback {:value val})
                  (callback {:error {:cause (or (.-error %) %)}}))))))

(defn evaluate [cmd ns-name opts callback]
  (if (= "cljs" (:session opts))
    (evaluate-cljs cmd ns-name opts callback)
    (evaluate-clj cmd ns-name opts callback)))

(defn- get-range [editor which]
  (case which
    :top-level (-> js/protoRepl .-EditorUtils
                  (.getCursorInBlockRange editor #js {:topLevel true}))
    :selection (.getSelectedBufferRange editor)
    (-> js/protoRepl .-EditorUtils (.getCursorInBlockRange editor))))

(defn- editor-and-range [opts]
  (let [editor (-> js/atom .-workspace .getActiveTextEditor)]
    [editor (get-range editor (:scope opts))]))

(defn- create-inline-result [value result]
  (inline/set-content! result (inline/parse value)))

(defn run-code-on-editor [opts]
  (let [[editor range] (editor-and-range opts)
        line (-> range .-end .-row)
        command (. editor getTextInBufferRange range)
        result (inline/new-result editor line)]
    (evaluate command (-> js/protoRepl .-EditorUtils (.findNsDeclaration editor)) opts
              (fn [{:keys [value error]}]
                (if value
                  (create-inline-result value result))))))
