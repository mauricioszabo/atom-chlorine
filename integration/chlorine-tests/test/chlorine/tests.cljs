(ns chlorine.tests
  (:require [cljs.core.async :as async :include-macros true :refer [<!]]
            [chlorine.aux :refer-macros [in-channel]]
            [clojure.test :refer [testing is deftest async run-all-tests run-tests]]
            [check.core :refer-macros [check]]
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

(defn- evaluate-command [command]
  (.. js/atom -commands (dispatch (. js/document -activeElement) command)))

(defn- with-clj [text]
  (async/go
   (evaluate-command "inline-results:clear-all")
   (with-text (<! (clj-editor)) 'user.test1 text)))

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

(deftest connection-and-evaluation
  (async done
    (async/go
     (evaluate-command "chlorine:disconnect")
     (<! (async/timeout 1000))
     (testing "connecting to editor"
       (<! (clj-editor))
       (<! (connect!))
       (prn :AFTER-CONNECT)
       (check (<! (find-element "div.message" #"REPL connected")) => exist?)
       (<! (async/timeout 1000)))

     (testing "connecting to editor"
       (with-clj "(str (+ 90 120))")
       (<! (async/timeout 100))
       (evaluate-command "chlorine:evaluate-top-block")
       (check (<! (find-inside-editor #"\"210\"")) => exist?))

     (testing "go to definition of a var"
       (prn :AFTER?)
       (with-clj "defn")
       (prn :AFTER?)
       (evaluate-command "chlorine:go-to-var-definition")
       (check (<! (find-inside-editor #":arglists")) => exist?)
       (evaluate-command "core:close")

      (done)))))

#_
(async/go
 (prn (<! (find-element "atom-text-editor div" #":arglists"))))


(defn run-my-tests []
  (run-tests))

(defn activate []
  (prn :RUNNING-TESTS)
  #_
  (run-all-tests)
  #_
  (async/go
    (let [e (<! (clj-editor))]
      (<! (with-text e 'test "(def foo 10)"))
      (<! (evaluate-command "core:select-all")))))

(defn after []
  (prn :RELOADED))
