(ns clojure-plus.helpers)

(def ^:private CompositeDisposable (.-CompositeDisposable (js/require "atom")))

(def ^:private commands (atom (CompositeDisposable.)))

(defn add-command [selector command function]
  (let [disposable (-> js/atom .-commands (.add selector command function))]
    (.add @commands disposable)))

(defn remove-all-commands []
  (.dispose @commands)
  (reset! commands (CompositeDisposable.)))
