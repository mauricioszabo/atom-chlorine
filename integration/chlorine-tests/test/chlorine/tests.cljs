(ns chlorine.tests
  (:require [chlorine.aux :refer-macros [in-channel async async-testing]]
            [clojure.test :refer [is deftest run-all-tests run-tests] :as t]
            [check.core :refer-macros [check]]
            [promesa.core :as p]
            ["remote" :as remote]))

(defn- await-for [fun]
  (js/Promise.
   (fn [resolve]
     ((fn acc [n resolve]
        (if (>= n 10)
          (resolve nil)
          (if-let [res (fun)]
            (resolve res)
            (js/setTimeout #(acc (inc n) resolve) 100))))
      0 resolve))))

(defn- evaluate-command [command]
  (.. js/atom -commands (dispatch (. js/document -activeElement) command)))

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
(defn- find-inside-console [match]
  (find-element "div.chlorine.console div" match))

(def exist? #(not (nil? %)))

(defn- dispatch-command [command]
  (.. js/atom -commands (dispatch (. js/document -activeElement) command)))

(defn- connect! []
  (p/alet [_ (evaluate-command "chlorine:connect-clojure-socket-repl")
           elem (await-for #(. js/document querySelector "input[placeholder=port]"))]
     (aset elem "value" 2233)
     (.onkeydown elem #js {:key "Enter"})))

(defn clj-editor []
  (.. js/atom -workspace (open "/tmp/test.clj" #js {:searchAllPanes true})))

(defn- with-text [^js editor namespace text]
  (p/alet [_ (.. editor (setText (str "(ns " namespace ")\n\n" text)))]
    (.setSelectedBufferRange editor (clj->js [[2 2] [2 2]]))))

(defn- with-clj [text]
  (p/alet [editor (clj-editor)
           _ (evaluate-command "inline-results:clear-all")]
    (with-text editor 'user.test1 text)))

(defn- eval-and-check [text command check-fn message]
  (p/alet [_ (with-clj text)
           _ (evaluate-command command)
           contents (check-fn message)]
    (check (some-> contents .-innerText) => message)))

(deftest connection-and-evaluation
  (async
    (async-testing "disconnecting editor"
      (p/alet [_ (evaluate-command "chlorine:disconnect")
               _ (p/delay 1000)]))

    (async-testing "connecting to editor"
      (p/alet [editor (clj-editor)
               _ (connect!)
               message (find-element "div.message" #"REPL connected")]
        (check message => exist?)))

    (async-testing "evaluates code"
      (eval-and-check "(str (+ 90 120))" "chlorine:evaluate-top-block"
                      find-inside-editor #"\"210\""))

    (async-testing "go to definition of a var"
      (p/alet [_ (eval-and-check "defn" "chlorine:go-to-var-definition"
                                 find-inside-editor #":arglists")]
        (evaluate-command "core:close")))

    (async-testing "shows definition of var"
      (eval-and-check "defn" "chlorine:source-for-var"
                      find-inside-console #"fdecl"))

    (async-testing "breaks evaluation"
      (p/alet [_ (with-clj "(Thread/sleep 2000)")
               _ (evaluate-command "chlorine:evaluate-top-block")
               _ (p/delay 400)
               _ (evaluate-command "chlorine:break-evaluation")
               contents (find-inside-editor #"Evaluation interrupted")]
        (check contents => exist?)))

    (async-testing "shows function doc"
      (eval-and-check "str" "chlorine:doc-for-var"
                      find-inside-editor
                      #"With no args, returns the empty string. With one arg x, returns"))))

    ; it('captures exceptions', async () => {})
    ;   await evalCommand(`(throw (ex-info "Error Number 1", {}))`)
    ;   assert.ok(await haveSelector(`div.error`))
    ;   assert.ok(await haveSelector(`span*=Error Number 1`))
    ;
    ;   await evalCommand(`(ex-info "Error Number 2", {})`)
    ;   assert.ok(await haveText(`Error Number 2`))
    ;
    ;
    ; it('allows big strings to be represented', async () => {})
    ;   await sendCommand('inline-results:clear-all')
    ;   await evalCommand("(str (range 200))")
    ;   assert.ok(await haveText("29"))
    ;   assert.ok(await haveText("..."))
    ;   // await app.client.click("a*=...")
    ;   // assert.ok(await haveText("52 53 54"))
    ;   await sendCommand('inline-results:clear-all')))
    ;


(defn run-my-tests []
  (run-tests))

(defn activate []
  (prn :RUNNING-TESTS))
  ; #_
  ; (run-all-tests))

(defn after []
  (prn :RELOADED))
