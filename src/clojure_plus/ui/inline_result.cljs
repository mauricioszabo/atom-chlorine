(ns clojure-plus.ui.inline-results
    (:require [cljs.reader :as reader]
              [clojure.string :as str]
              [clojure.walk :as walk]))

(defrecord WithTag [obj tag])
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
  (let [tag (when (instance? WithTag edn) (str "#" (:tag edn) " "))
        edn (cond-> edn (instance? WithTag edn) :obj)]
    (-> edn
        pr-str
        (str/replace #"\n$" "")
        (->> (str tag)))))

(declare to-tree)
(defn- as-map [[key val]]
  (let [k-str (str "[" (to-str key))
        v-str (str (to-str val) "]")]
    [:row [[k-str [(to-tree key)]]
           [v-str [(to-tree val)]]]]))

(defn- to-tree [edn]
  (let [txt (to-str edn)
        edn (cond-> edn (instance? WithTag edn) :obj)]
    (cond
      (map? edn) [txt (mapv as-map edn)]
      (coll? edn) [txt (mapv to-tree edn)]
      :else [txt])))

(defn- default-tag [tag data]
  (->WithTag data tag))

(defn parse [edn-string]
  (try
    (let [edn (reader/read-string {:default default-tag} edn-string)]
      (to-tree edn))
    (catch :default _
      (to-tree (symbol edn-string)))))

; (def foo "(#events_reports.events.NewService{:person {:id 1022118, :name \"Thiago Da Silva Pereira\", :document \"11007190752\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2017-09-03T00:00:00.000Z\", :due-date #time/utc \"2017-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 497385, :name \"Marceli Cristina Pedro da Silva\", :document \"14648631722\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2017-10-05T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 1577431, :name \"Ricardo Leitão da Silva\", :document \"77679466734\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2017-10-03T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 155288, :name \"Gustavo Cesar Amoreira Curty\", :document \"04747276700\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-04-06T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 32974, :name \"Marcio Cardoso Diniz\", :document \"07539407794\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-01-29T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 1261337, :name \"Ana Lucia Gomes Diniz\", :document \"08581709737\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-01-29T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 1973633, :name \"Cristiane Sanchotene Vaucher\", :document \"63768488004\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 468, :main-company-protheus-id 3, :acronym \"RJCCOP5\", :name \"Nsa. Copacabana 690\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-02-02T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 378526, :name \"Gabriel andrade ferreira\", :document \"16348917754\", :responsible-name \"Vilma lucia andrade souza\", :responsible-document \"01250673720\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-01-05T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 1547464, :name \"Veronica Germini de Cicco Marchetti\", :document \"08871589785\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 4, :main-company-protheus-id 3, :acronym \"RJCCOP1\", :name \"Barata Ribeiro\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-07-07T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}} #events_reports.events.NewService{:person {:id 1943077, :name \"Felipe Rodrigues da Silva\", :document \"07236167444\", :responsible-name \"\", :responsible-document \"\"}, :location {:id 468, :main-company-protheus-id 3, :acronym \"RJCCOP5\", :name \"Nsa. Copacabana 690\", :company-name \"SMART RIO ACADEMIA DE GINÁSTICA S/A.\"}, :plan {:name \"Black\", :value 99.9M, :start-date #time/utc \"2018-01-19T00:00:00.000Z\", :due-date #time/utc \"2018-10-01T00:00:00.000Z\"}})")

; (reader/read-string {:default default-tag} foo)

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
