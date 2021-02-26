(ns chlorine.ui.connection
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [chlorine.repl :as repl]
            [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [chlorine.utils :as aux]
            ["fs" :as node.fs]
            ["path" :as node.path]))

(defonce local-state
  (r/atom {:hostname "localhost"
           :port nil}))

(defn view []
  [:div.native-key-bindings.tab-able
   [:h2 "Connect to Socket REPL"]
   [:div.block
    [:label "Host: "]
    [:input.input-text {:type "text"
                        :tab-index "1"
                        :value (:hostname @local-state)
                        :on-change #(swap! local-state assoc :hostname (-> % .-target .-value))
                        :on-focus #(-> % .-target .select)}]]
   [:div.block
    [:label "Port: "]
    [:input.input-text {:type "text"
                        :tab-index "2"
                        :placeholder "port"
                        :value (:port @local-state)
                        :on-change #(swap! local-state assoc :port (-> % .-target .-value int))
                        :on-focus #(-> % .-target .select)}]]])

(defn destroy! [^js panel]
  (.destroy panel)
  (aux/refocus!))

(defn- treat-key [cmd panel event]
  (case (.-key event)
    "Escape" (destroy! panel)
    "Enter" (cmd panel)
    :no-op))

(defn- as-clj [nodelist]
  (js->clj (.. js/Array -prototype -slice (call nodelist))))

(defn- set-port-from-file! []
  (let [port-file (->> [[".shadow-cljs" "socket-repl.port"]
                        [".socket-repl-port"]
                        [".nrepl-port"]]
                       (map (fn [path]
                             (apply node.path/join
                                    (-> js/atom .-project .getPaths first)
                                    path)))
                       (filter node.fs/existsSync)
                       first)]
   (when port-file
    (swap! local-state assoc :port (-> port-file node.fs/readFileSync .toString int)))))

(defn conn-view [cmd]
  (let [div (. js/document (createElement "div"))
        panel (.. js/atom -workspace (addModalPanel #js {:item div}))]
    (set-port-from-file!)
    (rdom/render [view] div)
    (aux/save-focus! div)
    (doseq [elem (-> div (.querySelectorAll "input") as-clj)]
      (aset elem "onkeydown" (partial treat-key cmd panel)))))

(defn- already-connected []
  (atom/warn "REPL already connected"
             (str "REPL is already connected.\n\n"
                  "Please, disconnect the current REPL "
                  "if you want to connect to another.")))

(defn connect-socket! []
  (if (-> @state :repls :clj-eval nil?)
    (conn-view (fn [panel]
                 (repl/connect-socket! (:hostname @local-state) (or (:port @local-state)
                                                                    0))
                 (destroy! panel)))
    (already-connected)))
