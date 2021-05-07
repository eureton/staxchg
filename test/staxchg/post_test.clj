(ns staxchg.post-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [staxchg.post :refer :all]))

(deftest code-info-test
  (defn md [{:keys [tags snippet fence-syntax question-id answer-id]}]
    (cond-> {"tags" tags
             "body_markdown"  (str "``` " fence-syntax "\r\n" snippet "\r\n```")}
      question-id (assoc "question_id" question-id)
      answer-id (assoc "answer_id" answer-id)))
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
    (is (= (code-snippet-question-ids (md {:question-id "12321"}))
           ["12321"])))

  (testing "answer id"
    (let [result (code-info (md {:question-id "12321" :answer-id "32123"}))]
      (is (and (= (map :answer-id result)
                  ["32123"])
               (= (map :question-id result)
                  ["12321"]))))))

