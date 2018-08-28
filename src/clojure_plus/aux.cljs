(ns clojure-plus.aux)

(def ^:private atom-ed (js/require "atom"))
(def ^:private CompositeDisposable (.-CompositeDisposable atom-ed))
(def subscriptions (atom (CompositeDisposable.)))

(defn reload-subscriptions! []
  (reset! subscriptions (CompositeDisposable.)))

(defn command-for [name f]
  (let [disp (-> js/atom .-commands (.add "atom-text-editor"
                                          (str "clojure-plus-reloaded:" name)
                                          f))]
    (.add @subscriptions disp)))

(defn aux [param]
  (println "Some" param))
