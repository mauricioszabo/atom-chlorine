#!./node_modules/.bin/lumo --classpath=scripts/src
(require-macros '[promise :refer [promise-> timeout]])
(require '["child_process" :as cp])
(require '["net" :as net])

(doto (net/Socket.)
      (.on "error" #(println "ERROR ON CONNECTION"))
      (.connect 3311 (fn [a b] (prn [:a a b]))))

(def lein (cp/exec "./scripts/repl" #js {:cwd "./integration/fixture-app"}))

(defn wait-to-load [fun times resolve]
  (if (fun)
    (resolve true)
    (if (= times 10)
      (resolve false)
      (js/setTimeout #(wait-to-load fun (inc times) resolve) 1000))))

;(promise-> (timeout 100)
;           (fn [] 10)
;           prn
;           (fn [] (timeout 1000))
;           (fn [] 20)
;           prn)
(cljs.core/pr-str (try (require-macros '[promise :refer [promise-> timeout]])))
     ; res)))
(try
  (require-macros '[promise :refer [promise-> timeout]]))
