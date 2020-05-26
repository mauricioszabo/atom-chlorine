(ns chlorine.tests
  (:require [clojure.test :refer [deftest testing run-tests] :as test]
            [etaoin.api :as api]
            [etaoin.keys :as keys]
            [check.core :refer [check]]
            [clojure.string :as str]))

(def editor (atom nil))

(defn- prepare-selenium! []
  (reset! editor
          (api/boot-driver :chrome {:path-browser "./scripts/run-atom"
                                    :args [
                                           "--no-sandbox"
                                           "--disable-setuid-sandbox"
                                           "--foreground"]})))

(defn- run-worspace-cmd! [command]
  (api/js-execute @editor (str "atom.commands.dispatch("
                               "atom.views.getView(atom.workspace), "
                               (pr-str command)
                               ")")))

(defn- run-editor-cmd! [command]
  (api/js-execute @editor (str "atom.commands.dispatch("
                               "atom.views.getView(atom.workspace.getActiveTextEditor()), "
                               (pr-str command)
                               ")")))

(defn- title-of-editor []
  (and
   (api/js-execute @editor "if(atom.workspace.getActiveTextEditor()) {return true}")
   (api/js-execute @editor "return atom.workspace.getActiveTextEditor().getTitle()")))

(defn- close-other-tabs! []
  (doseq [n (range 10)]
    (if (#{"test.clj" "test.cljs" "data:,"} (title-of-editor))
      (do
        (run-worspace-cmd! "window:focus-next-pane")
        (run-worspace-cmd! "pane:show-next-item"))
      (run-worspace-cmd! "core:close"))))

(defn- connect! []
  (run-worspace-cmd! "chlorine:connect-socket-repl")
  (api/wait-visible @editor {:css "input[placeholder=port]"})
  (api/fill @editor {:css "input[placeholder=port]"} (keys/with-ctrl keys/backspace))
  (api/fill @editor {:css "input[placeholder=port]"} "3333")
  (api/fill @editor {:css "input[placeholder=port]"} keys/enter))

(defn- wait-for [f]
  (doseq [n (range 20)
          :while (not (f))]
    (Thread/sleep 100))
  (if-let [r (f)]
    r
    (throw (ex-info "Waiting failed" {:f f}))))

(defn has-text? [text]
  (wait-for #(api/has-text? @editor text))
  text)

(defn- goto-file [file]
  (doseq [n (range 5)
          :while (not= file (title-of-editor))]
    (run-worspace-cmd! "window:focus-next-pane")
    (run-worspace-cmd! "pane:show-next-item"))
  (when-not (= file (title-of-editor))
    (throw (ex-info "Can't file file!" {:last-file (title-of-editor)
                                        :expected-file file}))))

(defn- with-file [file contents]
  (goto-file file)
  (doseq [n (range 100)] (api/fill-active @editor (keys/with-ctrl keys/backspace)))
  (api/fill-active @editor contents))

(defn- eval-file [file contents]
  (with-file file contents)
  (run-editor-cmd! "chlorine:clear-console")
  (run-editor-cmd! "chlorine:evaluate-top-block"))

(defn- inline-text []
  (api/wait-exists @editor {:css "atom-text-editor div.result-overlay"})
  (api/wait-absent @editor {:css "atom-text-editor div.result-overlay span.loading"})
  (api/js-execute @editor (str "return "
                               "document.querySelector"
                               "('atom-text-editor div.result-overlay')"
                               ".innerText")))

(defn- editor-text []
  (api/js-execute @editor "return atom.workspace.getActiveTextEditor().getText()"))

(defn- console-text []
  (api/js-execute @editor (str "return "
                               "document.querySelector"
                               "('div.console')"
                               ".innerText")))

(defn- visible? [css]
  (api/js-execute @editor "return document.querySelector('div.result-overlay.error')"))

(defn- click-elision []
  (let [[_ elem] (api/query-all @editor "//a[contains(.,'...')]")]
    (api/click-el @editor elem)))

(deftest chlorine-tests []
  (try
    (prepare-selenium!)
    (api/wait-exists @editor {:css "atom-text-editor"})
    (close-other-tabs!)

    (testing "Clojure"
      (testing "can connect into editor"
        (connect!)
        (check (has-text? "REPL Connected") => #"Connected"))

      (testing "evaluates code"
        (eval-file "test.clj" "(str (+ 90 120))")
        (check (inline-text) => "\"210\""))

      (testing "go to definition of a var"
        (with-file "test.clj" "defn")
        (run-editor-cmd! "chlorine:go-to-var-definition")
        (goto-file "core.clj")
        (check (editor-text) => #":arglists")
        (run-editor-cmd! "core:close"))

      (testing "shows definition of var"
        (with-file "test.clj" "defn")
        (run-editor-cmd! "chlorine:source-for-var")
        (wait-for #(re-find #"fdecl" (console-text)))
        (check (console-text) => #"fdecl"))

      (testing "breaks evaluation"
        (eval-file "test.clj" "(Thread/sleep 4000)")
        (wait-for #(do
                     (run-editor-cmd! "chlorine:break-evaluation")
                     (re-find #"interrupted" (console-text))))
        (check (inline-text) => #"Evaluation interrupted"))

      (testing "shows function doc"
        (eval-file "test.clj" "str")
        (run-editor-cmd! "chlorine:doc-for-var")
        (check (inline-text) => #"With no args, returns the empty string. With one arg x, returns"))

      (testing "captures exceptions"
        (eval-file "test.clj" "(throw (ex-info \"Error Number 1\", {}))")
        (run-editor-cmd! "chlorine:evaluate-top-block")
        (check (inline-text) => #"Error Number 1")
        (check (visible? "div.result-overlay.error") => {}))

      (testing "captures evaluated exceptions"
        (eval-file "test.clj" "(ex-info \"Error Number 2\", {})")
        (run-editor-cmd! "chlorine:evaluate-top-block")
        (check (inline-text) => #"Error Number 2")
        (check (visible? "div.result-overlay.error") => nil))

      (testing "allows big strings to be represented"
        (eval-file "test.clj" "(str (range 200))")
        (check (inline-text) => #"(?m)29[\s\n]*\.\.\."))

      (testing "expands big strings"
        (click-elision)
        (wait-for #(re-find #"52" (console-text)))
        (check (console-text) => #"52 53 54")
        (goto-file "test.clj")
        (run-editor-cmd! "chlorine:clear-inline-results"))

    ;;; CLOJURESCRIPT!
      (testing "ClojureScript"
        (testing "connecting"
          (run-editor-cmd! "chlorine:connect-embedded")
          (check (has-text? "Connected to ClojureScript") => #"ClojureScript"))

        (testing "evaluates code"
          (eval-file "test.cljs" "(str (+ 90 120))")
          (check (inline-text) => "\"210\""))

        (testing "go to definition of a var"
          (with-file "test.cljs" "println")
          (run-editor-cmd! "chlorine:go-to-var-definition")
          (goto-file "core.cljs")
          (check (editor-text) => #"Same as print")
          (run-editor-cmd! "core:close"))

        (testing "captures exceptions"
          (eval-file "test.cljs" "(throw (ex-info \"Error Number 1\", {}))")
          (run-editor-cmd! "chlorine:evaluate-top-block")
          (check (inline-text) => #"Error Number 1")
          (check (visible? "div.result-overlay.error") => {}))

        (testing "captures non-error exceptions"
          (eval-file "test.cljs" "(throw \"ERROR\")")
          (run-editor-cmd! "chlorine:evaluate-top-block")
          (check (inline-text) => #"ERROR")
          (check (visible? "div.result-overlay.error") => {}))

        (testing "captures evaluated exceptions"
          (eval-file "test.cljs" "(ex-info \"Error Number 2\", {})")
          (run-editor-cmd! "chlorine:evaluate-top-block")
          (check (inline-text) => #"Error Number 2")
          (check (visible? "div.result-overlay.error") => nil))))
    (finally
      (Thread/sleep 1000)
      ; Because, Chome.... :(
      (try (api/close-window @editor) (catch Throwable _))
      (api/quit @editor))))

(defn run-all-tests []
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (+ fail error))))

#_
(load-file "test/chlorine/tests.clj")
