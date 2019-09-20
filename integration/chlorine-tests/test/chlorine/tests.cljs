(ns chlorine.tests
  (:require [cljs.core.async :as async :include-macros true :refer [<!]]
            [chlorine.aux :refer-macros [in-channel]]
            [clojure.test :refer [testing is deftest async run-all-tests run-tests]]
            [check.core :refer-macros [check]]
            [promesa.core :as p]
            ["remote" :as remote]))

(defn clj-editor []
  (in-channel
    (.. js/atom -workspace
        (open "/tmp/test.clj" #js {:searchAllPanes true})
        (then #(async/put! chan %)))))

(defn- editor []
  (.. js/atom -workspace getActiveTextEditor))

(defn- with-text [^js editor namespace text]
  (in-channel
   (.. editor
       (setText (str "(ns " namespace ")\n\n" text)))
   (.setSelectedBufferRange editor (clj->js [[2 2] [2 2]]))
   (async/put! chan :ok)))

(defn- await-for [fun]
  (in-channel
   (async/go-loop [n 0]
     (<! (async/timeout 100))
     (if (>= n 10)
       (async/put! chan false)
       (do
         (if-let [res (fun)]
           (async/put! chan res)
           (do
             (<! (async/timeout 100))
             (recur (inc n)))))))))

(defn- evaluate-command [command]
  (.. js/atom -commands (dispatch (. js/document -activeElement) command)))

(defn- with-clj [text]
  (async/go
   (let [editor (<! (clj-editor))]
     (evaluate-command "inline-results:clear-all")
     (with-text editor 'user.test1 text))))

(defn- as-vec [node-list]
  (map #(aget node-list %) (range (.-length node-list))))

(defn- query-all [selector]
  (as-vec (.. js/document (querySelectorAll selector))))

(defn- containing-text [selector match]
  (->> selector
       query-all
       (filter #(re-find match (.-innerText %)))
       first))

(defn- find-element [selector text]
  (await-for #(containing-text selector text)))

(defn- find-inside-editor [match]
  (find-element "atom-text-editor:not(*[style*='display: none']) div" match))

(defn- connect! []
  (async/go
   (evaluate-command "chlorine:connect-clojure-socket-repl")
   (let [elem (<! (await-for #(. js/document querySelector "input[placeholder=port]")))]
     ; (aset elem "value" 2233)
     (.onkeydown elem #js {:key "Enter"}))))

(def exist? #(not (nil? %)))

; (defn- timeout [n]
;   (js/Promise. #(js/setTimeout % n)))

(defn- dispatch-command [command]
  (.. js/atom -commands (dispatch (. js/document -activeElement) command)))

(defn clj-editor1 []
  (.. js/atom -workspace (open "/tmp/test.clj" #js {:searchAllPanes true})))

(deftest connection-and-evaluation
  (async done
    (p/alet
     [_ (evaluate-command "chlorine:disconnect")
      _ (p/delay 1000)])

    (testing "connecting to editor"
      (p/alet
       [editor (clj-editor1)
        _ (dispatch-command "chlorine:connect-clojure-socket-repl")]))))
      ;   message (find-element "div.message" #"REPL connected")])
      ; (check (<! (find-element "div.message" #"REPL connected")) => exist?))))
    ;   (<! (async/timeout 1000)))))

#_
(deftest connection-and-evaluation
  (async done
    (async/go
      (evaluate-command "chlorine:disconnect")
      (<! (async/timeout 1000))
      (testing "connecting to editor"
        (<! (clj-editor))
        (<! (connect!))
        (check (<! (find-element "div.message" #"REPL connected")) => exist?)
        (<! (async/timeout 1000)))

      (testing "evaluating simple forms"
        (with-clj "(str (+ 90 120))")
        (evaluate-command "chlorine:evaluate-top-block")
        (check (<! (find-inside-editor #"\"210\"")) => exist?)))

      ; (testing "go to definition of a var"
      ;   (with-clj "defn")
      ;   (evaluate-command "chlorine:go-to-var-definition")
      ;   (check (<! (find-inside-editor #":arglists")) => exist?)
      ;   (evaluate-command "core:close"))
      ;
      ; ; (testing "shows definition of var"
      ; ;   (with-clj "defn")
      ; ;   (evaluate-command "chlorine:source-for-var")
      ; ;   (check (<! (find-inside-editor #"fdecl")) => exist?))

    (done)))

(defn run-my-tests []
  (run-tests))

(defn activate []
  (prn :RUNNING-TESTS))
  ; #_
  ; (run-all-tests))

(defn after []
  (prn :RELOADED))
