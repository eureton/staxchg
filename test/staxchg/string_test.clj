(ns staxchg.string-test
  (:require [clojure.test :refer :all]
            [staxchg.string :refer :all]))

(deftest bite-test
  (testing "ready fit"
    (testing "one word"
      (are [policy] (= (bite 3 "abc" policy) ["abc"])
           :truncate
           :float))
    (testing "multiple words"
      (are [policy] (= (bite 11 "abc 123 xyz" policy) ["abc 123 xyz"])
           :truncate
           :float)))
  (testing "break"
    (are [policy] (= (bite 5 "abc xyz" policy) ["abc" "xyz"])
         :truncate
         :float))
  (testing "preserves whitespace before break"
    (are [width string split] (= (bite width string :float) split)
         4 "abc xyz"   ["abc"   "xyz"]
         5 "abc  xyz"  ["abc "  "xyz"]
         6 "abc   xyz" ["abc  " "xyz"]))
  (testing "preserves trailing whitespace"
    (are [width string split] (= (bite width string :float) split)
         3 "abc xyz"   ["abc" "xyz"  ]
         4 "abc xyz "  ["abc" "xyz " ]
         5 "abc xyz  " ["abc" "xyz  "]))
  (testing "no fit"
    (testing "with truncation"
      (is (= (bite 3 "abcd" :truncate) ["ab…" nil])))
    (testing "without truncation"
      (is (= (bite 3 "abcd" :float) ["" "abcd"])))))

(deftest pack-test
  (testing "minimal"
    (is (= (pack 1 7 "abc xyz") ["abc" "xyz"])))
  (testing "x affects first line only"
    (is (= (pack 1 7 "abc xyz klm") ["abc" "xyz klm"])))
  (testing "truncation (word fits below)"
    (is (= (pack 1 4 "abcd") ["" "abcd"])))
  (testing "truncation (word doesn't fit below)"
    (is (= (pack 1 3 "abcd") ["" "ab…"])))
  (testing "x equal to width"
    (is (= (pack 3 3 "abc") ["" "abc"])))
  (testing "x greater than width"
    (is (= (pack 4 3 "abc") ["" "abc"])))
  (testing "trim left"
    (is (= (pack 7 7 " abc def") ["" "abc def"])))
  (testing "multiple lines"
    (testing "no truncation"
      (is (= (pack 7 10 "abc 1234 xyz 5678 def 90123456 qxy")
             ["abc" "1234 xyz" "5678 def" "90123456" "qxy"])))
    (testing "truncation"
      (is (= (pack 7 10 "abc 1234 xyz 567890ABCDEF") ["abc" "1234 xyz" "567890ABC…"]))))
  (testing "leading spaces"
    (is (= (pack 0 100 "  return x;") ["  return x;"]))))

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

