(ns chlorine.features.refresh
  (:require [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
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
           (clojure.core/alter-var-root #'clojure.test/*test-out* (clojure.core/constantly *out*))
           (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly true)))))
    '(do
       (clojure.core/require '[clojure.tools.namespace.repl])
       (try
         (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly false))
         (clojure.tools.namespace.repl/refresh)
         (finally
           (clojure.core/alter-var-root #'clojure.test/*test-out* (clojure.core/constantly *out*))
           (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly true)))))))

(defn- refresh-editor [editor mode]
  (when-not (repl/need-cljs? editor)
    (let [ns-name (repl/ns-for editor)
          code (if (= :simple mode)
                 (str "(do (require '[" ns-name " :reload :all]) :ok)")
                 (full-command))]
      (repl/evaluate-aux editor ns-name nil nil nil code
                         #(if (-> % :result (= :ok))
                            (do
                              (swap! state assoc-in [:refresh :needs-clear?] false)
                              (atom/info "Refresh Successful" ""))
                            (do
                              (swap! state assoc-in [:refresh :needs-clear?] true)
                              (atom/warn "Failed to refresh" (:error %))))))))

(defn run-refresh! []
  (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                  (-> @state :config :refresh-mode)))

(defn run-editor-refresh! []
  (when (-> @state :config :refresh-on-save)
    (run-refresh!)))

(defn toggle-refresh []
  (swap! state update-in [:config :refresh-mode] {:simple :full, :full :simple}))
