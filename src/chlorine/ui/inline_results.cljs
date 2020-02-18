(ns chlorine.ui.inline-results
  (:require [reagent.core :as r]
            [chlorine.ui.atom :as atom]
            [repl-tooling.editor-integration.renderer :as render]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defonce results (atom {}))

(defn get-result [editor row]
  ; TODO: Remove this, use only div maybe?
  (get-in @results [(.-id editor) row :result]))

(defn all-parsed-results []
  (for [[editor-id v] @results
        [row {:keys [parsed-ratom]}] v
        :when parsed-ratom]
    parsed-ratom))

(defn- discard-old-results! []
  (doseq [[editor-id v] @results
          [row {:keys [result div]}] v
          ; TODO: Feature toggle
          :when (or (and (not div) (not (some-> result .-view .-view .-isConnected)))
                    (and div (.isDestroyed result)))]
    (swap! results update editor-id dissoc row)))

; TODO: Remove Ink
(defn ^js new-result [^js editor row]
  (discard-old-results!)
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (let [result (InkResult. editor #js [row row] #js {:type "block"})]
      (swap! results assoc-in [(.-id editor) row :result] result)
      result)))

(defn ^js new-inline-result [^js editor [[r1 c1] [r2 c2]]]
  (discard-old-results!)
  (let [marker (. editor markBufferRange
                 (clj->js [[(inc r1) (inc c1)] [(inc r2) (inc c2)]])
                 #js {:invalidate "inside"})
        div (doto (. js/document createElement "div")
                  (aset "classList" "chlorine result-overlay")
                  (aset "innerHTML" "..."))]
    (when-let [result (get-result editor r2)]
      (and (.-isDestroyed result) (.destroy result)))

    (swap! results assoc-in [(.-id editor) r2] {:result marker :div div})
    (. editor decorateMarker marker #js {:type "block" :position "tail" :item div})))

(defn- create-div! [parsed-ratom]
  (let [div (. js/document createElement "div")]
    (aset div "classList" "chlorine result-overlay")
    (when (-> parsed-ratom meta :error) (.. div -classList (add "error")))
    (.. div -classList (add "result" "chlorine"))
    (r/render [:div [render/view-for-result parsed-ratom]] div)
    div))

(defn- get-or-create-div! [editor row parsed-ratom]
  (let [div (or (get-in @results [(.-id editor) row :div])
                (. js/document createElement "div"))]
    (when (-> parsed-ratom meta :error) (.. div -classList (add "error")))
    (.. div -classList (add "result"))
    (r/render [:div [render/view-for-result parsed-ratom]] div)
    div))

(defn update-with-result [editor row parsed-ratom]
  (when-let [inline-result (get-result editor row)]
    (swap! results assoc-in [(.-id editor) row :parsed-ratom] parsed-ratom)
    (let [div (get-or-create-div! editor row parsed-ratom)]
      (when (.-setContent inline-result)
        (.setContent inline-result div #js {:error (-> parsed-ratom meta :error)})))))

(defn inline-result [^js editor row parsed-ratom]
  (update-with-result editor row parsed-ratom))

(defn parse-and-inline [editor row parsed-result]
  (let [parse (:parse @state)]
    (inline-result editor row (parse parsed-result))))
