(ns clojure-plus.ui.inline-results
    (:require [cljs.reader :as reader]
              [clojure.string :as str]
              [clojure.walk :as walk]))

(defprotocol Taggable
  (obj [this])
  (tag [this]))

(deftype WithTag [obj tag]
  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (-write writer "#")
    (-write writer tag)
    (-write writer " ")
    (-write writer obj))

  Taggable
  (obj [_] obj)
  (tag [_] (str "#" tag " ")))

(defonce ink (atom nil))

(defn new-result [editor row]
  (let [InkResult (.-Result @ink)]
    (InkResult. editor #js [0 row] #js {:type "block"})))

(defn- ink-tree [header elements block?]
  (cond-> (-> @ink .-tree (.treeView header (clj->js elements)))
          block? (doto (-> .-classList (.add "line")))))

(defn set-content [result header elements]
  (let [contents (ink-tree header elements true)]
    (.setContent result contents #js {:error false})))

(defn- to-str [edn]
  (let [tag (when (instance? WithTag edn) (tag edn))
        edn (cond-> edn (instance? WithTag edn) obj)]

    (-> edn
        pr-str
        (str/replace #"\{:repl-tooling/\.\.\. .+?\}" "...")
        (->> (str tag)))))

(declare to-tree)
(defn- as-map [[key val]]
  (let [k-str (str "[" (to-str key))
        v-str (str (to-str val) "]")]
    [:row [[k-str [(to-tree key)]]
           [v-str [(to-tree val)]]]]))

(defn- to-tree [edn]
  (let [txt (to-str edn)
        edn (cond-> edn (instance? WithTag edn) obj)]
    (cond
      (map? edn) [txt (mapv as-map edn)]
      (coll? edn) [txt (mapv to-tree edn)]
      :else [txt])))

(defn- default-tag [tag data]
  (WithTag. data tag))

(defn parse [edn-string]
  (try
    (let [edn (reader/read-string {:default default-tag} edn-string)]
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
