(ns chlorine.aux
  (:require [clojure.core.async :as async]
            [promesa.core :as p]
            [clojure.test :as t]))

(defmacro in-channel [ & forms]
  `(let [~'chan (cljs.core.async/promise-chan)]
     ~@forms
     ~'chan))

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
