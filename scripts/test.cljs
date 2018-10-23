#!./node_modules/.bin/lumo --classpath=scripts/src
(require-macros '[promise :refer [promise-> timeout]])
(require '["child_process" :as cp])

(promise-> (timeout 100)
           (fn [] 10)
           prn
           (fn [] (timeout 1000))
           (fn [] 20)
           prn)
