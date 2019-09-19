(ns chlorine.providers-consumers.autocomplete
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [chlorine.repl :as repl]
            [chlorine.state :refer [state]]
            [repl-tooling.eval :as eval]
            [chlorine.ui.inline-results :as inline]
            [repl-tooling.features.autocomplete :as compl]))

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=\?_:]+")

(defn- min-word-size []
  (.. js/atom -config (get "autocomplete-plus.minimumWordLength")))

(defn- treat-result [prefix {:keys [candidate type]}]
  {:text candidate
   :type type
   :replacementPrefix prefix})

(defn suggestions [{:keys [^js editor ^js bufferPosition]}]
  (let [prefix (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex}))]
    (when (-> prefix count (>= (min-word-size)))
      (let [complete (some-> @state :tooling-state deref :editor/features :autocomplete)]
        (.. (complete)
            (then #(map (partial treat-result prefix) %))
            (then clj->js))))))

(def provider
  (fn []
    #js {:selector ".source.clojure"
         :disableForSelector ".source.clojure .comment"

         :inclusionPriority 100
         :excludeLowerPriority false

         :suggestionPriority 200

         :filterSuggestions true

         :getSuggestions (fn [data]
                           (-> data js->clj walk/keywordize-keys suggestions clj->js))}))

   ; # (optional): (*experimental*) called when user the user selects a suggestion for the purpose of loading additional information about the suggestion.
   ; getSuggestionDetailsOnSelect: (suggestion) ->
   ; new Promise (resolve) ->
   ; resolve(newSuggestion)
