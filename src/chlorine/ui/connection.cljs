(ns chlorine.ui.connection
  (:require [reagent.core :as r]
            [cljsjs.react :as react]
            [chlorine.repl :as repl]
            [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [repl-tooling.repl-client :as repl-client]
            [repl-tooling.repl-client.clojure :as clj-repl]
            [chlorine.aux :as aux]
            [repl-tooling.editor-integration.connection :as connection]
            ["fs" :refer [existsSync readFileSync]]))

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
                        :on-change #(swap! local-state assoc :hostname (-> % .-target .-value))
                        :on-focus #(-> % .-target .select)}]]
   [:div.block
    [:label "Port: "]
    [:input.input-text {:type "text"
                        :placeholder "port"
                        :value (:port @local-state)
                        :on-change #(swap! local-state assoc :port (-> % .-target .-value int))
                        :on-focus #(-> % .-target .select)}]]])

(defn destroy! [^js panel]
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
        panel (.. js/atom -workspace (addModalPanel #js {:item div}))
        port-file (-> js/atom .-project .getPaths first
                      (str "/.shadow-cljs/socket-repl.port"))]
    (when (existsSync port-file)
      (swap! local-state assoc :port (-> port-file readFileSync .toString int)))
    (r/render [view] div)
    (aux/save-focus! div)
    (doseq [elem (-> div (.querySelectorAll "input") as-clj)]
      (aset elem "onkeydown" (partial treat-key cmd panel)))))

(defn- already-connected []
  (atom/warn "REPL already connected"
             (str "REPL is already connected.\n\n"
                  "Please, disconnect the current REPL "
                  "if you want to connect to another.")))

(defn connect! []
  (if (-> @state :repls :clj-eval nil?)
    (conn-view repl-connect!)
    (already-connected)))

(defn connect-cljs! []
  (if (-> @state :repls :cljs-eval nil?)
    (conn-view #(do
                  (repl/connect-cljs! (:hostname @local-state) (:port @local-state))
                  (destroy! %)))
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
  (connection/disconnect!))
