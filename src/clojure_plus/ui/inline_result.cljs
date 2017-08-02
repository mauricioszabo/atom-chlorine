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

; (let [edn (reader/read-string "{:a 20}")]
;   (walk/postwalk to-tree edn))
;
; (parse "{:a 20}")

(comment
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
