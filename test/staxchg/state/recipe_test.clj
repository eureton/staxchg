(ns staxchg.state.recipe-test
  (:require [clojure.test :refer :all]
            [staxchg.state.recipe :refer :all]))

(deftest sanitize-syntax-test
  (testing "pun nil"
    (is (nil? (sanitize-syntax nil nil))))
  (testing "valid highlighter, invalid syntax => nil"
    (is (nil? (sanitize-syntax :skylighting "123qwerty321"))))
  (testing "invalid highlighter, valid syntax => nil"
    (is (nil? (sanitize-syntax :xyz "java"))))
  (testing "valid input, no translation => no change"
    (is (= (sanitize-syntax :skylighting "ruby")
           "ruby")))
  (testing "valid input, translation => translated"
    (is (= (sanitize-syntax :skylighting "c++")
           "cpp"))))

(deftest highlight-code-step-test
  (testing "pun nil"
    (is (nil? (highlight-code-step nil :skylighting))))

  (testing "standard usage"
    (is (= (highlight-code-step {:string "xyz"
                                 :syntax #{"ruby"}
                                 :question-id "qid"
                                 :answer-id "aid"} :skylighting)
           {:function :staxchg.io/run-skylighting!
            :params ["xyz" "ruby" "qid" "aid"]})))

  (testing "params"
    (def snippet {:string "xyz"
                  :syntax #{"ruby"}
                  :question-id "qid"
                  :answer-id "aid"})

    (are [index value]
         (= (get-in (highlight-code-step snippet :skylighting) [:params index])
            value)
         0 "xyz"
         1 "ruby"
         2 "qid"
         3 "aid"))

  (testing "function"
    (are [highlighter function]
         (= (:function (highlight-code-step {:string "x" :syntax #{"c"}}
                                            highlighter))
            function)
         :skylighting :staxchg.io/run-skylighting!
         :highlight.js :staxchg.io/run-highlight.js!))

  (testing "highlight.js, multiple syntaxes"
    (def snippet {:string "x" :syntax #{"c" "c++"}})

    (is (= (get-in (highlight-code-step snippet :highlight.js) [:params 1])
           #{"c" "c++"})))

  (testing "syntax translation"
    (defn snippet [syntax] {:string "x" :syntax #{syntax}})

    (testing "skylighting"
      (is (= (get-in (highlight-code-step (snippet "f#") :skylighting) [:params 1])
             "fsharp")))

    (testing "highlight.js"
      (is (= (get-in (highlight-code-step (snippet "avr") :highlight.js) [:params 1])
             #{"avrasm"})))))

