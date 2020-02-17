(ns chlorine.utils
  (:require [chlorine.state :refer [state]]
            ["atom" :refer [CompositeDisposable]]))

(def subscriptions (atom (CompositeDisposable.)))

(defn reload-subscriptions! []
  (reset! subscriptions (CompositeDisposable.)))

(defn command-for [name f]
  (let [disp (-> js/atom .-commands (.add "atom-text-editor"
                                          (str "chlorine:" name)
                                          f))]
    (.add @subscriptions disp)))

(defn save-focus! [elem]
  (when (-> @state :last-focus nil?)
    (swap! state assoc :last-focus
           (some-> js/atom .-workspace .getActiveTextEditor .-element)))
  (js/setTimeout #(.focus (.querySelector elem "input")) 100))

(defn refocus! []
  (when-let [elem (:last-focus @state)]
    (.focus elem)
    (swap! state dissoc :last-focus)))

(goog-define TESTS false)
