(ns chlorine.providers-consumers.autocomplete
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [chlorine.state :refer [state]]
            [promesa.core :as p]
            [repl-tooling.editor-integration.doc :as doc]
            [repl-tooling.eval :as eval]
            [repl-tooling.editor-integration.commands :as cmds]))

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=\?_:]+")

(defn- min-word-size []
  (.. js/atom -config (get "autocomplete-plus.minimumWordLength")))

(defn- treat-result [prefix {:keys [candidate type]}]
  {:text candidate
   :type type
   :replacementPrefix prefix})

(defn suggestions [{:keys [^js editor]}]
  (let [prefix (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex}))]
    (when (-> prefix count (>= (min-word-size)))
      (when-let [complete (some-> @state :tooling-state deref :editor/features :autocomplete)]
        (.. (complete)
            (then #(map (partial treat-result prefix) %))
            (then clj->js))))))

(defn- meta-for-var [var]
  (p/let [state (:tooling-state @state)
          res (cmds/run-feature! state :eval {:text (str "(meta (resolve '" var "))")
                                              :auto-detect true
                                              :aux true})]
    (:result res)))

(defn- detailed-suggestion [suggestion]
  (p/catch
   (p/let [txt (.-text suggestion)
           {:keys [arglists doc]} (meta-for-var txt)]
     (aset suggestion "rightLabel" (str arglists))
     (aset suggestion "description" (-> doc str (str/replace #"(\n\s+)" " ")))
     suggestion)
   (constantly nil)))

(def provider
  (fn []
    #js {:selector ".source.clojure"
         :disableForSelector ".source.clojure .comment"

         :inclusionPriority 100
         :excludeLowerPriority false

         :suggestionPriority 200

         :filterSuggestions true

         :getSuggestions (fn [data]
                           (-> data js->clj walk/keywordize-keys suggestions clj->js))

         :getSuggestionDetailsOnSelect #(detailed-suggestion %)}))
