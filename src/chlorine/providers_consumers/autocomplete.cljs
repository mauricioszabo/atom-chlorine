(ns chlorine.providers-consumers.autocomplete
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [chlorine.repl :as repl]
            [chlorine.state :refer [state]]
            [repl-tooling.eval :as eval]
            [chlorine.ui.inline-results :as inline]
            [repl-tooling.features.autocomplete :as compl]))

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=_:]+")

(defn suggestions [{:keys [^js editor ^js bufferPosition scopeDescriptor prefix activatedManually]}]
  (let [prefix (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex}))
        [range text] (repl/top-level-code editor bufferPosition)
        [row col] (if range
                    [(- (.-row bufferPosition) (.. ^js range -start -row))
                     (.-column bufferPosition)]
                    [0 0])
        ns-name (repl/ns-for editor)
        clj-completions (delay
                         (some-> @state :repls :clj-aux
                                 (compl/complete ns-name (str text) prefix row col)
                                 (.then #(->> %
                                              (map (fn [{:keys [candidate type]}]
                                                     {:text candidate
                                                      :type type
                                                      :replacementPrefix prefix}))))))]

    (if (repl/need-cljs? editor)
      (some-> @state :repls :cljs-eval
              (compl/complete ns-name (str text) prefix row col)
              (.then #(->> %
                        (map (fn [res] {:text res
                                        :type "function"
                                        :replacementPrefix prefix}))
                        clj->js)))
      (some-> @clj-completions (.then clj->js)))))


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
