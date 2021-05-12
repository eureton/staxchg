(ns staxchg.code-test
  (:require [clojure.test :refer :all]
            [staxchg.code :refer :all]))

(deftest expand-tabs-test
  (testing "pun nil"
    (is (nil? (expand-tabs nil "java"))))

  (testing "java"
    (are [in out] (= (expand-tabs in "java")
                     out)
         "xyz\t"    "xyz    "
         "\txyz"    "    xyz"
         "abc\txyz" "abc    xyz"))

  (testing "ruby"
    (are [in out] (= (expand-tabs in "ruby")
                     out)
         "xyz\t"    "xyz  "
         "\txyz"    "  xyz"
         "abc\txyz" "abc  xyz"))

  (testing "unknown syntax"
    (is (= (expand-tabs "abc\txyz" "1234")
           "abc    xyz")))

  (testing "nil syntax"
    (is (= (expand-tabs "abc\txyz" nil)
           "abc    xyz"))))

