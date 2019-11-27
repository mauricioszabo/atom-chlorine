(ns chlorine.ui.inline-results
  (:require [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defonce results (atom {}))

(defn ^js new-result [^js editor row]
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (let [result (InkResult. editor #js [row row] #js {:type "block"})]
      (swap! results assoc [(.-id editor) row] result)
      result)))

(defn- create-div! [parsed-ratom]
  (let [div (. js/document createElement "div")]
    (when (-> parsed-ratom meta :error) (.. div -classList (add "error")))
    (.. div -classList (add "result" "chlorine"))
    (r/render [render/view-for-result parsed-ratom] div)
    div))

(defn- parse-result [result]
  (let [parse (:parse @state)]
    (parse result)))

(defn render-inline! [^js inline-result parsed-result]
  (let [parsed-ratom (parse-result parsed-result)
        div (create-div! parsed-ratom)]
    (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)})))

(defn render-error! [^js inline-result parsed-result]
  (let [parsed-ratom (parse-result parsed-result)
        div (create-div! parsed-ratom)]
    (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)})))

(defn inline-result [^js editor row parsed-ratom]
  (let [div (create-div! parsed-ratom)
        inline-result ^js (get @results [(.-id editor) row])]
    (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)})))
