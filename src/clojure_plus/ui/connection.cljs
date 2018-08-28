(ns clojure-plus.ui.connection
  (:require [reagent.core :as r]
            [cljsjs.react :as react]))

(defonce state
  (r/atom {:username "localhost"
           :port ""}))

; function(event){ if(event.key === "Tab"){ if(event.shiftKey){ previousInputElement.focus(); }else{ nextInputElement.focus(); } }else if(event.key === "Enter"){ submitForm(); } }
(defn view []
  [:div.native-key-bindings.tab-able
   [:h2 "Connect to Socket REPL"]
   [:div.block
    [:label "Host: "]
    [:input.input-text {:type "text"
                        :value (:username @state)
                        :on-change #(swap! state assoc :username (-> % .-target .-value))}]]
   [:div.block
    [:label "Port: "]
    [:input.input-text {:type "text"
                        :placeholder "port"
                        :value (:port @state)
                        :on-change #(swap! state assoc :port (-> % .-target .-value int))}]]
   [:div
    [:button.btn.btn-primary "Connect"]]])

(defn- treat-tab [panel event]
  (println "key press" (.-key event))
  (case (.-key event)
    "Escape" (.destroy panel)
    :no-op))

(defn- as-clj [nodelist]
  (js->clj (.. js/Array -prototype -slice (call nodelist))))

(defn conn-view []
  (let [div (. js/document (createElement "div"))
        panel (.. js/atom -workspace (addModalPanel #js {:item div}))]
    (r/render [view] div)
    (doseq [elem (-> div (.querySelectorAll "input") as-clj)]
      (aset elem "onkeydown" (partial treat-tab panel)))))

(conn-view)
