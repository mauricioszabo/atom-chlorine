(ns chlorine.ui.inline-results
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [clojure.walk :as walk]
            [repl-tooling.eval :as eval]
            [repl-tooling.editor-helpers :as editor-helpers]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defn ^js new-result [^js editor row]
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (InkResult. editor #js [row row] #js {:type "block"})))

(defn- create-div! [parsed-ratom]
  (let [div (. js/document createElement "div")]
    (.. div -classList (add "result" "chlorine"))
    (r/render [render/view-for-result parsed-ratom] div)
    div))

(defn render-on-console! [^js console parsed-result]
  (let [parsed (render/parse-result parsed-result (-> @state :repls :clj-eval))
        div (create-div! parsed)]
    (some-> console (.result div))))

(defn render-inline! [^js inline-result parsed-result]
  (let [parsed-ratom (render/parse-result parsed-result (-> @state :repls :clj-eval))
        div (create-div! parsed-ratom)]
    (.setContent inline-result div #js {:error false})))

(defn- to-str [edn]
  (let [tag (when (instance? editor-helpers/WithTag edn) (editor-helpers/tag edn))
        edn (cond-> edn (instance? editor-helpers/WithTag edn) editor-helpers/obj)
        start (if-let [more (get edn {:repl-tooling/... nil})]
                (-> edn (dissoc {:repl-tooling/... nil})
                    pr-str (str/replace-first #"\}$" " ...}"))
                (pr-str edn))]

    (-> start
        (str/replace #"\{:repl-tooling/\.\.\. .+?\}" "...")
        (->> (str tag)))))

(defn put-more-to-end [contents]
  (if-let [get-more (get contents {:repl-tooling/... nil})]
    (-> contents (dissoc {:repl-tooling/... nil}) vec (conj get-more))
    contents))

(defn- get-more [path command wrap?]
  (let [with-res (fn [{:keys [result]}]
                   (let [res (cond->> (put-more-to-end (editor-helpers/read-result result))
                                      wrap? (map #(hash-map :contents %)))
                         tagged? (instance? editor-helpers/WithTag @path)
                         obj (cond-> @path tagged? editor-helpers/obj)
                         ; FIXME: this cond is unnecessary, but it will stay here because
                         ; I want to be sure
                         merged (cond
                                  (vector? obj) (-> obj butlast (concat res) vec)
                                  :else (throw (ex-info "NOT FOUND!!! FIX IT" {})))]
                     (if tagged?
                       (reset! path (editor-helpers/WithTag. merged (editor-helpers/tag @path)))
                       (reset! path merged))))]
    (some-> @state :repls :clj-eval (eval/evaluate command {:ignore true} with-res))))

(defn- parse-stack [path stack]
  (if (and (map? stack) (:repl-tooling/... stack))
    {:contents "..." :fn #(get-more path (:repl-tooling/... stack) false)}
    (if (string? stack)
      {:contents stack}
      (let [[class method file num] stack]
        (when-not (re-find #"unrepl\.repl\$" (str class))
          {:contents (str "in " (demunge class) " (" method ") at " file ":" num)})))))

(defn- stack-line [idx piece]
  [:div {:key idx}
   (if-let [fun (:fn piece)]
     [:a {:on-click fun} (:contents piece)]
     (:contents piece))])

(defn- string-row [result]
  (let [contents (:contents @result)
        with-res (fn [res]
                   (let [res (editor-helpers/parse-result res)]
                     (when-let [string (:result res)]
                       (swap! result update :contents editor-helpers/concat-with string))))
        more-str (fn [command]
                   (some-> @state :repls :clj-eval
                           (eval/evaluate command {:ignore true} with-res)))]

    (cond
      (instance? editor-helpers/IncompleteStr contents)
      [:span.multiline
       [:span (str/replace (pr-str contents) #"\.{3}\"$" "")]
       [:a {:on-click #(-> contents meta :get-more more-str)} "..."]
       [:span "\""]]

      (instance? editor-helpers/LiteralRender contents)
      [:span.multiline (to-str contents)]

      (string? contents)
      [:span.multiline (to-str contents)]

      :else
      [:span.single-line (to-str contents)])))

(defn- deconstruct-stack
  "Returns a [type message trace-atom] for exception"
  [error]
  (let [e @error
        ex (:ex e)]
    (if ex ; Probably Clojure exception
      (let [[cause] (:via ex)]
        [(:type cause) (or (:cause ex) (:message cause)) (r/cursor error [:ex :trace])])
      (let [t (:trace e)
            trace (cond-> t
                          (string? t)
                          (str/split "\n"))]
        [(or (:type e) "Error") (or (:message e) (:obj e)) (r/atom trace)]))))

(defn- error-view [error]
  (when (instance? editor-helpers/WithTag (:ex @error))
    (swap! error update :ex editor-helpers/obj))
  (let [ex (:ex @error)
        [ex-type ex-msg trace] (deconstruct-stack error)
        stacks (->> @trace
                    (map (partial parse-stack trace))
                    (filter identity))]
    [:div
     [:strong {:class "error-description"}
      [:div
       [:span "\033[0m"]
       [:span ex-type]
       [:span ": "]
       [string-row (r/atom {:contents ex-msg})]]]
     [:div {:class "stacktrace"}
      (map stack-line (range) stacks)]]))

(defn render-error! [^js result error]
  (let [div (. js/document (createElement "div"))
        res (r/atom error)]
    (r/render [error-view res] div)
    (.. div -classList (add "error" "chlorine"))
    (.setContent result div #js {:error true})))
