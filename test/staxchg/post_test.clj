(ns staxchg.post-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [staxchg.post :refer :all]))

(deftest code-info-test
  (defn md [{:keys [tags snippet fence-syntax id-key id-value]}]
    {"tags" tags
     "body_markdown"  (str "``` " fence-syntax "\r\n" snippet "\r\n```")
     id-key id-value})
  (defn code-snippet-strings [md]
    (->> md code-info (map (comp clojure.string/trim-newline :string))))
  (defn code-snippet-syntaxes [md]
    (->> md code-info (map :syntax)))
  (defn code-snippet-question-ids [md]
    (->> md code-info (map :question-id)))
  (defn code-snippet-answer-ids [md]
    (->> md code-info (map :answer-id)))

  (testing "string"
    (is (= (code-snippet-strings (md {:snippet "foo"}))
           ["foo"])))

  (testing "tag with no fence syntax"
    (is (= (code-snippet-syntaxes (md {:tags ["java"]}))
           ["java"])))

  (testing "tag with no fence syntax"
    (is (= (code-snippet-syntaxes (md {:tags ["java"] :fence-syntax "clojure"}))
           ["clojure"])))

  (testing "question id"
    (is (= (code-snippet-question-ids (md {:id-key "question_id" :id-value "12321"}))
           ["12321"])))

  (testing "answer id"
    (is (= (code-snippet-answer-ids (md {:id-key "answer_id" :id-value "12321"}))
           ["12321"]))))

