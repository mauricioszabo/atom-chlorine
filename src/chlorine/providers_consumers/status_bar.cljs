(ns chlorine.providers-consumers.status-bar
  (:require [reagent.core :as r]
            [chlorine.state :refer [state]]))

(defonce status-bar (atom nil))
(defonce status-bar-tile (atom nil))

(defn- view []
  [:div
   (when (-> @state :repls :clj-eval)
     [:span
      " "
      [:img {:src (str "file://" js/__dirname "/clj.png") :width 18}]
      (cond-> " CLJ connected"
              (-> @state :config :refresh-mode (= :simple)) (str " (simple refresh)")
              (-> @state :config :refresh-mode (not= :simple)) (str " (full refresh)"))])

   (when (-> @state :repls :cljs-eval)
     [:span {:style {:margin-left "13px"}}
      [:img {:src (str "file://" js/__dirname "/cljs.png") :width 18}]
      " CLJS connected"])])

(defn activate [s]
  (swap! status-bar #(or % s))
  (let [div (. js/document (createElement "div"))]
    ; FIXME: Remove, debug info!
    (def div div)
    (.. div -classList (add "inline-block" "chlorine"))
    (reset! status-bar-tile (.
                              ^js @status-bar
                              (addRightTile #js {:item div :priority 101})))
    (r/render [view] div)))
