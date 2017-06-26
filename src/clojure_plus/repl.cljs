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
