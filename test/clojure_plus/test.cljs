(ns clojure-plus.test)

(defn testing [txt & functions]
  (doseq [f functions]
    (let [res (f)]
      (when-not res
        (throw (ex-info (str "Assertion failed in test: '" txt "'!")
                        {:result res
                         :failing txt})))))
  true)
