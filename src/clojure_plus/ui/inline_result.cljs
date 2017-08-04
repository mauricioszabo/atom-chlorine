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
  (-> edn
      prn
      with-out-str
      (str/replace #"\n$" "")))

(declare to-tree)
(defn- as-map [[key val]]
  (let [k-str (str "{" (to-str key))
        v-str (str (to-str val) "}")]
    [[:inline k-str [(to-tree key)]]
     [:inline v-str [(to-tree val)]]]))

(defn- to-tree
  ([edn] (to-tree edn (to-str edn)))
  ([edn txt]
   (cond
     (map? edn) [:block txt (mapcat as-map edn)]
     (coll? edn) [:block txt (mapv to-tree edn)]
     :else [:block txt])))

(defn parse [edn-string]
  (let [edn (reader/read-string edn-string)]
    (to-tree edn)))

(defn- leaf [text]
  (let [inner (.createElement js/document "div")
        inner-class (.-classList inner)]

    ; (.add inner-class "header" "gutted" "closed")
    (aset inner "innerText" text)
    inner))

(defn- to-html [[kind header children]]
  (cond
    (empty? children) (leaf header)
    (= :inline kind) (ink-tree header (mapv to-html children) false)
    :block (ink-tree header (mapv to-html children) true)))

(defn set-content! [result result-tree]
  (let [;contents (ink-tree header elements true)
        contents result-tree]
    (.setContent result contents #js {:error false})))

(comment
 (set-content! (new-result (js/ce) 60)
   (to-html (parse "{:a {:b [10 20 30]}}")))

 (set-content! (new-result (js/ce) 60)
   (ink-tree "[1 2 3]"
             [(ink-tree "1" ["1"] false)
              (ink-tree "2" ["V"] false)
              (ink-tree "3" ["D"] false)]
             false))

 (set-content! (new-result (js/ce) 60)
   (leaf "FOO"))

 (set-content! (new-result (js/ce) 40)
   (-> js/ink .-tree (.treeView "FOO")))

 (set-content! (new-result (js/ce) 40)
   (ink-tree "[1 2 3]"
             []
             false))

 (set-content! (new-result (js/ce) 40)
   (ink-tree "[1 2 3]"
             [(ink-tree "1" ["1"] false)
              (ink-tree "2" ["V"] false)
              (ink-tree "3" ["D"] false)]
             false))

              ; [(ink-tree "bar" ["B" "A" "BA"] false) (ink-tree "baz" ["B"] false)]))
 (identity ["bar" "baz" "B" "B"])
 (-> (js/ce) .getBuffer .getLines (nth 2) count)
 (def r (new-result (js/ce) 4))
 (aset js/window "r" (ink-tree "foo" ["bar" "baz"] true))
 (.log js/console (ink-tree "foo" ["bar" "baz"] true))
 (set-content r "foo" [(ink-tree "barasdasdasdasdasdasdasd" ["B"]) (ink-tree "bazasdasdasdasdadsasd" ["B"])])
 (set-content r (doto (.createElement js/document "div") (aset "innerHTML" "FOO")))
 (set-content (new-result (js/ce) 40)
              "BAR"
              [])
              ; [(ink-tree "bar" ["B" "A" "BA"] false) (ink-tree "baz" ["B"] false)])
 (set-content (new-result (js/ce) 40)
              "[\"bar\" \"baz\"]"
              [(ink-tree "bar" ["B" "A" "BA"] false) (ink-tree "baz" ["B"] false)])

 (reader/read-string "
#object[clojure.lang.Atom 0x17bcda63 {:status :ready, :val [:foo {:bar \"BAZ\"}]}]
")

 (defrecord UnknownObject [tag child])
 (->UnknownObject "#foo" [10 20])
 (print-method 10 *out*)

 (reader/register-default-tag-parser!
  (fn [tag data]
    (.log js/console data)
    data)))

; (reader/read-string "{:a 10 \"foo\" #object [cljs.core.Atom {:val \"Bar\"}]}")
; (println (str {:a "FOO"
;                :b (atom "Bar")}))
