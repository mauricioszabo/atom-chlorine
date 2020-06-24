(ns chlorine.ui.inline-results
  (:require [reagent.dom :as rdom]
            [promesa.core :as p]
            [repl-tooling.editor-integration.schemas :as schemas]
            [schema.core :as s]
            [repl-tooling.editor-integration.renderer :as render]
            [chlorine.state :refer [state]]
            ["atom" :refer [TextEditor]]))

(defonce ^:private results (atom {}))

(defn- update-marker [id, ^js marker, ^js dec]
  (when-not (.isValid marker)
    (let [div (get-in @results [id :div])]
      (try (rdom/unmount-component-at-node div) (catch :default _)))
    (swap! results dissoc id)
    (.destroy dec)
    (.destroy marker)))

(defn- create-result [id editor range]
  (let [marker ^js (. editor markBufferRange
                     (clj->js range)
                     #js {:invalidate "inside"})
        div (. js/document createElement "div")
        dec (. ^js editor decorateMarker marker #js {:type "block" :position "after" :item div})]
    (.onDidChange marker (fn [_] (update-marker id marker dec)))
    (.onDidDestroy marker (fn [_] (update-marker id marker dec)))
    (swap! results assoc id {:marker marker :div div :editor editor})
    (aset marker "__divElement" div)
    div))

(defn- find-result [^js editor range]
  (-> editor
      (.findMarkers #js {:endBufferRow (-> range last first)})
      (->> (filter #(.-__divElement ^js %))
           first)))

(s/defn new-result [data :- schemas/EvalData]
  (when-let [editor (-> data :editor-data :editor)]
    (let [id (:id data)
          range (:range data)
          _ (when-let [old-marker (find-result editor range)]
              (.destroy old-marker))
          div (create-result id editor range)]
      (doto div
        (aset "classList" "chlorine result-overlay native-key-bindings")
        (aset "innerHTML" "<div><span class='repl-tooling icon loading'></span></div>")))))

(s/defn update-result [result :- schemas/EvalResult]
  (let [id (:id result)
        {:keys [editor range]} (:editor-data result)]
    (when-let [{:keys [div]} (get @results id)]
      (let [parse (-> @state :tooling-state deref :editor/features :result-for-renderer)
            parsed (parse result)]
        (.. div -classList (add "result" (when (-> parsed meta :error) "error")))
        (swap! results update id assoc :parsed parsed)
        (rdom/render [render/view-for-result parsed] div)))))

(defn all-parsed-results []
  (for [[_ {:keys [parsed]}] @results
        :when parsed]
    parsed))

(s/defn clear-results! [curr-editor :- TextEditor]
  (doseq [[_ {:keys [editor marker]}] @results
          :when (= (.-id curr-editor) (.-id editor))]
    (.destroy ^js marker)))
