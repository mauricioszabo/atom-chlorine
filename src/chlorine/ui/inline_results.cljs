(ns chlorine.ui.inline-results
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [reagent.core :as r]
            [clojure.walk :as walk]
            [repl-tooling.eval :as eval]
            [repl-tooling.editor-helpers :as editor-helpers]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defn ^js new-result [^js editor row]
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (InkResult. editor #js [row row] #js {:type "block"})))

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

(declare to-tree)
(defn- as-map [[key val]]
  (let [k-str (str "[" (to-str key))
        v-str (str (to-str val) "]")]
    [:row [[k-str [(to-tree key)]]
           [v-str [(to-tree val)]]]]))

(defn- to-tree [edn]
  (let [txt (to-str edn)
        edn (cond-> edn (instance? editor-helpers/WithTag edn) editor-helpers/obj)]
    (cond
      (map? edn) [txt (mapv as-map edn)]
      (coll? edn) [txt (mapv to-tree edn)]
      :else [txt])))

(defn- as-obj [unrepl-obj]
  (let [tag? (and unrepl-obj (vector? unrepl-obj) (->> unrepl-obj first (instance? editor-helpers/WithTag)))]
    (if (and tag? (-> unrepl-obj first editor-helpers/tag (= "#class ")))
      (let [[f s] unrepl-obj]
        (symbol (str (editor-helpers/obj f) "@" s)))
      unrepl-obj)))

(defn- leaf [text]
  (doto (.createElement js/document "div")
    (aset "innerText" text)))

(declare to-html)
(defn- html-row [children]
  (let [div (.createElement js/document "div")]
    (doseq [child children]
      (.appendChild div (to-html child)))
    div))

(defn set-content! [^js result result-tree]
  (let [contents (to-html result-tree)]
    (.setContent result contents #js {:error false})))

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

(defn expand [parent]
  (if (contains? @parent :children)
    (swap! parent dissoc :children)
    (let [contents (:contents @parent)
          tag? (instance? editor-helpers/WithTag contents)
          tag-or-map? (or (map? contents) (and tag? (-> contents editor-helpers/obj map?)))
          to-get (cond-> contents (instance? editor-helpers/WithTag contents) editor-helpers/obj)
          children (mapv (fn [c] {:contents c}) (put-more-to-end to-get))]
      (swap! parent assoc :children children))))

(defn- as-tree-element [edn result]
  (let [tag (when (instance? editor-helpers/WithTag edn) (editor-helpers/tag edn))
        edn (cond-> edn (instance? editor-helpers/WithTag edn) editor-helpers/obj)]
    (-> edn
        pr-str
        (str/replace #"\{:repl-tooling/\.\.\. .+?\}" "...")
        (->> (str tag)))))

(defn- coll-views [result]
  [:span
   [:a {:on-click #(expand result)}
    [:span {:class ["icon" (if (contains? @result :children)
                               "icon-chevron-down"
                               "icon-chevron-right")]}]]])

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

(defn- result-row [is-more? parent result]
  (if is-more?
    [:a {:on-click #(get-more (r/cursor parent [:children])
                              (-> @result :contents :repl-tooling/...)
                              true)}
     (to-str (:contents @result))]
    [string-row result]))

(defn- result-view [parent key]
  (let [result (r/cursor parent key)
        r @result
        contents (:contents r)
        is-more? (and (map? contents) (-> contents keys (= [:repl-tooling/...])))
        keys-of #(if (-> r :children map?)
                   (-> r :children keys)
                   (-> r :children keys count range))]

    [:div {:key (str key)}
     [:div {:style {:display "flex"}}
      (when (and (not is-more?)
                 (or (instance? editor-helpers/WithTag contents) (coll? contents)))
        [coll-views result])

      [result-row is-more? parent result]]

     (when (contains? r :children)
       [:div {:class "children"}
        (doall (map #(result-view result [:children %]) (keys-of)))])]))

(defn view-for-result
  ([eval-result] (view-for-result eval-result true))
  ([eval-result parse?]
   (let [div (. js/document (createElement "div"))
         res (r/atom [{:contents (cond-> eval-result parse? editor-helpers/read-result)}])]
     (r/render [:div
                [result-view res [0]]] div)
     [div res])))

(defn render-result! [^js result eval-result]
  (let [[div res] (view-for-result eval-result)]
    (.. div -classList (add "result" "chlorine"))
    (.setContent result div #js {:error false})))

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
        res (r/atom (editor-helpers/read-result error))]
    (r/render [error-view res] div)
    (.. div -classList (add "error" "chlorine"))
    (.setContent result div #js {:error true})))
