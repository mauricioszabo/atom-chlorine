(ns clojure-plus.core
  (:require [cljs.nodejs :as nodejs]
            [clojure-plus.repl :as repl]
            [clojure-plus.refactor-nrepl :as refactor]))

(defonce disposable
  (-> js/window (aget "clojure plus extensions") .-disposable))

(nodejs/enable-util-print!)

(defn command-for [name f]
  (let [disp (-> js/atom .-commands (.add "atom-text-editor"
                                          (str "clojure-plus:" name)
                                          f))]
    (.add disposable disp)))

(defn- current-editor []
  (-> js/atom .-workspace .getActiveTextEditor))

(.onDidConnect
 js/protoRepl
 (fn []
   (repl/execute-cmd '(require '[clojure.tools.nrepl])
                     "user"
                     (fn [res]
                       (println res)
                       (when (contains? res :value)
                         (command-for 'new-evaluate-block
                                      #(repl/run-code-on-editor {:scope :top-level}))

                         (command-for 'new-evaluate-top-block
                                      #(repl/run-code-on-editor {:scope :top-level}))

                         (command-for 'new-evaluate-selection
                                      #(repl/run-code-on-editor {:scope :selection})))))

   (repl/execute-cmd '(require '[refactor-nrepl.core])
                     "user"
                     (fn [res]
                       (when (contains? res :value)
                         (command-for 'organize-namespace
                                      #(refactor/organize-ns (current-editor)))
                         (command-for 'add-import-for-var
                                      #(refactor/find-missing-symbol! (current-editor)))
                         (command-for 'hotload-dependency
                                      #(refactor/hotload! (current-editor))))))))
