#!./node_modules/.bin/lumo --classpath=scripts/src
(require-macros '[promise :refer [promise-> timeout]])
(require '["child_process" :as cp])
(require '["net" :as net])

(try
  (. (net/Socket.) (connect 3311))
  (catch js/Error e e))

(def lein (cp/exec "lein trampoline run -m shadow.cljs.devtools.cli --npm watch dev" #js {:cwd "./integration/fixture-app"}))

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
