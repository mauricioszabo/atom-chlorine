(ns clojure-plus.ui.connection
  (:require [reagent.core :as r]
            [cljsjs.react :as react]
            [clojure-plus.repl :as repl]
            [clojure-plus.state :refer [state]]))

(defonce local-state
  (r/atom {:hostname "localhost"
           :port ""}))

(defn view []
  [:div.native-key-bindings.tab-able
   [:h2 "Connect to Socket REPL"]
   [:div.block
    [:label "Host: "]
    [:input.input-text {:type "text"
                        :value (:hostname @local-state)
                        :on-change #(swap! local-state assoc :hostname (-> % .-target .-value))}]]
   [:div.block
    [:label "Port: "]
    [:input.input-text {:type "text"
                        :placeholder "port"
                        :value (:port @local-state)
                        :on-change #(swap! local-state assoc :port (-> % .-target .-value int))}]]])
   ; [:div
    ; [:button.btn.btn-primary "Connect"]]])

(defn- repl-connect! [panel]
  (repl/connect! (:hostname @local-state) (:port @local-state))
  (.destroy panel))

(defn- treat-tab [panel event]
  (println "key press" (.-key event))
  (case (.-key event)
    "Escape" (.destroy panel)
    "Enter" (repl-connect! panel)
    :no-op))

(defn- as-clj [nodelist]
  (js->clj (.. js/Array -prototype -slice (call nodelist))))

(defn conn-view []
  (let [div (. js/document (createElement "div"))
        panel (.. js/atom -workspace (addModalPanel #js {:item div}))]
    (r/render [view] div)
    (doseq [elem (-> div (.querySelectorAll "input") as-clj)]
      (aset elem "onkeydown" (partial treat-tab panel)))))

(defn connect! []
  (if (-> @state :repls :clj-eval)
    (.. js/atom -notifications (addWarning "REPL already connected"
                                           #js {:detail "REPL is already connected.

Please, disconnect the current REPL if you want to connect to another REPL"}))
    (conn-view)))

(connect!)
