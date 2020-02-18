(ns chlorine.ui.inline-results
  (:require [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defonce results (atom {}))

(defn get-result [editor row]
  (get-in @results [(.-id editor) row :result]))

(defn all-parsed-results []
  (for [[editor-id v] @results
        [row {:keys [parsed-ratom]}] v
        :when parsed-ratom]
    parsed-ratom))

(defn ^js new-result [^js editor row]
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (let [result (InkResult. editor #js [row row] #js {:type "block"})]
      (doseq [[editor-id v] @results
              [row {:keys [result]}] v
              :when (not (some-> result .-view .-view .-isConnected))]
        (swap! results update editor-id dissoc row))
      (swap! results assoc-in [(.-id editor) row :result] result)
      result)))

(defn- create-div! [parsed-ratom]
  (let [div (. js/document createElement "div")]
    (when (-> parsed-ratom meta :error) (.. div -classList (add "error")))
    (.. div -classList (add "result" "chlorine"))
    (r/render [:div [render/view-for-result parsed-ratom]] div)
    div))

(defn update-with-result [editor row parsed-ratom]
  (when-let [inline-result (get-result editor row)]
    (swap! results assoc-in [(.-id editor) row :parsed-ratom] parsed-ratom)
    (let [div (create-div! parsed-ratom)]
      (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)}))))

(defn inline-result [^js editor row parsed-ratom]
  (update-with-result editor row parsed-ratom))

(defn parse-and-inline [editor row parsed-result]
  (let [parse (:parse @state)]
    (inline-result editor row (parse parsed-result))))
