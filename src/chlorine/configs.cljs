(ns chlorine.configs
  (:require [chlorine.state :refer [configs state]]
            [clojure.walk :as walk]
            [chlorine.aux :as aux]
            [check.core :refer-macros [check]]))

(defn- propagate-to-config [new-value]
  (let [config (. js/atom -config)]
    (doseq [[key value] (:config new-value)
            :let [atom-key (str "chlorine." (name key))
                  atom-value (clj->js value)]
            :when (not= atom-value (.get config atom-key))]
      (.set config atom-key atom-value))))

(defn- propagate-to-state [new-value]
  (let [normalized (-> new-value js->clj walk/keywordize-keys
                    (update :eval-mode keyword)
                    (update :refresh-mode keyword))]
    (when-not (-> @state :config (= normalized))
      (swap! state assoc :config normalized))))

(defn- transform-config [configs]
  (let [type-for (fn [{:keys [type]}] (if (vector? type)
                                        :string
                                        type))]
    (->> configs
         (map (fn [[k v]] [k (cond-> {:type (type-for v)
                                      :title (:description v)
                                      :default (:default v)}
                                     (vector? (:type v)) (assoc :enum (:type v)))]))
         (into {}))))

(when aux/TESTS
  (check (transform-config {:foo {:type [:a :b] :default :a
                                  :description "Some Foo"}})
         => {:foo {:type :string :enum [:a :b] :default :a
                             :title "Some Foo"}})

  (check (transform-config {:bar {:type :boolean :default false :description "BAR"}})
         => {:bar {:type :boolean :default false :title "BAR"}}))

(defn get-configs []
  (-> configs
      transform-config
      (merge {:console-pos {:type "string"
                            :title "Position of console when connecting REPL"
                            :enum ["right" "down"]
                            :default "right"}})
      clj->js))

(defn observe-configs! []
  (.add @aux/subscriptions
        (.. js/atom -config (observe "chlorine" propagate-to-state)))
  (add-watch state :configs (fn [_ _ _ value] (propagate-to-config value))))
