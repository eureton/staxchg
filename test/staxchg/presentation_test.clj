(ns staxchg.presentation-test
  (:require [clojure.test :refer :all]
            [staxchg.presentation :refer :all]))

(deftest selected-question-test
  (testing "pun nil"
    (is (nil? (selected-question nil))))
  (testing "index in range"
    (let [questions [:a :b :c]]
      (are [index result]
           (= (selected-question {:questions questions
                                  :selected-question-index index})
              result)
           0 :a
           1 :b
           2 :c)))
  (testing "index out of range => nil"
    (let [questions [:a :b :c]]
      (are [index]
           (nil? (selected-question {:questions questions
                                     :selected-question-index index}))
           -1 3))))

