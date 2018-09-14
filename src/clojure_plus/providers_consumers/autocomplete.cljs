(ns clojure-plus.providers-consumers.autocomplete
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure-plus.repl :as repl]
            [clojure-plus.state :refer [state]]
            [repl-tooling.eval :as eval]
            [clojure-plus.ui.inline-results :as inline]
            [repl-tooling.features.autocomplete :as compl]))

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=_:]+")

(defn suggestions [{:keys [editor bufferPosition scopeDescriptor prefix activatedManually]}]
  (let [prefix (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex}))
        [range text] (repl/top-level-code editor bufferPosition)
        [row col] (if range
                    [(- (.-row bufferPosition) (.. range -start -row))
                     (.-column bufferPosition)]
                    [0 0])]

    (some-> @state :repls :clj-aux (compl/complete (repl/ns-for editor)
                                                   (str text)
                                                   prefix
                                                   row col)
            (.then #(->> %
                         (mapv (fn [{:keys [candidate type]}]
                                {:text candidate
                                 :type type
                                 :replacementPrefix prefix}))))
            (.then clj->js))))


(def sug (atom suggestions))

(def provider
  (fn []
    (prn [:PROVIDER-CALLED!])
    #js {:selector ".source.clojure"
         :disableForSelector ".source.clojure .comment"

         :inclusionPriority 100
         :excludeLowerPriority false

         :suggestionPriority 200

         :filterSuggestions true

         :getSuggestions (fn [data]
                           (-> data js->clj walk/keywordize-keys (@sug) clj->js))}))

   ; # (optional): (*experimental*) called when user the user selects a suggestion for the purpose of loading additional information about the suggestion.
   ; getSuggestionDetailsOnSelect: (suggestion) ->
   ; new Promise (resolve) ->
   ; resolve(newSuggestion)
