(ns clojure-plus.ui.select-view)

(def SelectListView (.-SelectListView (js/require "atom-space-pen-views")))

(defn select-view [items]
  (let [view-for-item (fn [item] (str "<li>" (.-label item) "</li>"))
        select-list (doto (SelectListView.)
                          (.addClass "overlay from-top")
                          (aset "viewForItem" view-for-item)
                          (.setItems (clj->js items)))
        panel (-> js/atom .-workspace (.addModalPanel #js {:item select-list}))]

    (doto select-list
          (aset "cancelled" #(.destroy panel))
          (aset "confirmed" (fn [item] (.run item) (.cancel select-list))))
    (.show panel)
    (.focusFilterEditor select-list)))
