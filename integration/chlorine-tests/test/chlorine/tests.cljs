(ns chlorine.tests
  (:require [chlorine.utils :refer-macros [async async-testing]]
            [clojure.test :refer [deftest run-tests] :as test]
            [check.core :refer-macros [check]]
            [promesa.core :as p]))

(defn- await-for [fun]
  (js/Promise.
   (fn [resolve]
     ((fn acc [n resolve]
        (if (>= n 20)
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

(defn containing-text [selector match]
  (->> selector
       query-all
       (filter #(and (some-> % .-isConnected)
                     (re-find match (.-innerText %))))
       first))

(defn- find-element [selector text]
  (await-for #(containing-text selector text)))

(defn- find-element-inside-editor [element match]
  (find-element (str "atom-text-editor:not(*[style*='display: none']) " element)
                match))

(defn- find-inside-editor [match]
  (find-element-inside-editor "div" match))

(defn- find-inside-console [match]
  (find-element "div.repl-tooling.console div" match))

(defn exist? [some-string]
  #(not (nil? %)))

(defn- connect! []
  (p/alet [_ (evaluate-command "chlorine:connect-socket-repl")
           elem (await-for #(. js/document querySelector "input[placeholder=port]"))]
     (aset elem "value" 2233)
     (.onkeydown elem #js {:key "Enter"})))

(defn clj-editor []
  (.. js/atom -workspace (open "/tmp/test.clj" #js {:searchAllPanes true})))
(defn cljs-editor []
  (.. js/atom -workspace (open "/tmp/test.cljs" #js {:searchAllPanes true})))

(defn- with-text [^js editor namespace text]
  (p/alet [_ (.. editor (setText (str "(ns " namespace ")\n\n" text)))]
    (.setSelectedBufferRange editor (clj->js [[2 2] [2 2]]))))

(defn- with-clj [text]
  (p/alet [editor (clj-editor)
           _ (evaluate-command "inline-results:clear-all")]
    (with-text editor 'user.test1 text)))

(defn- with-cljs [text]
  (p/alet [editor (cljs-editor)
           _ (evaluate-command "inline-results:clear-all")]
    (with-text editor 'user.test2 text)))

(defn- eval-and-check [text command check-fn message]
  (p/alet [_ (with-clj text)
           _ (evaluate-command command)
           contents (check-fn message)]
    (check (some-> contents .-innerText) => message)))

(defn- cljs-eval-and-check [text command check-fn message]
  (p/alet [_ (with-cljs text)
           _ (evaluate-command command)
           contents (check-fn message)]
    (check (some-> contents .-innerText) => message)))

(deftest clojure-connection-and-evaluation
  (async
    (async-testing "disconnecting editor"
      (p/alet [_ (evaluate-command "chlorine:disconnect")
               _ (evaluate-command "tree-view:show")
               _ (evaluate-command "tree-view:toggle")
               _ (p/delay 1000)]))

    (async-testing "connecting to editor"
      (p/alet [editor (clj-editor)
               _ (p/delay 1000)
               _ (connect!)
               _ (p/delay 1000)
               message (find-element "div.message" #"REPL Connected")]
        (check message => (exist? "REPL connected"))
        (p/delay 5000)))

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
      (p/do!
       (with-clj "(Thread/sleep 4000)")
       (evaluate-command "chlorine:evaluate-top-block"))
      (p/alet [_ (p/delay 400)
               _ (evaluate-command "chlorine:break-evaluation")
               contents (find-inside-editor #"Evaluation interrupted")]
        (check contents => (exist? "Evaluation interrupted"))))

    (async-testing "shows function doc"
      (eval-and-check "str" "chlorine:doc-for-var"
                      find-inside-editor
                      #"With no args, returns the empty string. With one arg x, returns"))

    (async-testing "captures exceptions"
      (eval-and-check "(throw (ex-info \"Error Number 1\", {}))"
                      "chlorine:evaluate-top-block"
                      (partial find-element "div.error")
                      #"Error Number 1"))

    (async-testing "captures evaluated exceptions"
      (eval-and-check "(ex-info \"Error Number 2\", {})"
                      "chlorine:evaluate-top-block"
                      find-inside-editor
                      #"Error Number 2"))

    (async-testing "allows big strings to be represented"
      (eval-and-check "(str (range 200))"
                      "chlorine:evaluate-top-block"
                      find-inside-editor
                      #"(?m)29[\s\n]*\.\.\.")
      (p/alet [_ (p/delay 1000)
               link (find-element "div.string a" #"\.\.\.")
               _ (.click link)
               element (find-inside-editor #"52 53 54")]
        (check element => (exist? "52 53 54"))))))

(deftest cljs-connection-and-evaluation
  (async
    (async-testing "connecting to clojurescript"
      (p/alet [editor (cljs-editor)
               _ (evaluate-command "chlorine:connect-embedded")
               message (find-element "div.message" #"Connected to ClojureScript")]
        (check message => (exist? "Connected to ClojureScript"))))

    (async-testing "evaluates code"
      (cljs-eval-and-check "(str (+ 90 120))" "chlorine:evaluate-top-block"
                           find-inside-editor #"\"210\""))

    (async-testing "evaluates #Inf"
      (cljs-eval-and-check "(/ 10 0)" "chlorine:evaluate-top-block"
                           find-inside-editor #"##Inf"))

    #_ ;NOT YET
    (async-testing "go to definition of a var"
      (p/alet [_ (cljs-eval-and-check "defn" "chlorine:go-to-var-definition"
                                      find-inside-editor #":arglists")]
        (evaluate-command "core:close")))

    #_ ;NOT YET
    (async-testing "shows definition of var"
      (cljs-eval-and-check "defn" "chlorine:source-for-var"
                           find-inside-console #"fdecl"))

    (async-testing "shows function doc"
      (cljs-eval-and-check "str" "chlorine:doc-for-var"
                           find-inside-editor
                           #"With no args, returns the empty string. With one arg x, returns"))

    (async-testing "captures exceptions"
      (cljs-eval-and-check "(throw (ex-info \"Error Number 1\", {}))"
                           "chlorine:evaluate-top-block"
                           (partial find-element "div.error")
                           #"Error Number 1"))

    (async-testing "captures evaluated exceptions"
      (cljs-eval-and-check "(ex-info \"Error Number 2\", {})"
                           "chlorine:evaluate-top-block"
                           find-inside-editor
                           #"Error Number 2"))

    (async-testing "captures pure objects as exceptions"
      (cljs-eval-and-check "(throw \"Error Number 3\")"
                           "chlorine:evaluate-top-block"
                           find-inside-editor
                           #"Error Number 3"))))

(defmethod test/report [::test/default :summary] [{:keys [test pass fail error]}]
  (println "--- TESTS ENDED ---")
  (println "\nRan" test "tests containing"
           (+ pass fail error) "assertions.")
  (println fail "failures," error "errors.")
  (when (-> js/process .-env .-ELECTRON_ENABLE_LOGGING (= "true"))
    (let [app (-> "remote" js/require .-app)]
      (if (= 0 fail error)
        (.exit app 0)
        (.exit app 1)))))

(defn run-my-tests []
  (run-tests))

(defn activate []
  (prn :RUNNING-TESTS)
  (when (-> js/process .-env .-ELECTRON_ENABLE_LOGGING (= "true"))
    (run-my-tests)))

(defn after []
  (prn :RELOADED))
