(ns staxchg.presentation.state
  (:require [staxchg.post :as post])
  (:gen-class))

(defn selected-question
  "Currently selected question."
  [{:as world
    :keys [selected-question-index]}]
  (get-in world [:questions selected-question-index]))

(defn selected-answer
  "Currently selected answer."
  ([{:strs [question_id answers]}
    world]
   (let [answer-id (or (get-in world [:selected-answers question_id])
                       (-> answers (get 0) (get "answer_id")))]
     (some #(when (= answer-id (% "answer_id")) %)
           answers)))
  ([world]
   (selected-answer (selected-question world) world)))

(defn line-offset
  "Vertical scroll offset at which post was last viewed."
  [post world]
  (or (get-in world [:line-offsets (post/id post)])
      0))

