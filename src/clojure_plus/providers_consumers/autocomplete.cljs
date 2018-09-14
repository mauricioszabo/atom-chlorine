(ns clojure-plus.providers-consumers.autocomplete
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure-plus.repl :as repl]
            [clojure-plus.state :refer [state]]
            [repl-tooling.eval :as eval]
            [clojure-plus.ui.inline-results :as inline]))
42168

(def clj-var-regex #"[a-zA-Z0-9\-.$!?\/><*=_:]+")

(def text "(
  (+ 1 2)
  (+ foo/ba 4)
)")

(defn re-escape [str]
  (str/replace str #"[.*+?^${}()|\[\]\\]" "\\$&"))

(defn- make-context [text prefix row col]
  (let [lines (str/split-lines text)
        pattern (re-pattern (str "(.{" (- col (count prefix)) "})" (re-escape prefix)))]
    (->> "$1__prefix__"
         (update lines row str/replace-first pattern)
         (str/join "\n"))))

(declare take-results)
(defn- get-more [repl resolve more acc]
  (eval/evaluate repl more {} #(take-results repl resolve acc %)))

(defn- take-results [repl resolve acc {:keys [result]}]
  (let [acc (vec (concat acc (inline/read-result result)))
        more (-> acc last :repl-tooling/...)
        size (count acc)]
    (cond
      (-> size (> 50) (and more)) (-> acc butlast resolve)
      ; (> size 50) (resolve acc)
      (-> size (< 50) (and more)) (get-more repl resolve more acc)
      :else (resolve acc))))

  ; (let [parsed (inline/read-result result)]
  ;   (prn [:res parsed])))

(defn complete [repl ns context prefix]
  (let [ns (symbol ns)
        code `(do
                (clojure.core/require 'compliment.core)
                (clojure.core/let [completions# (compliment.core/completions
                                                 ~prefix
                                                 {:tag-candidates true
                                                  :ns '~ns
                                                  :context ~context})]
                  (clojure.core/vec completions#)))]
    (js/Promise. (fn [resolve]
                   (eval/evaluate repl code {} #(take-results repl resolve [] %))))))

(defn suggestions [{:keys [editor bufferPosition scopeDescriptor prefix activatedManually]}]
  (let [prefix (.. editor (getWordUnderCursor #js {:wordRegex clj-var-regex}))
        [range text] (repl/top-level-code editor bufferPosition)
        context (make-context text prefix
                              (- (.-row bufferPosition) (.. range -start -row))
                              (.-column bufferPosition))]

    (some-> @state :repls :clj-aux (complete (repl/ns-for editor) context prefix)
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

   ; # (optional): called _after_ the suggestion `replacementPrefix` is replaced
   ; # by the suggestion `text` in the buffer
   ; onDidInsertSuggestion: ({editor, triggerPosition, suggestion}) ->

   ; # (optional): called when your provider needs to be cleaned up. Unsubscribe
   ; # from things, kill any processes, etc.
   ; dispose: ->})
