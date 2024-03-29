(ns staxchg.markdown-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [staxchg.markdown :refer :all]))

(defn fenced-block [snippet syntax] (->> [(str "``` " syntax)
                                          snippet
                                          "```"]
                                         (clojure.string/join "\r\n")))
(defn indented-block [snippet]
  (->> snippet
       clojure.string/split-lines
       (map #(str  "    " %))
       (clojure.string/join "\r\n")))
(defn join-lines [& ls]
  (clojure.string/join "\r\n" ls))

(deftest plot-test
  (defn cs [s opts]
    (->> (plot s opts) (map first)))
  (defn xys [s opts]
    (->> (plot s opts) (map second)))
  (defn xs [s opts]
    (->> (xys s opts) (map first)))
  (defn ys [s opts]
    (->> (xys s opts) (map second)))

  (testing "defaults"
    (is (= (plot "Hello world!" {:width 100})
           [[\H     [ 0 0]]
            [\e     [ 1 0]]
            [\l     [ 2 0]]
            [\l     [ 3 0]]
            [\o     [ 4 0]]
            [\space [ 5 0]]
            [\w     [ 6 0]]
            [\o     [ 7 0]]
            [\r     [ 8 0]]
            [\l     [ 9 0]]
            [\d     [10 0]]
            [\!     [11 0]]])))
  (testing ":left"
    (is (= (xs "123" {:width 100 :left 10}) [10 11 12])))
  (testing ":top"
    (is (= (ys "123" {:width 100 :top 20}) [20 20 20])))
  (testing ":x"
    (is (= (xs "123" {:width 100 :x 5}) [5 6 7])))
  (testing ":x and :left"
    (is (= (xs "123" {:width 100 :x 5 :left 10}) [15 16 17])))
  (testing ":y"
    (is (= (ys "123" {:width 100 :y 7}) [7 7 7])))
  (testing ":y and :top"
    (is (= (ys "123" {:width 100 :y 7 :top 20}) [27 27 27])))
  (testing "soft line break"
    (is (= (plot "12\r\n34" {:width 100})
           [[\1     [0 0]]
            [\2     [1 0]]
            [\space [2 0]]
            [\space [3 0]]
            [\3     [4 0]]
            [\4     [5 0]]])))
  (testing "hard line break"
    (is (= (plot "12  \r\n34" {:width 100})
           [[\1 [0 0]]
            [\2 [1 0]]
            [\3 [0 1]]
            [\4 [1 1]]])))
  (testing "thematic line break"
    (is (= (plot "----" {:width 10})
           [[\- [0 0] {:traits #{:horz}}]
            [\- [1 0] {:traits #{:horz}}]
            [\- [2 0] {:traits #{:horz}}]
            [\- [3 0] {:traits #{:horz}}]
            [\- [4 0] {:traits #{:horz}}]
            [\- [5 0] {:traits #{:horz}}]
            [\- [6 0] {:traits #{:horz}}]
            [\- [7 0] {:traits #{:horz}}]
            [\- [8 0] {:traits #{:horz}}]
            [\- [9 0] {:traits #{:horz}}]])))
  (testing "paragraph"
    (is (= (plot "12\r\n\r\n34" {:width 100})
           [[\1 [0 0]]
            [\2 [1 0]]
            [\3 [0 2]]
            [\4 [1 2]]])))
  (testing "bullet list: characters"
    (are [marker]
         (let [s (format "%1$s 1\r\n%1$s 2\r\n%1$s 3" marker)]
           (= (cs s {:width 100}) [\+ \space \1
                                   \+ \space \2
                                   \+ \space \3]))
         \- \* \+))
  (testing "bullet list: coordinates"
    (are [marker]
         (let [s (format "%1$s 1\r\n%1$s 2\r\n%1$s 3" marker)]
           (= (xys s {:width 100}) [[0 0] [1 0] [2 0]
                                    [0 1] [1 1] [2 1]
                                    [0 2] [1 2] [2 2]]))
         \- \* \+))
  (testing "bullet list: bottom margin"
    (is (= (ys "- 1\r\n\r\n23" {:width 100})
           [0 0 0 2 2])))
  (testing "bullet list: reflow"
    (let [s "- abc def\r\n- 123 456"
          opts {:width 8}]
      (is (and (= (cs s opts)
                  [\+ \space \a \b \c
                             \d \e \f
                   \+ \space \1 \2 \3
                             \4 \5 \6])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0]
                               [2 1] [3 1] [4 1]
                   [0 2] [1 2] [2 2] [3 2] [4 2]
                               [2 3] [3 3] [4 3]])))))
  (testing "bullet list: nesting (one level)"
    (let [s "- 1\r\n    - 1a\r\n    - 1b\r\n- 2"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [                            \+ \space \1
                                 \space \space \+ \space \1 \a
                                 \space \space \+ \space \1 \b
                                               \+ \space \2      ])
               (= (xys s opts)
                  [                        [0 0] [1 0] [2 0]
                               [0 1] [1 1] [2 1] [3 1] [4 1] [5 1]
                               [0 2] [1 2] [2 2] [3 2] [4 2] [5 2]
                                           [0 3] [1 3] [2 3]      ])))))
  (testing "bullet list: nesting (two levels)"
    (let [s "- 1\r\n    - 1a\r\n        - 1aA\r\n        - 1aB\r\n    - 1b\r\n- 2"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [                            \+ \space \1
                                 \space \space \+ \space \1 \a
                   \space \space \space \space \+ \space \1 \a \A
                   \space \space \space \space \+ \space \1 \a \B
                                 \space \space \+ \space \1 \b
                                               \+ \space \2      ])
               (= (xys s opts)
                  [                        [0 0] [1 0] [2 0]
                               [0 1] [1 1] [2 1] [3 1] [4 1] [5 1]
                   [0 2] [1 2] [2 2] [3 2] [4 2] [5 2] [6 2] [7 2] [8 2]
                   [0 3] [1 3] [2 3] [3 3] [4 3] [5 3] [6 3] [7 3] [8 3]
                               [0 4] [1 4] [2 4] [3 4] [4 4] [5 4]
                                           [0 5] [1 5] [2 5]            ])))))
  (testing "fenced code block: coordinates"
    (is (= (xys "```\r\n12\r\n34\r\n```" {:width 100})
           [[0 0] [1 0]
            [0 1] [1 1]])))
  (testing "fenced code block: bottom margin"
    (is (= (xys "```\r\n12\r\n```\r\n34" {:width 100})
           [[0 0] [1 0]
            [0 2] [1 2]])))
  (testing "indented code block"
    (let [s "    12\r\n    34"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\1 \2
                   \3 \4])
               (= (xys s opts)
                  [[0 0] [1 0]
                   [0 1] [1 1]])))))
  (testing "indented code block: bottom margin"
    (is (= (xys "    12\r\n34" {:width 100})
           [[0 0] [1 0]
            [0 2] [1 2]])))
  (testing "ordered list: characters"
    (is (= (cs "1. 1\r\n1. 2\r\n1. 3" {:width 100}) [\1 \. \space \1
                                                     \2 \. \space \2
                                                     \3 \. \space \3])))
  (testing "ordered list: coordinates"
    (is (= (xys "1. 1\r\n1. 2\r\n1. 3" {:width 100}) [[0 0] [1 0] [2 0] [3 0]
                                                      [0 1] [1 1] [2 1] [3 1]
                                                      [0 2] [1 2] [2 2] [3 2]])))
  (testing "ordered list: bottom margin"
    (is (= (ys "1. 1\r\n\r\n23" {:width 100})
           [0 0 0 0 2 2])))
  (testing "ordered list: reflow"
    (let [s "1. abc def\r\n1. 123 456"
          opts {:width 8}]
      (is (and (= (cs s opts)
                  [\1 \. \space \a \b \c
                                \d \e \f
                   \2 \. \space \1 \2 \3
                                \4 \5 \6])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0] [5 0]
                                     [3 1] [4 1] [5 1]
                   [0 2] [1 2] [2 2] [3 2] [4 2] [5 2]
                                     [3 3] [4 3] [5 3]])))))
  (testing "ordered list: nesting (one level)"
    (let [s "1. 1\r\n    1. 1a\r\n    1. 1b\r\n1. 2"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [                            \1 \. \space \1
                                 \space \space \a \. \space \1 \a
                                 \space \space \b \. \space \1 \b
                                               \2 \. \space \2   ])
               (= (xys s opts)
                  [                        [0 0] [1 0] [2 0] [3 0]
                               [0 1] [1 1] [2 1] [3 1] [4 1] [5 1] [6 1]
                               [0 2] [1 2] [2 2] [3 2] [4 2] [5 2] [6 2]
                                           [0 3] [1 3] [2 3] [3 3]      ])))))
  (testing "ordered list: nesting (two levels)"
    (let [s "1. 1\r\n    1. 1a\r\n        1. 1aA\r\n        1. 1aB\r\n    1. 1b\r\n1. 2"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [                            \1 \. \space \1
                                 \space \space \a \. \space \1 \a
                   \space \space \space \space \1 \. \space \1 \a \A
                   \space \space \space \space \2 \. \space \1 \a \B
                                 \space \space \b \. \space \1 \b
                                               \2 \. \space \2      ])
               (= (xys s opts)
                  [                        [0 0] [1 0] [2 0] [3 0]
                               [0 1] [1 1] [2 1] [3 1] [4 1] [5 1] [6 1]
                   [0 2] [1 2] [2 2] [3 2] [4 2] [5 2] [6 2] [7 2] [8 2] [9 2]
                   [0 3] [1 3] [2 3] [3 3] [4 3] [5 3] [6 3] [7 3] [8 3] [9 3]
                               [0 4] [1 4] [2 4] [3 4] [4 4] [5 4] [6 4]
                                           [0 5] [1 5] [2 5] [3 5]            ])))))
  (testing "ordered list: decor alignment"
    (let [s "1. a\r\n1. b\r\n1. c\r\n1. d\r\n1. e\r\n1. f\r\n1. g\r\n1. h\r\n1. i\r\n1. j"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\space \1 \. \space \a
                   \space \2 \. \space \b
                   \space \3 \. \space \c
                   \space \4 \. \space \d
                   \space \5 \. \space \e
                   \space \6 \. \space \f
                   \space \7 \. \space \g
                   \space \8 \. \space \h
                   \space \9 \. \space \i
                       \1 \0 \. \space \j])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0]
                   [0 1] [1 1] [2 1] [3 1] [4 1]
                   [0 2] [1 2] [2 2] [3 2] [4 2]
                   [0 3] [1 3] [2 3] [3 3] [4 3]
                   [0 4] [1 4] [2 4] [3 4] [4 4]
                   [0 5] [1 5] [2 5] [3 5] [4 5]
                   [0 6] [1 6] [2 6] [3 6] [4 6]
                   [0 7] [1 7] [2 7] [3 7] [4 7]
                   [0 8] [1 8] [2 8] [3 8] [4 8]
                   [0 9] [1 9] [2 9] [3 9] [4 9]])))))
  (testing "link"
    (let [s "[ab](cd)"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\a \b \space \c \d])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0]])))))
  (testing "link: treat as inline"
    (let [s "[ab](cd) xy"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\a \b \space \c \d \space \x \y])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0] [5 0] [6 0] [7 0]])))))
  (testing "block quote"
    (let [s "> 12  \r\n34"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\>     \space \1 \2
                                 \3 \4])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0]
                               [2 1] [3 1]])))))
  (testing "block quote: bottom margin"
    (is (= (xys "> 12\r\n\r\n34" {:width 100})
           [[0 0] [1 0] [2 0] [3 0]
            [0 2] [1 2]])))
  (testing "link ref"
    (let [s "[abc]"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\[ \a \b \c \]])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0]])))))
  (testing "link ref: treat as inline"
    (let [s "[abc] xy"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\[ \a \b \c \] \space \x \y])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [3 0] [4 0] [5 0] [6 0] [7 0]])))))
  (testing "heading"
    (are [s] (= (cs s {:width 100})
                [\a \b \c])
         "abc\r\n---"
         "# abc"))
  (testing "heading: bottom margin"
    (let [s "abc\r\n---\r\n\r\nxyz"
          opts {:width 100}]
      (is (and (= (cs s opts)
                  [\a \b \c \x \y \z])
               (= (xys s opts)
                  [[0 0] [1 0] [2 0] [0 2] [1 2] [2 2]])))))
  (testing "html comment block"
    (is (= (cs "<!-- xyz -->" {:width 100})
           [\< \! \- \- \space \x \y \z \space \- \- \>])))
  (testing "html comment block: bottom margin"
    (is (= (drop 10 (xys "<!-- x -->\r\n\r\nyz" {:width 100}))
           [[0 2] [1 2]])))

  (testing "tab expansion"
    (testing "indented code block"
      (def md (indented-block (join-lines "_" "\ta" "\t\tb")))
      (defn opts [syntax] {:width 100 :syntax syntax})

      (testing "java: characters"
        (is (= (cs md (opts "java"))
               [\_
                \space \space \space \space \a
                \space \space \space \space \space \space \space \space \b])))

      (testing "java: coordinates"
        (is (= (xys md (opts "java"))
               [[0 0]
                [0 1] [1 1] [2 1] [3 1] [4 1]
                [0 2] [1 2] [2 2] [3 2] [4 2] [5 2] [6 2] [7 2] [8 2]])))

      (testing "ruby: characters"
        (is (= (cs md (opts "ruby"))
               [\_
                \space \space \a
                \space \space \space \space \b])))

      (testing "ruby: coordinates"
        (is (= (xys md (opts "ruby"))
               [[0 0]
                [0 1] [1 1] [2 1]
                [0 2] [1 2] [2 2] [3 2] [4 2]]))))

    (testing "fenced code block"
      (def md (partial fenced-block (join-lines "\ta" "b\t" "\t\tc")))
      (defn opts [syntax] {:width 100 :syntax syntax})
      (def cs-by-4 [\space \space \space \space     \a
                        \b \space \space \space \space
                    \space \space \space \space \space \space \space \space \c])
      (def xys-by-4 [[0 0] [1 0] [2 0] [3 0] [4 0]
                     [0 1] [1 1] [2 1] [3 1] [4 1]
                     [0 2] [1 2] [2 2] [3 2] [4 2] [5 2] [6 2] [7 2] [8 2]])
      (def cs-by-2 [\space \space     \a
                        \b \space \space
                    \space \space \space \space \c])
      (def xys-by-2 [[0 0] [1 0] [2 0]
                     [0 1] [1 1] [2 1]
                     [0 2] [1 2] [2 2] [3 2] [4 2]])

      (testing "java tag, no fence info: characters"
        (is (= (cs (md nil) (opts "java"))
               cs-by-4)))

      (testing "java tag, no fence info: coordinates"
        (is (= (xys (md nil) (opts "java"))
               xys-by-4)))

      (testing "java tag, ruby fence info: characters"
        (is (= (cs (md "ruby") (opts "java"))
               cs-by-2)))

      (testing "java tag, ruby fence info: coordinates"
        (is (= (xys (md "ruby") (opts "java"))
               xys-by-2)))

      (testing "ruby tag, no fence info: characters"
        (is (= (cs (md nil) (opts "ruby"))
               cs-by-2)))

      (testing "ruby tag, no fence info: coordinates"
        (is (= (xys (md nil) (opts "ruby"))
               xys-by-2)))

      (testing "ruby tag, java fence info: characters"
        (is (= (cs (md "java") (opts "ruby"))
               cs-by-4)))
      
      (testing "ruby tag, java fence info: coordinates"
        (is (= (xys (md "java") (opts "ruby"))
               xys-by-4))))))

(deftest code-info-test
  (defn rand-snippet [snippet-line-count]
    (->> snippet-line-count
         (gen/sample (gen/not-empty gen/string-alphanumeric))
         (clojure.string/join "\r\n")))
  (def snippet-gen (gen/fmap rand-snippet (gen/choose 5 15)))
  (def string-gen (gen/not-empty gen/string-alphanumeric))
  (def snippet-count (+ 2 (rand-int 6)))
  (def snippets (gen/generate (gen/vector snippet-gen snippet-count)))

  (defn code-snippet-strings [md]
    (->> md code-info (map :string)))
  (defn code-snippet-syntaxes [md]
    (->> md code-info (map :syntax)))

  (testing "fenced code blocks"
    (def syntax-gen string-gen)

    (def syntaxes (gen/generate (gen/vector syntax-gen snippet-count)))
    (def md (->> (map fenced-block snippets syntaxes)
                 (interleave (gen/generate (gen/vector string-gen snippet-count)))
                 (clojure.string/join "\r\n")))

    (testing "string extraction"
      (is (= (code-snippet-strings md)
             (map #(str % "\r\n") snippets))))

    (testing "preserves leading empty lines"
      (is (= (code-snippet-strings (fenced-block "\r\n\r\nfoo" "bar"))
             ["\r\n\r\nfoo\r\n"])))

    (testing "syntax extraction"
      (is (= (code-snippet-syntaxes md)
             syntaxes))))

  (testing "indented code blocks"
    (def md (->> (map indented-block snippets)
                 (interleave (gen/generate (gen/vector string-gen snippet-count)))
                 (clojure.string/join "\r\n\r\n")))

    (testing "string extraction"
      (is (= (code-snippet-strings md)
             (map #(str % "\r\n") snippets))))

    (testing "syntax extraction"
      (is (= (code-snippet-syntaxes md)
             (repeat snippet-count nil))))))

