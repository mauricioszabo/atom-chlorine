(ns clojure-plus.refactor-nrepl-test
  (:require [clojure.test :refer-macros [deftest is testing run-tests async]]
            [clojure-plus.test-helpers :as th :refer-macros [with-text-editor create-text-editor]]
            [clojure-plus.refactor-nrepl :as refactor]))

(deftest ns-rewrite
  (testing "rewrites NS"
      (with-text-editor editor
        (th/set-text editor "(ns something
  (:require
            [clojure.walk :as walk]))
:foo")
        (refactor/rewrite-ns editor "BAR")
        (is (= "BAR\n:foo") (th/text editor)))))

(deftest do-nothing-if-no-require
    (async done
      (create-text-editor editor
        (th/set-text editor "(ns foo)")
        (-> (refactor/organize-ns editor)
            (.then (fn []
                     (is (= "(ns foo)"
                            (th/text editor)))
                     (.destroy editor)
                     (done)))))))

(deftest format-ns
  (testing "formatting a simple require"
    (is (= "(ns something\n  (:require [foo.bar :as bar]))"
           (refactor/format-ns '(ns something (:require [foo.bar :as bar]))))))

  (testing "formatting multiple requires"
    (is (= "(ns something
  (:require [foo.bar :as bar]
            [foo.baz :as baz]))"
           (refactor/format-ns '(ns something (:require [foo.bar :as bar]
                                                        [foo.baz :as baz]))))))

  (testing "formatting require with import"
    (is (= "(ns something
  (:require [foo.bar :as bar]
            [foo.baz :as baz])
  (:import [foo.bar Baz]))"
           (refactor/format-ns '(ns something
                                  (:require [foo.bar :as bar]
                                            [foo.baz :as baz])
                                  (:import [foo.bar Baz]))))))

  (testing "formatting require with gen-class"
    (is (= "(ns something
  (:require [foo.bar :as bar]
            [foo.baz :as baz])
  (:gen-class))"
           (refactor/format-ns '(ns something
                                  (:require [foo.bar :as bar]
                                            [foo.baz :as baz])
                                  (:gen-class)))))))

(deftest clean-ns
  (async done
    (create-text-editor editor
                           (th/set-text editor "(ns something
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.walk :as walk]))
(str/join (walk/prewalk [1 2 3] str))")
                           (-> (refactor/organize-ns editor)
                               (.then
                                (fn []
                                  (is (= "(ns something
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))
(str/join (walk/prewalk [1 2 3] str))"
                                         (th/text editor)))
                                  (.destroy editor)
                                  (done)))))))

(deftest resolve-missing
  (with-text-editor editor
    (testing "adds a new require at namespace"
      (th/set-text editor "(ns foo\n      )")
      (refactor/add-require editor "[clojure.string :as str]")
      (is (= "(ns foo\n  (:require [clojure.string :as str]))"
             (th/text editor))))

    (testing "adds a new dependency at namespace"
      (refactor/add-require editor "[clojure.set :as set]")
      (is (= "(ns foo
  (:require [clojure.set :as set]
            [clojure.string :as str]))"
             (th/text editor))))))
