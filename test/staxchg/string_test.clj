(ns staxchg.string-test
  (:require [clojure.test :refer :all]
            [staxchg.string :refer :all]))

(deftest pack-test
  (testing "ready fit"
    (is (= (pack 3 "abc") ["abc"])))
  (testing "break"
    (is (= (pack 5 "abc xyz") ["abc" "xyz"])))
  (testing "preserves whitespace before break"
    (are [width string split] (= (pack width string) split)
         4 "abc xyz"   ["abc"   "xyz"]
         5 "abc  xyz"  ["abc "  "xyz"]
         6 "abc   xyz" ["abc  " "xyz"]))
  (testing "preserves trailing whitespace"
    (are [width string split] (= (pack width string) split)
         3 "abc xyz"   ["abc" "xyz"  ]
         4 "abc xyz "  ["abc" "xyz " ]
         5 "abc xyz  " ["abc" "xyz  "]))
  (testing "x"
    (is (= (pack 1 7 "abc xyz") ["abc" "xyz"])))
  (testing "x affects first line only"
    (is (= (pack 1 7 "abc xyz klm") ["abc" "xyz klm"]))))

