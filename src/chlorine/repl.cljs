(ns chlorine.repl
  (:require [repl-tooling.eval :as eval]
            [chlorine.state :refer [state]]
            [repl-tooling.editor-helpers :as helpers]
            [chlorine.ui.inline-results :as inline]
            [chlorine.ui.console :as console]
            [repl-tooling.editor-integration.connection :as connection]
            [chlorine.ui.atom :as atom]
            [repl-tooling.editor-integration.evaluation :as e-eval]
            ["atom" :refer [CompositeDisposable]]
            [repl-tooling.editor-integration.schemas :as schemas]
            [schema.core :as s]))

(defonce ^:private commands-subs (atom (CompositeDisposable.)))

(defn- handle-disconnect! []
  (let [repls (:repls @state)]
    (if (or (:clj-eval repls) (:cljs-eval repls))
      (atom/info "Disconnected from REPLs" "")))

  (swap! state assoc
         :repls {:clj-eval nil
                 :cljs-eval nil
                 :clj-aux nil}
         :connection nil)
  (.dispose ^js @commands-subs)
  (reset! commands-subs (CompositeDisposable.)))

(defn- decide-command [cmd-name command]
  (let [old-cmd (:old-command command)
        new-cmd (:command command)]
    (fn []
      (if (and old-cmd (-> @state :config :experimental-features (not= true)))
        (old-cmd)
        (new-cmd)))))

(defn- register-commands! [commands]
  (doseq [[k command] commands
          :let [disp (-> js/atom
                         .-commands
                         (.add "atom-text-editor"
                               (str "chlorine:" (name k))
                               (decide-command k command)))]]
    (.add ^js @commands-subs disp)))

(s/defn ^:private get-editor-data :- schemas/EditorData []
  (when-let [editor (atom/current-editor)]
    (let [range (.getSelectedBufferRange editor)
          start (.-start range)
          end (.-end range)]
      {:editor editor
       :contents (.getText editor)
       :filename (.getPath editor)
       :range [[(.-row start) (.-column start)]
               [(.-row end) (cond-> (.-column end)
                                    (not= (.-column start) (.-column end)) dec)]]})))

(defn- notify! [{:keys [type title message]}]
  (case type
    :info (atom/info title message)
    :warn (atom/warn title message)
    (atom/error title message)))

(defn- prompt! [{:keys [title message arguments]}]
  (js/Promise.
   (fn [resolve]
     (let [notification (atom nil)
           buttons (->> arguments (map (fn [{:keys [key value]}]
                                         {:text value
                                          :onDidClick (fn []
                                                        (resolve key)
                                                        (.dismiss ^js @notification))})))]

       (reset! notification (.. js/atom -notifications
                                (addInfo title (clj->js {:detail message
                                                         :dismissable true
                                                         :buttons buttons}))))
       (.onDidDismiss ^js @notification #(fn [] (resolve nil) true))))))

(defn- create-inline-result! [{:keys [range editor-data]}]
  (when-let [editor (:editor editor-data)]
    (if (-> @state :config :experimental-features (not= true))
      (inline/new-result editor (-> range last first))
      (inline/new-inline-result editor range))))

(defn- update-inline-result! [{:keys [range editor-data] :as result}]
  (let [editor (:editor editor-data)
        parse (-> @state :tooling-state deref :editor/features :result-for-renderer)]
    (when editor
      (inline/inline-result editor (-> range last first) (parse result)))))

(defn- on-copy! [txt]
  (.. js/atom -clipboard (write txt))
  (atom/info "Copied result" ""))

(s/defn get-config :- schemas/Config []
  (assoc (:config @state)
         :project-paths (->> js/atom
                             .-project
                             .getDirectories
                             (map #(.getPath ^js %)))))

(defn connect-socket! [host port]
  (let [p (connection/connect!
           host port
           {:on-stdout console/stdout
            :on-stderr console/stderr
            ; :on-result console/result
            :on-disconnect handle-disconnect!
            :on-start-eval create-inline-result!
            :on-eval (fn [res]
                       (console/result res)
                       (update-inline-result! res))
            :get-rendered-results #(concat (inline/all-parsed-results)
                                           (->> @console/out-state
                                                (filter (fn [r] (-> r first (= :result))))
                                                (map second)))
            :on-copy on-copy!
            :editor-data get-editor-data
            :get-config get-config
            :notify notify!
            :prompt prompt!})]

    (.then p (fn [st]
               (when st
                 (console/open-console (-> @state :config :console-pos)
                                       #(connection/disconnect!))
                 (swap! state #(-> %
                                   (assoc-in [:repls :clj-eval] (:clj/repl @st))
                                   (assoc-in [:repls :clj-aux] (:clj/aux @st))
                                   (assoc :parse (-> @st :editor/features :result-for-renderer))
                                   (assoc :connection {:host host :port port}
                                          ; FIXME: This is just here so we can migrate
                                          ; code to REPL-Tooling little by little
                                          :tooling-state st)))
                 (-> @st :editor/commands register-commands!))))))

(defn need-cljs? [editor]
  (e-eval/need-cljs? (:config @state) (.getFileName editor)))

(defn- eval-cljs [editor ns-name filename row col code ^js result opts callback]
  (if-let [repl (some-> @state :tooling-state deref :cljs/repl)]
    (eval/evaluate repl code
                   {:namespace ns-name :row row :col col :filename filename
                    :pass opts}
                   callback)
    (do
      (some-> result .destroy)
      (atom/error "REPL not connected"
                  (str "REPL not connected for ClojureScript.\n\n"
                       "You can connect a repl using "
                       "'Connect ClojureScript Socket REPL' command,"
                       "or 'Connect a self-hosted ClojureScript' command")))))

(defn evaluate-aux
  ([^js editor ns-name filename row col code callback]
   (evaluate-aux editor ns-name filename row col code {} callback))
  ([^js editor ns-name filename row col code opts callback]
   (if (need-cljs? editor)
     (eval-cljs editor ns-name filename row col code nil opts #(-> % helpers/parse-result callback))
     (some-> @state :tooling-state deref :clj/aux
             (eval/evaluate code
                            {:namespace ns-name :row row :col col :filename filename
                             :pass opts}
                            #(-> % helpers/parse-result callback))))))

(defn- txt-in-range []
  (let [{:keys [contents range]} (get-editor-data)]
    [range (helpers/text-in-range contents range)]))

(defn get-code [kind]
  (when-let [editor (atom/current-editor)]
    (let [range (.getSelectedBufferRange editor)
          start (.-start range)
          row (.-row start)
          col (.-column start)
          contents (.getText editor)
          [range text] (case kind
                         "top-block" (helpers/top-block-for contents [row col])
                         "block" (helpers/block-for contents [row col])
                         "var" (helpers/current-var contents [row col])
                         "selection" (txt-in-range)
                         "ns" (helpers/ns-range-for contents [row col]))]
      (clj->js {:text text
                :range range}))))

(defn evaluate-and-present [code range]
  (when-let [command (some-> @state :tooling-state deref
                             :editor/features :eval-and-render)]
    (command code (js->clj range))))

(defn wrap-in-rebl-submit
  "Clojure 1.10 only, require REBL on the classpath (and UI open)."
  [code]
  (str "(let [value " code "]"
       " (try"
       "  ((requiring-resolve 'cognitect.rebl/submit) '" code " value)"
       "  (catch Throwable _))"
       " value)"))

(defn inspect-top-block! []
  (let [res (get-code "top-block")]
    (some-> (.-text res)
            (wrap-in-rebl-submit)
            (evaluate-and-present (.-range res)))))

(defn inspect-block! []
  (let [res (get-code "block")]
    (some-> (.-text res)
            (wrap-in-rebl-submit)
            (evaluate-and-present (.-range res)))))

(defn clear-inline! []
  (inline/clear-results! (atom/current-editor)))

(def exports
  #js {:get_top_block #(get-code "top-block")
       :get_block #(get-code "block")
       :get_var #(get-code "var")
       :get_selection #(get-code "selection")
       :get_namespace #(get-code "ns")
       :evaluate_and_present evaluate-and-present})
