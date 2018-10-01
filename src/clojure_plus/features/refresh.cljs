(ns clojure-plus.features.refresh
  (:require [clojure-plus.state :refer [state]]
            [clojure-plus.ui.atom :as atom]
            [clojure-plus.repl :as repl]))

(defn full-command []
  '(do
     (require '[clojure.tools.namespace.repl])
     (clojure.tools.namespace.repl/refresh-all)))

(defn- refresh-editor [editor mode]
  (when-not (repl/need-cljs? editor)
    (let [ns-name (repl/ns-for editor)
          code (if (= :simple mode)
                 (str "(require '[" ns-name " :reload :all])")
                 (full-command))]
      (repl/evaluate-aux editor ns-name nil nil nil code
                         #(if-let [res (:result %)]
                            (atom/info "Refresh Successful" "")
                            (atom/warn "Failed to refresh" (:error %)))))))

(defn run-refresh! []
  (when (-> @state :refresh :on-save?)
    (refresh-editor (.. js/atom -workspace getActiveTextEditor)
                    (-> @state :refresh :mode))))
