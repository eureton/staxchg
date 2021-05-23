(ns staxchg.post-test
  (:require [clojure.test :refer :all]
            [staxchg.post :refer :all]
            [clojure.string :as string]))

(deftest code-info-test
  (defn fence-blk
    ([code syntax]
     (str "``` " syntax "\r\n" code "\r\n```"))
    ([code]
     (fence-blk code "")))
  (defn post [{:keys [tags snippet fence-syntax question-id answer-id]}]
    (cond-> {"tags" tags
             "body_markdown" (fence-blk snippet fence-syntax)}
      question-id (assoc "question_id" question-id)
      answer-id (assoc "answer_id" answer-id)))
  (defn code-snippet-strings [post]
    (->> post code-info (map (comp string/trim-newline :string))))
  (defn code-snippet-syntaxes [post]
    (->> post code-info (map :syntax)))
  (defn code-snippet-question-ids [post]
    (->> post code-info (map :question-id)))
  (defn code-snippet-answer-ids [post]
    (->> post code-info (map :answer-id)))

  (testing "string"
    (is (= (code-snippet-strings (post {:snippet "foo"
                                        :tags ["clojure"]}))
           ["foo"])))

  (testing "tag with no fence syntax"
    (is (= (code-snippet-syntaxes (post {:tags ["java"]}))
           [#{"java"}])))

  (testing "tag with fence syntax"
    (is (= (code-snippet-syntaxes (post {:tags ["java"]
                                         :fence-syntax "clojure"}))
           [#{"clojure"}])))

  (testing "question id"
    (is (= (code-snippet-question-ids (post {:question-id "12321"
                                             :tags ["clojure"]}))
           ["12321"])))

  (testing "answer id"
    (let [result (code-info (post {:question-id "12321"
                                   :answer-id "32123"
                                   :tags ["clojure"]}))]
      (is (and (= (map :answer-id result)
                  ["32123"])
               (= (map :question-id result)
                  ["12321"])))))

  (testing "tab expansion by fence annotation"
    (are [syntax result]
         (= (code-snippet-strings (post {:tags []
                                         :fence-syntax syntax
                                         :snippet "abc\txyz"}))
            [result])
         "java" "abc    xyz"
         "ruby" "abc  xyz"))

  (testing "tab expansion by post tag"
    (are [syntax result]
         (= (code-snippet-strings (post {:tags [syntax]
                                         :snippet "abc\txyz"}))
            [result])
         "java" "abc    xyz"
         "ruby" "abc  xyz"))

  (testing "tab expansion: fence annotation trumps post tag"
    (is (= (code-snippet-strings (post {:tags ["ruby"]
                                        :fence-syntax "java"
                                        :snippet "abc\txyz"}))
           ["abc    xyz"])))

  (testing "no syntax detected => no info"
    (def qid 12345678)

    (are [md result] (= (code-info {"body_markdown" md
                                    "question_id" qid})
                        result)
         (fence-blk "xyz")              []
         (fence-blk "xyz" "invalid")    []
         (str (fence-blk "xyz")
              "\r\n\r\n"
              (fence-blk "abc" "java")) [{:string "abc\r\n"
                                          :syntax #{"java"}
                                          :question-id qid}]))
 (testing "pun nil"
    (is (empty? (code-info nil)))))

