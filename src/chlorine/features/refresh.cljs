(ns chlorine.features.refresh
  (:require [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [chlorine.ui.console :as console]
            [repl-tooling.editor-helpers :as helpers]
            [chlorine.repl :as repl]))

(defn full-command []
  (if (-> @state :refresh :needs-clear?)
    '(do
       (clojure.core/require '[clojure.tools.namespace.repl])
       (clojure.core/require '[clojure.test])
       (try
         (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly false))
         (clojure.tools.namespace.repl/clear)
         (clojure.tools.namespace.repl/refresh-all)
         (finally
           (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly true)))))
    '(do
       (clojure.core/require '[clojure.tools.namespace.repl])
       (try
         (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly false))
         (clojure.tools.namespace.repl/refresh)
         (finally
           (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly true)))))))

(defn- refresh-editor [editor mode]
  (when-not (repl/need-cljs? editor)
    (let [evaluate (-> @state :tooling-state deref :editor/features :eval)
          editor-data (repl/get-editor-data)
          [_ ns-name] (helpers/ns-range-for (:contents editor-data)
                                            (-> editor-data :range first))
          code (if (= :simple mode)
                 (str "(do (require '[" ns-name " :reload :all]) :ok)")
                 (full-command))]
      (.. (evaluate code {})
          (then #(if (-> % :result (= :ok))
                   (do
                     (swap! state assoc-in [:refresh :needs-clear?] false)
                     (atom/info "Refresh Successful" ""))
                   (do
                     (swap! state assoc-in [:refresh :needs-clear?] true)
                     (atom/warn "Failed to refresh" nil)
                     (console/result {:id (gensym "refresh")
                                      :editor-data editor-data
                                      :result (-> %
                                                  (dissoc :result)
                                                  (assoc :error (:result %)))}))))
          (catch (fn [result]
                   (swap! state assoc-in [:refresh :needs-clear?] true)
                   (console/result {:id (gensym "refresh")
                                    :editor-data editor-data
                                    :result result})))))))

(defn run-refresh! []
  (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                  (-> @state :config :refresh-mode)))

(defn run-editor-refresh! []
  (when (-> @state :config :refresh-on-save)
    (run-refresh!)))

(defn toggle-refresh []
  (swap! state update-in [:config :refresh-mode] {:simple :full, :full :simple}))
