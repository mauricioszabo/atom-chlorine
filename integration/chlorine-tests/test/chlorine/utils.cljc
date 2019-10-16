(ns chlorine.utils
  (:require [promesa.core :as p]
            [clojure.test :as t]))

(def promises (atom []))

(defmacro async [ & body]
  `(clojure.test/async done#
                       (reset! promises [])
                       ~@body
                       (.. js/Promise
                           (~'all (~'clj->js @promises))
                           (~'then done#))))

(defmacro async-testing [description & body]
  `(t/testing ~description
    (swap! promises conj (p/alet [_# (last @promises)]
                           ~@body))))
