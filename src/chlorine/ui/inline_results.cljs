(ns chlorine.ui.inline-results
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [reagent.core :as r]
            [repl-tooling.editor-integration.renderer :as render]
            [clojure.walk :as walk]
            [repl-tooling.eval :as eval]
            [repl-tooling.editor-helpers :as editor-helpers]
            [chlorine.state :refer [state]]))

(defonce ink (atom nil))

(defn ^js new-result [^js editor row]
  (when-let [InkResult (some-> ^js @ink .-Result)]
    (InkResult. editor #js [row row] #js {:type "block"})))

(defn- create-div! [parsed-ratom error?]
  (let [div (. js/document createElement "div")]
    (when error? (.. div -classList (add "error")))
    (.. div -classList (add "result" "chlorine"))
    (r/render [render/view-for-result parsed-ratom] div)
    div))

(defn render-on-console! [^js console parsed-result]
  (let [parsed (render/parse-result parsed-result (-> @state :repls :clj-eval))
        div (create-div! parsed false)]
    (some-> console (.result div))))

(defn render-inline! [^js inline-result parsed-result]
  (let [parsed-ratom (render/parse-result parsed-result (-> @state :repls :clj-eval))
        div (create-div! parsed-ratom false)]
    (.setContent inline-result div #js {:error false})))

(defn render-error! [^js inline-result parsed-result]
  (let [parsed-ratom (render/parse-result parsed-result (-> @state :repls :clj-eval))
        div (create-div! parsed-ratom true)]
    (.setContent inline-result div #js {:error true})))
