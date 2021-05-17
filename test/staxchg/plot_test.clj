(ns staxchg.plot-test
  (:require [clojure.test :refer :all]
            [staxchg.plot :refer :all]))

(deftest cluster-by-test
  (def i1 [\a [0 0] {:traits #{:x}}])
  (def i2 [\a [1 0] {:traits #{:x}}])
  (def i3 [\a [2 0] {:traits #{:y}}])
  (def i4 [\b [3 0] {:traits #{:y}}])
  (def i5 [\b [4 0] {:traits #{:x}}])
  (def i6 [\b [5 0] {:traits #{:x}}])
  (def plot [i1 i2 i3 i4 i5 i6])

  (testing "by traits"
    (is (= (cluster-by #(get-in % [2 :traits]) plot)
           [{:plot [i1 i2] :from 0 :to 2 :value #{:x}}
            {:plot [i3 i4] :from 2 :to 4 :value #{:y}}
            {:plot [i5 i6] :from 4 :to 6 :value #{:x}}])))

  (testing "by character"
    (is (= (cluster-by first plot)
           [{:plot [i1 i2 i3] :from 0 :to 3 :value \a}
            {:plot [i4 i5 i6] :from 3 :to 6 :value \b}]))))

(deftest cluster-by-trait-test
  (def x1 [\a [0 0] {:traits #{:x}}])
  (def x2 [\b [1 0] {:traits #{:x}}])
  (def y1 [\c [2 0] {:traits #{:y}}])
  (def y2 [\d [3 0] {:traits #{:y}}])
  (def x3 [\e [4 0] {:traits #{:x}}])
  (def x4 [\f [5 0] {:traits #{:x}}])
  (def plot [x1 x2 y1 y2 x3 x4])

  (testing "standard use"
    (are [trait result] (= (cluster-by-trait plot trait)
                           result)
         :x [{:plot [x1 x2] :from 0 :to 2}
             {:plot [x3 x4] :from 4 :to 6}]
         :y [{:plot [y1 y2] :from 2 :to 4}]))

  (testing "trait not found => returns empty"
    (is (empty? (cluster-by-trait nil :z))))

  (testing "nil plot => returns empty"
    (is (empty? (cluster-by-trait nil :x))))

  (testing "empty plot => returns empty"
    (is (empty? (cluster-by-trait [] :x)))))

(deftest text-test
  (testing "standard use"
    (is (= (text [[\a [0 0]]
                  [\b [1 0]]
                  [\c [0 1]]
                  [\d [0 2]]
                  [\e [1 2]]
                  [\f [0 3]]])
           "ab\r\nc\r\nde\r\nf"))))

