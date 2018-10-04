(ns chlorine.features.refresh
  (:require [chlorine.state :refer [state]]
            [chlorine.ui.atom :as atom]
            [chlorine.repl :as repl]))

(defn full-command []
  '(do
     (require '[clojure.tools.namespace.repl])
     (alter-var-root #'clojure.test/*load-tests* (constantly false))
     (try
       (clojure.tools.namespace.repl/refresh-all)
       (finally
        (alter-var-root #'clojure.test/*test-out* (constantly *out*))
        (alter-var-root #'clojure.test/*load-tests* (constantly true))))))

(defn- refresh-editor [editor mode]
  (when-not (repl/need-cljs? editor)
    (let [ns-name (repl/ns-for editor)
          code (if (= :simple mode)
                 (str "(require '[" ns-name " :reload :all])")
                 (full-command))]
      (repl/evaluate-aux editor ns-name nil nil nil code
                         #(if (-> % :result (= :ok))
                            (atom/info "Refresh Successful" "")
                            (atom/warn "Failed to refresh" (:error %)))))))

(defn run-refresh! []
  (when (-> @state :refresh :on-save?)
    (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                    (-> @state :refresh :mode))))
