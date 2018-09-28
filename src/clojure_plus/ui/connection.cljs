(ns clojure-plus.ui.connection
  (:require [reagent.core :as r]
            [cljsjs.react :as react]
            [clojure-plus.repl :as repl]
            [clojure-plus.state :refer [state]]
            [clojure-plus.ui.atom :as atom]
            [repl-tooling.repl-client :as repl-client]
            [repl-tooling.repl-client.clojure :as clj-repl]
            [clojure-plus.aux :as aux]))

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

(defn destroy! [panel]
  (.destroy panel)
  (aux/refocus!))

(defn- repl-connect! [panel]
  (repl/connect! (:hostname @local-state) (:port @local-state))
  (destroy! panel))

(defn- treat-key [cmd panel event]
  (case (.-key event)
    "Escape" (destroy! panel)
    "Enter" (cmd panel)
    :no-op))

(defn- as-clj [nodelist]
  (js->clj (.. js/Array -prototype -slice (call nodelist))))

(defn conn-view [cmd]
  (let [div (. js/document (createElement "div"))
        panel (.. js/atom -workspace (addModalPanel #js {:item div}))]
    (r/render [view] div)
    (aux/save-focus! div)
    (doseq [elem (-> div (.querySelectorAll "input") as-clj)]
      (aset elem "onkeydown" (partial treat-key cmd panel)))))

(defn- already-connected []
  (atom/warn "REPL already connected"
             (str "REPL is already connected.\n\n"
                  "Please, disconnect the current REPL "
                  "if you want to connect to another REPL")))

(defn connect! []
  (if (-> @state :repls :clj-eval nil?)
    (conn-view repl-connect!)
    (already-connected)))

(defn connect-self-hosted! []
  (cond
    (-> @state :repls :clj-eval nil?) (atom/warn "REPL not connected"
                                                 (str "To connect a self-hosted REPL, "
                                                      "you first need to connect a "
                                                      "Clojure REPL"))
    (-> @state :repls :cljs-eval nil?) (repl/connect-self-hosted)
    :else (already-connected)))

(defn disconnect! []
  (repl-client/disconnect! :clj-eval)
  (repl-client/disconnect! :clj-aux)
  (repl-client/disconnect! :cljs-eval)
  (swap! state assoc
         :repls {:clj-eval nil
                 :cljs-eval nil
                 :clj-aux nil}
         :connection nil))
