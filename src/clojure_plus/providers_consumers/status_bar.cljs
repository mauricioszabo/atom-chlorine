(ns clojure-plus.providers-consumers.status-bar
  (:require [reagent.core :as r]
            [clojure-plus.state :refer [state]]))

(defonce status-bar (atom nil))
(defonce status-bar-tile (atom nil))

(defn- view []
  [:div {:style {:padding-left "2px" :padding-right "2px"}}
   [:img {:src (str "file://" js/__dirname "/clj.png") :width 18}]
   (if (-> @state :repls :clj-eval)
     [:span " CLJ connected"]
     [:span " -_-zZ"])])

(defn activate [s]
  (println "ACTIVATE STATUS")
  (swap! status-bar #(or % s))
  (let [div (. js/document (createElement "div"))]
    ; FIXME: Remove, debug info!
    (def div div)
    (.. div -classList (add "inline-block" "clojure-plus"))
    (reset! status-bar-tile (.
                              @status-bar
                              (addRightTile #js {:item div :priority 101})))
    (r/render [view] div)))
