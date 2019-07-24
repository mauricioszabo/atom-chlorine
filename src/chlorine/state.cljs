(ns chlorine.state
  (:require [reagent.core :as r]))

(def configs {:eval-mode
              {:description "Should we evaluate Clojure or ClojureScript?"
               :type [:discover :prefer-cljs :clj :cljs]
               :default :discover}

              :refresh-mode
              {:description "Should we use clojure.tools.namespace to refresh, or a simple require?"
               :type [:full :simple]
               :default :simple}

              :refresh-on-save
              {:description "Should we refresh namespaces when we save a file (Clojure Only)?"
               :type :boolean
               :default false}

              :experimental-features
              {:description "Enable experimental (and possibly unstable) features?"
               :type :boolean
               :default false}})

(defn- seed-configs []
  (->> configs
       (map (fn [[k v]] [k (:default v)]))
       (into {})))

(defonce state
  (r/atom {:repls {:clj-eval nil
                   :cljs-eval nil
                   :clj-aux nil}
           :refresh {:needs-clear? true}
           :config (seed-configs)}))
