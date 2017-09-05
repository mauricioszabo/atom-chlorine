(ns clojure-plus.repl
  (:require [cljs.reader :as edn]))

(def repl (-> (js/require "../../../clojure-plus")
              .getCommands .-promisedRepl))

(defn execute-cmd
  ([cmd callback] (execute-cmd cmd {} callback))
  ([cmd ns-or-opts callback]
   (-> repl
       (.syncRun (str cmd) ns-or-opts)
       (.then #(if-let [val (.-value %)]
                 (callback {:value (edn/read-string val)})
                 (callback {:error (or (.-error %) %)}))))))

(defn- evaluate-clj [cmd ns-name opts callback]
  (let [cmd (str "(try {:value " cmd "}\n(catch Throwable e"
                 "{:error {:cause (str e)"
                 "         :trace (map clj.--check-deps--/prettify-stack (.getStackTrace e))}}))")]
    (-> repl
        (.syncRun cmd ns-name opts)
        (.then #(if-let [val (.-value %)]
                  (callback (edn/read-string val))
                  (callback {:error {:cause (or (.-error %) %)}}))))))

(defn evaluate [cmd ns-name opts callback]
  (if (= "cljs" (:session opts))
    :foo
    (evaluate-clj cmd ns-name opts callback)))
