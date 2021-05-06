(ns staxchg.post
  (:require [staxchg.markdown :as markdown])
  (:gen-class))

(defn answer?
  "Returns true if post is an answer, false otherwise."
  [post]
  (contains? post "answer_id"))

(defn question?
  "Returns true if post is a question, false otherwise."
  [post]
  (and (not (answer? post))
       (contains? post "question_id")))

(defn code-snippets
  ""
  [post]
  (let [answer? (answer? post)
        question? (question? post)
        id-key (cond answer? :answer-id question? :question-id)
        id-value (post (cond answer? "answer_id" question? "question_id"))]
    (->> (get post "body_markdown")
         staxchg.markdown/code-snippets
         (map #(assoc % id-key id-value)))))

