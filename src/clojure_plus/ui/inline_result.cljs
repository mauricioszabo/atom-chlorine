(ns clojure-plus.ui.inline-results
    (:require [cljs.reader :as reader]
              [clojure.string :as str]
              [clojure.walk :as walk]))

(def InkResult (-> js/ink .-Result))

(defn new-result [editor row]
  (InkResult. editor #js [0 row] #js {:type "inline"}))

(defn- ink-tree [header elements block?]
  (cond-> (-> js/ink .-tree (.treeView header (clj->js elements)))
          block? (doto (-> .-classList (.add "line")))))

(defn set-content [result header elements]
  (let [contents (ink-tree header elements true)]
    (.setContent result contents #js {:error false})))

(defn- to-str [edn]
  (let [tag (some-> edn meta :tag (str " "))]
    (-> edn
        prn
        with-out-str
        (str/replace #"\n$" "")
        (->> (str tag)))))

(declare to-tree)
(defn- as-map [[key val]]
  (let [k-str (str "[" (to-str key))
        v-str (str (to-str val) "]")]
    [:row [[k-str [(to-tree key)]]
           [v-str [(to-tree val)]]]]))

(defn- to-tree [edn]
  (let [txt (to-str edn)]
    (cond
      (map? edn) [txt (mapv as-map edn)]
      (coll? edn) [txt (mapv to-tree edn)]
      :else [txt])))

(defn parse [edn-string]
  (try
    (let [edn (reader/read-string edn-string)]
      (to-tree edn))
    (catch :default _
      (to-tree (symbol edn-string)))))

(defn- leaf [text]
  (doto (.createElement js/document "div")
    (aset "innerText" text)))

(declare to-html)
(defn- html-row [children]
  (let [div (.createElement js/document "div")]
    (doseq [child children]
      (.appendChild div (to-html child)))
    div))

(defn- to-html [[header children]]
  (cond
    (empty? children) (leaf header)
    (= :row header) (html-row children)
    :else (ink-tree header (mapv to-html children) false)))

(defn set-content! [result result-tree]
  (let [contents (to-html result-tree)]
    (.setContent result contents #js {:error false})))

(reader/register-default-tag-parser!
 (fn [tag data]
   (with-meta data {:tag (str "#" tag)})))
