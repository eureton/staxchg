(ns staxchg.state-test
  (:require [clojure.test :refer :all]
            [staxchg.state :refer :all]))

(deftest question-id-to-index-test
  (testing "pun nil"
    (is (nil? (question-id-to-index nil nil))))
  (testing "1 arg"
    (let [question-1 {"question_id" "1234"}
          question-2 {"question_id" "4321"}
          world {:questions [question-1 question-2]}]
      (are [id result]
           (= (question-id-to-index id world) result)
           "1234" 0
           "4321" 1
           "1357" nil))))

(deftest selected-answer-index-test
  (testing "1 arg: pun nil"
    (is (nil? (selected-answer-index nil))))
  (testing "1 arg"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}
          question {"question_id" "1234"
                    "answers" [answer-1 answer-2]}]
      (are [answer-id result]
           (let [world {:questions [question]
                        :selected-question-index 0
                        :selected-answers {"1234" answer-id}}]
             (= (selected-answer-index world) result))
           "1357" 0
           "7531" 1)))
  (testing "2 args"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}
          answer-3 {"answer_id" "2468"}
          answer-4 {"answer_id" "8642"}
          question-1 {"question_id" "1234"
                      "answers" [answer-1 answer-2]}
          question-2 {"question_id" "4321"
                      "answers" [answer-3 answer-4]}]
      (are [question result]
           (let [world {:questions [question-1 question-2]
                        :selected-answers {"1234" "1357"
                                           "4321" "8642"}}]
             (= (selected-answer-index question world) result))
           question-1  0
           question-2  1
           {:x "0000"} nil))))

(deftest fetch-answers-page?-test
  (testing "pun nil"
    (is (nil? (fetch-answers? nil nil))))
  (testing "answer_count: non-zero, no answers exist => true"
    (is (true? (boolean (fetch-answers? {"answer_count" 1} nil)))))
  (testing "answer_count: non-zero, answers exist => false"
    (is (false? (boolean (fetch-answers? {"answer_count" 1
                                          "answers" [:x]} nil)))))
  (testing "answer_count: zero => false"
    (is (false? (boolean (fetch-answers? {"answer_count" 0} nil)))))
  (testing "selected ID is last, :more-answers-to-fetch? is true => true"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}]
      (are [selected-id more-to-fetch? result]
           (let [question {"question_id" "1234"
                           "answers" [answer-1 answer-2]
                           :more-answers-to-fetch? more-to-fetch?}
                 world {:questions [question]
                        :selected-question-index 0
                        :selected-answers {"1234" selected-id}}]
             (= (boolean (fetch-answers? question world)) result))
             "1357" nil   false
             "1357" false false
             "1357" true  false
             "7531" nil   false
             "7531" false false
             "7531" true  true))))

(deftest decrement-selected-answer-test
  (testing "no selected answer => NOP"
    (let [question {"question_id" "1234"}
          world {:questions [question]
                 :selected-question-index 0
                 :selected-answers {}}
          result (decrement-selected-answer world)]
      (is (= world result))))
  (testing "selected ID"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}]
      (are [selected-id]
           (let [question {"question_id" "1234"
                           "answers" [answer-1 answer-2]}
                 world {:questions [question]
                        :selected-question-index 0
                        :selected-answers {"1234" selected-id}}
                 result (decrement-selected-answer world)]
             (= (get-in result [:selected-answers "1234"]) "1357"))
             "7531"
             "1357"))))

(deftest increment-selected-answer-test
  (testing "no selected answer => NOP"
    (let [question {"question_id" "1234"}
          world {:questions [question]
                 :selected-question-index 0
                 :selected-answers {}}
          result (increment-selected-answer world)]
      (is (= world result))))
  (testing "must fetch => mark"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}
          question {"question_id" "1234"
                    "answer_count" 2
                    "answers" [answer-1 answer-2]
                    :more-answers-to-fetch? true}
          world {:questions [question]
                 :selected-question-index 0
                 :selected-answers {"1234" "7531"}}
          result (increment-selected-answer world)]
      (is (= (result :fetch-answers)
             {:question-id "1234" :page 1}))))
  (testing "selected ID"
    (let [answer-1 {"answer_id" "1357"}
          answer-2 {"answer_id" "7531"}]
      (are [selected-id]
           (let [question {"question_id" "1234"
                           "answers" [answer-1 answer-2]}
                 world {:questions [question]
                        :selected-question-index 0
                        :selected-answers {"1234" selected-id}}
                 result (increment-selected-answer world)]
             (= (get-in result [:selected-answers "1234"]) "7531"))
             "7531"
             "1357"))))

(deftest next-answers-page-test
  (testing "pun nil"
    (is (= (next-answers-page nil) 1)))
  (testing "mod 5"
    (are [answers result] (= (next-answers-page {"answers" answers})
                             result)
         (repeat  0 :x) 1
         (repeat  1 :x) 1
         (repeat  4 :x) 1
         (repeat  5 :x) 2
         (repeat  6 :x) 2
         (repeat  9 :x) 2
         (repeat 10 :x) 3
         (repeat 11 :x) 3
         (repeat 14 :x) 3
         (repeat 15 :x) 4
         (repeat 16 :x) 4)))

(deftest update-for-answers-response-test
  (defn to-response [body]
    {:body (cheshire.core/encode body)})
  (defn to-answer [id]
    {"answer_id" id
     "body_markdown" ""
     "comments" []})

  (testing "clears irrelevant marks"
    (are [mark] (let [result (update-for-answers-response
                               {mark "any"}
                               (to-response {"items" []})
                               nil)]
                  (not (contains? result mark)))
         :scroll-deltas
         :query?
         :quit?
         :search-term
         :fetch-answers))
  (testing "answers set"
    (are [previous-answer-ids current-answer-ids]
      (let [question-id "1234"
            question {"question_id" question-id
                      "answer_count" (count previous-answer-ids)
                      "answers" (map to-answer previous-answer-ids)}
            world {:questions [question]
                   :selected-question-index 0}
            result (update-for-answers-response
                     world
                     (to-response {"items" (map to-answer current-answer-ids)})
                     question-id)]
        (= (map #(% "answer_id") (get-in result [:questions 0 "answers"]))
           (concat previous-answer-ids current-answer-ids)))
      nil      ["1357" "7531"]
      []       ["1357" "7531"]
      ["abcd"] ["1357" "7531"]))
  (testing "supplementation with question id"
    (let [question-id "1234"
          world {:questions [{"question_id" question-id}]
                 :selected-question-index 0}
          result (update-for-answers-response
                   world
                   (to-response {"items" (map to-answer ["a1" "a2"])})
                   question-id)]
      (is (= (->> (get-in result [:questions 0 "answers"])
                  (map #(% "question_id"))
                  distinct)
             [question-id]))))
  (testing "supplementation with question tags"
    (let [question-id "1234"
          tags ["t1" "t2"]
          world {:questions [{"question_id" question-id
                              "tags" tags}]
                 :selected-question-index 0}
          result (update-for-answers-response
                   world
                   (to-response {"items" (map to-answer ["a1" "a2"])})
                   question-id)]
      (is (= (->> (get-in result [:questions 0 "answers"])
                  (map #(% "tags")))
             (repeat 2 tags)))))
  (testing "more-answers-to-fetch? set"
    (are [value]
      (let [question-id "1234"
            question {"question_id" question-id}
            world {:questions [question]
                   :selected-question-index 0}
            result (update-for-answers-response
                     world
                     (to-response {"items" [(to-answer "1357")] "has_more" value})
                     question-id)]
        (= (get-in result [:questions 0 :more-answers-to-fetch?])
           value))
      nil
      false
      true))
  (testing "selected-answers set"
    (are [answers selected-id]
      (let [question-id "1234"
            question {"question_id" question-id}
            world {:questions [question]
                   :selected-question-index 0}
            result (update-for-answers-response
                     world
                     (to-response {"items" answers})
                     question-id)]
        (= (get-in result [:selected-answers question-id])
           selected-id))
      nil                                     nil
      []                                      nil
      [(to-answer "1357")]                    "1357"
      [(to-answer "1357") (to-answer "7531")] "1357"))
  (testing "no answers => set :no-answers"
    (let [result (update-for-answers-response
                   {}
                   (to-response {"items" []})
                   nil)]
      (is (true? (result :no-answers)))))
  (testing "error => set :fetch-failed"
    (are [value] (let [result (update-for-answers-response
                                {}
                                (to-response {"error" value})
                                nil)]
                   (true? (result :fetch-failed)))
         0
         1
         true
         "")))

(deftest increment-selected-answer-test
  (defn question
    ([{:keys [answer-count answers more-answers-to-fetch?]}]
     (cond-> {"question_id" "1234"
              "answer_count" answer-count}
       (some? answers) (assoc "answers" answers)
       (some? more-answers-to-fetch?) (assoc :more-answers-to-fetch? more-answers-to-fetch?)))
    ([]
     (question {:answer-count 0})))

  (defn world
    ([{:keys [question selected-answers]}]
     {:questions [question]
      :selected-question-index 0
      :selected-answers selected-answers})
    ([]
     (world {:question (question) :selected-answers {}})))

  (testing "pun nil"
    (is (nil? (increment-selected-answer nil))))
  (testing "no previous answers, no clamp, no fetch => NOP"
    (is (= (increment-selected-answer (world))
           (world))))
  (testing "previous answers, no clamp => increment"
    (let [question (question {:answer-count 2
                              :answers [{"answer_id" "1357"}
                                        {"answer_id" "7531"}]})
          world (world {:question question
                        :selected-answers {"1234" "1357"}})
          result (increment-selected-answer world)]
      (is (= (get-in result [:selected-answers "1234"])
             "7531"))))
  (testing "previous answers, clamp => no increment"
    (let [question (question {:answer-count 2
                              :answers [{"answer_id" "1357"}
                                        {"answer_id" "7531"}]})
          world (world {:question question
                        :selected-answers {"1234" "7531"}})
          result (increment-selected-answer world)]
      (is (= (get-in result [:selected-answers "1234"])
             "7531"))))
  (testing "no previous answers, fetch => mark"
    (let [question (question {:answer-count 2})
          world (world {:question question})
          result (increment-selected-answer world)]
      (is (and (= (result :fetch-answers)
                  {:question-id "1234" :page 1})
               (= (dissoc result :fetch-answers)
                  world)))))
  (testing "previous answers, fetch => mark"
    (let [question (question {:answer-count 2
                              :answers [{"answer_id" "1357"}
                                        {"answer_id" "7531"}]
                              :more-answers-to-fetch? true})
          world (world {:question question
                        :selected-answers {"1234" "7531"}})
          result (increment-selected-answer world)]
      (is (and (= (result :fetch-answers)
                  {:question-id "1234" :page 1})
               (= (dissoc result :fetch-answers)
                  world))))))

