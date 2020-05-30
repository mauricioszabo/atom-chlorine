(ns chlorine.features.refresh
  (:require [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [chlorine.ui.console :as console]
            [repl-tooling.editor-helpers :as helpers]
            [repl-tooling.editor-integration.evaluation :as e-eval]
            [chlorine.repl :as repl]))

(defn full-command []
  (if (-> @state :refresh :needs-clear?)
    '(do
       (clojure.core/require '[clojure.tools.namespace.repl])
       (clojure.core/require '[clojure.test])
       (clojure.tools.namespace.repl/clear)
       (clojure.tools.namespace.repl/refresh-all))
    '(do
       (clojure.core/require '[clojure.tools.namespace.repl])
       (clojure.tools.namespace.repl/refresh))))

(defn- refresh-editor [editor mode]
  (when-not (e-eval/need-cljs? (:config @state) (.getFileName editor))
    (when-let [evaluate (some-> @state :tooling-state deref :editor/features :eval)]
      (let [editor-data (repl/get-editor-data)
            [_ ns-name] (helpers/ns-range-for (:contents editor-data)
                                              (-> editor-data :range first))
            code (if (= :simple mode)
                   (str "(do (require '[" ns-name " :reload :all]) :ok)")
                   (full-command))]
        (.. (evaluate {:code code :aux true})
            (then #(if (-> % :result (= :ok))
                     (do
                       (swap! state assoc-in [:refresh :needs-clear?] false)
                       (atom/info "Refresh Successful" ""))
                     (do
                       (swap! state assoc-in [:refresh :needs-clear?] true)
                       (atom/warn "Failed to refresh" nil)
                       (console/result {:id (gensym "refresh")
                                        :editor-data editor-data
                                        :repl (-> @state :tooling-state deref :clj/aux)
                                        :result (-> %
                                                    (dissoc :result)
                                                    (assoc :error (:result %)))}))))
            (catch (fn [result]
                     (swap! state assoc-in [:refresh :needs-clear?] true)
                     (console/result {:id (gensym "refresh")
                                      :editor-data editor-data
                                      :repl (-> @state :tooling-state deref :clj/aux)
                                      :result result}))))))))

(defn run-refresh! []
  (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                  (-> @state :config :refresh-mode)))

(defn run-editor-refresh! []
  (when (-> @state :config :refresh-on-save)
    (run-refresh!)))

(defn toggle-refresh []
  (swap! state update-in [:config :refresh-mode] {:simple :full, :full :simple}))
