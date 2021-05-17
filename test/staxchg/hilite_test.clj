(ns staxchg.hilite-test
  (:require [clojure.test :refer :all]
            [staxchg.hilite :refer :all]))

(deftest info-test
  (testing "preserves whitespace"
    (def code "//    xyz")

    (testing "inside tag"
      (def html (str "<span class=\"co\">" code "</span>"))

      (is (->> (info html)
               (filter (comp #{html} :html))
               some?)))

    (testing "inside nested tag"
      (def html
        (str "<span class=\"cv\"><span class=\"co\">" code "</span></span>"))

      (is (->> (info html)
               (filter (comp #{html} :html))
               some?)))))

(deftest truncate-test
  (defn hilite [lines] (parse {:exit 0
                               :out (str "<code class=\"sourceCode\">"
                                         (clojure.string/join "\n" lines)
                                         "</code>")}))

  (testing "standard use"
    (def l1 "(<span class=\"kw\">ns</span> foo.bar)")
    (def l2 "")
    (def l3 "<span class=\"co\">; foo</span>")
    (def l4 "(<span class=\"kw\">def</span> x 1)")

    (are [in n out] (= (->> in hilite (truncate n) :html)
                       (->> out hilite :html))
         [l1 l2 l3 l4] 0 [l1 l2 l3 l4]
         [l1 l2 l3 l4] 1 [l2 l3 l4]
         [l1 l2 l3 l4] 2 [l3 l4]
         [l1 l2 l3 l4] 3 [l4]
         [l1 l2 l3 l4] 4 []))

  (testing "index out of range"
    (def lines ["abc" "xyz"])

    (are [n out] (= (->> lines hilite (truncate n) :html)
                    (->> out hilite :html))
         -1 lines
          3 []
          4 [])))

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

