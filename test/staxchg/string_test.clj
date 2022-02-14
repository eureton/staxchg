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
  (testing "truncation"
    (is (= (pack 3 "abcd") ["ab…"])))
  (testing "x"
    (testing "minimal"
      (is (= (pack 1 7 "abc xyz") ["abc" "xyz"])))
    (testing "x affects first line only"
      (is (= (pack 1 7 "abc xyz klm") ["abc" "xyz klm"])))
    (testing "x: truncation (word fits below)"
      (is (= (pack 1 4 "abcd") ["" "abcd"])))
    (testing "x: truncation (word doesn't fit below)"
      (is (= (pack 1 3 "abcd") ["" "ab…"])))
    (testing "x equal to width"
      (is (= (pack 3 3 "abc") ["" "abc"])))
    (testing "x greater than width"
      (is (= (pack 4 3 "abc") ["" "abc"])))
    (testing "x: trim left"
      (is (= (pack 7 7 " abc def") ["" "abc def"])))))

(deftest trim-leading-indent-test
  (testing "pun nil"
    (nil? (trim-leading-indent nil)))

  (testing "spaces: less than 4"
    (are [s] (= (trim-leading-indent s) s)
         "xyz"
         " xyz"
         "  xyz"
         "   xyz"))

  (testing "spaces: equal to or more than 4"
    (are [in out] (= (trim-leading-indent in) out)
         "    xyz"  "xyz"
         "     xyz" " xyz"))

  (testing "tabs"
    (are [in out] (= (trim-leading-indent in) out)
         "\txyz"   "xyz"
         "\t\txyz" "\txyz")))

(deftest append-missing-crlf-test
  (testing "pun nil"
    (nil? (append-missing-crlf nil)))

  (testing "missing"
    (are [s] (= (append-missing-crlf s)
                (str s "\r\n"))
         ""
         "xyz"
         "xyz\r"
         "xyz\n"
         "xy\r\nz"))

  (testing "not missing"
    (are [s] (= (append-missing-crlf s) s)
         "xyz\r\n"
         "\r\n")))

