(ns chlorine.features.refresh
  (:require [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [chlorine.repl :as repl]))

(defn clear-and-refresh [])

(defn full-command []
  (if (-> @state :refresh :needs-clear?)
    '(try
       (require '[clojure.tools.namespace.repl])
       (alter-var-root #'clojure.test/*load-tests* (constantly false))
       (clojure.tools.namespace.repl/clear)
       (clojure.tools.namespace.repl/refresh-all)
       (finally
         (alter-var-root #'clojure.test/*test-out* (constantly *out*))
         (alter-var-root #'clojure.test/*load-tests* (constantly true))))))

(defn- refresh-editor [editor mode]
  (when-not (repl/need-cljs? editor)
    (let [ns-name (repl/ns-for editor)
          code (if (= :simple mode)
                 (str "(do (require '[" ns-name " :reload :all]) :ok)")
                 (full-command))]
      (prn code)
      (repl/evaluate-aux editor ns-name nil nil nil code
                         #(if (-> % :result (= :ok))
                            (atom/info "Refresh Successful" "")
                            (atom/warn "Failed to refresh" (:error %)))))))

(defn run-refresh! []
  (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                  (-> @state :config :refresh-mode)))

(defn run-editor-refresh! []
  (when (-> @state :config :refresh-on-save)
    (run-refresh!)))

(defn toggle-refresh []
  (swap! state update-in [:config :refresh-mode] {:simple :full, :full :simple}))
