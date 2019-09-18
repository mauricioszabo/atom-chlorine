(ns chlorine.tests
  (:require [cljs.core.async :as async :include-macros true]
            [chlorine.aux :refer-macros [in-channel]]
            [clojure.test :refer [testing is deftest async run-all-tests]]
            [check.core :refer-macros [check]]
            ["remote" :as remote]))

(defn clj-editor []
  (in-channel
    (.. js/atom -workspace
        (open "/tmp/test.clj")
        (then #(do
                 (.setText ^js % "(ns test)")
                 (async/put! chan %))))))

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

(defn- await-for [fun]
  (prn :FIRST-PRESENTER fun)
  (in-channel
   (async/go-loop [n 0]
     (prn :LOOP n)
     (async/<! (async/timeout 100))
     (if (>= n 10)
       (async/put! chan false)
       (do
         (prn :RES (fun))
         (if-let [res (fun)]
           (async/put! chan res)
           (do
             (async/<! (async/timeout 100))
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

(defn- connect! []
  (async/go
   (evaluate-command "chlorine:connect-clojure-socket-repl")
   (let [elem (async/<! (await-for #(. js/document querySelector "input[placeholder=port]")))]
     ; (aset elem "value" 2233)
     (.onkeydown elem #js {:key "Enter"}))))

(def exist? #(not (nil? %)))

(deftest connection-and-evaluation
  (async done
    (async/go
     (testing "connecting to editor"
       (async/<! (clj-editor))
       (async/<! (connect!))
       (prn :AFTER-CONNECT)
       (check (async/<! (find-element "div.message" #"REPL connected")) => exist?))

     (testing "connecting to editor"
       (with-text (async/<! (clj-editor)) 'user.test1 "(str (+ 90 120))")
       (async/<! (timeout 100))
       (evaluate-command "chlorine:evaluate-top-block")
       (check (async/<! (find-element "div.result" #"\"210\"")) => exist?)))

    (done)))

(defn run-tests []
  (run-all-tests))

(defn activate []
  (prn :RUNNING-TESTS)
  #_
  (run-all-tests)
  #_
  (async/go
    (let [e (async/<! (clj-editor))]
      (async/<! (with-text e 'test "(def foo 10)"))
      (async/<! (evaluate-command "core:select-all")))))

(defn after []
  (prn :RELOADED))
