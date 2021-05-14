(ns staxchg.hilite-test
  (:require [clojure.test :refer :all]
            [staxchg.hilite :refer :all]))

(deftest annotate-test
  (testing "HTML entities outside tags"
    (def plot [[\t []]
               [\p []]
               [\l []]
               [\space []]
               [\< []]
               [\c []]
               [\l []]
               [\a []]
               [\s []]
               [\s []]
               [\space []]
               [\T []]
               [\> []]
               [\{ []]
               [\} []]])
    (def html "tpl &lt;<span class=\"kw\">class</span> T&gt;{}")

    (is (= (annotate plot html)
           [[\t []]
            [\p []]
            [\l []]
            [\space []]
            [\< []]
            [\c [] {:traits #{:hilite-keyword}}]
            [\l [] {:traits #{:hilite-keyword}}]
            [\a [] {:traits #{:hilite-keyword}}]
            [\s [] {:traits #{:hilite-keyword}}]
            [\s [] {:traits #{:hilite-keyword}}]
            [\space []]
            [\T []]
            [\> []]
            [\{ []]
            [\} []]])))

  (testing "HTML entities inside tags"
    (def plot [[\( []]
               [\> []]
               [\space []]
               [\x []]
               [\space []]
               [\y []]
               [\) []]])
    (def html "(<span class=\"op\">&gt;</span> <span class=\"va\">x</span> <span class=\"va\">y</span>)")

    (is (= (annotate plot html)
           [[\( []]
            [\> [] {:traits #{:hilite-operator}}]
            [\space []]
            [\x [] {:traits #{:hilite-variable}}]
            [\space []]
            [\y [] {:traits #{:hilite-variable}}]
            [\) []]]))))

