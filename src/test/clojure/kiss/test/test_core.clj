(ns kiss.test.test-core
  (:use clojure.test)
  (:use kiss.core)
  (:use [mikera.cljutils error]) 
  (:import [kiss.lang Expression Environment] ))

(deftest environment-tests
  (let [e (empty-environment)]
    (is (empty? (seq e)))
    (let [e (assoc e 'foo 1)]
      (is (== 1 (e 'foo)))))
  (let [env (environment {foo 1, 
                         bar 2,
                         baz (clojure.core/+ foo bar)})]
    (is (== 3 ('baz env)))))

(deftest test-analyser
  (let [^Environment e (environment)
        ^Expression ex (analyse 1)]
    (is (instance? Expression ex))
    (is (== 1 (.eval ex e)))))

(deftest test-lookup
  (let [^Environment e (environment)
        e (assoc e 'foo 10) 
        ^Expression ex (analyse 'foo)]
    (is (instance? Expression ex))
    (is (== 10 (.eval ex e)))
    (is (== 10 (kiss e foo)))
    (is (== 17 (kiss e (let [foo 17] foo))))))

(deftest test-errors
  (is (error? (kiss (1))))
  (is (error? (kiss (1 2))))
  (is (error? (kiss (clojure.core/+ 1 "foo"))))) 

(deftest test-constants
  (is (= nil (kiss nil)))
  (is (= 1 (kiss 1))))

(deftest test-let
  (is (== 13 (kiss (let [a 13] a))))
  (is (== 13 (kiss (let [a 13 b a] b))))
  (is (error? (kiss (let [a 13 b] b)))))

(deftest test-if 
  (is (== 4 (kiss (if true 4 5))))
  (is (== 5 (kiss (if false 9 5))))
  (is (== 5 (kiss (if nil 9 5))))
  (is (= "foo" (kiss (if false 9 "foo")))))

(deftest test-def
  (let [e (kisse (def kiss.core/a 1))]
    (is (instance? Environment e))
    (is (= 1 (e 'kiss.core/a))))
  (let [e (kisse (do (def kiss.core/a 1) (def kiss.core/b 2)))]
    (is (instance? Environment e))
    (is (= 1 (e 'kiss.core/a)))
    (is (= 2 (e 'kiss.core/b))))
  (let [e (kisse (let [foo 2] (def kiss.core/a (clojure.core/+ foo 1))))]
    (is (instance? Environment e))
    (is (= 3 (e 'kiss.core/a)))))

(deftest test-merge
  (let [e1 (kisse (def a 1))
        e2 (kisse (def b 2))
        e3 (kmerge e1 e2)]
    (is (= 1 (e3 'a)))
    (is (= 2 (e3 'b)))))

(deftest test-vectors
  (is (= [] (kiss [])))
  (is (= [1] (kiss [1])))
  (is (= [3 5 nil] (kiss [(clojure.core/+ 1 2) (clojure.core/inc 4) nil]))))

(deftest test-loop-recur
  ;; (is (error? (kiss (recur 2))))  ;; TODO: figure out wha this should do?
  )

(deftest test-return
  (is (= 3 (kiss ((fn [x] (do (return (clojure.core/inc x)) (clojure.core/dex x))) 2))))
  ;; (is (error? (kiss (return 3)))) ; TODO figure out what this should be?
  )

(deftest test-maps
  (is (= {} (kiss {})))
  (is (= {5 3} (kiss {(clojure.core/+ 2 3) (clojure.core/+ 1 2)}))))

(deftest test-clojure-fn
  (is (== 3 (kiss (clojure.core/+ 1 2))))
  (is (nil? (kiss ({} 2)))))

(deftest test-unbound-error
  (is (== 6 (kiss (do (def b a) 6))))
  (is (error? (kiss (do (def b a) b))))
  ;; (is (== 1 (kiss (do (def b a) (def a 1) b))))
  (is (== 1 (kiss (do (def a 1) a))))) 

(deftest test-lambda
  (is (== 3 (kiss ((fn [x] 3) 2)))))
