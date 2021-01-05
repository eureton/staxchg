(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:gen-class))

(defn increment-selected-question-index
  [{:as world
    :keys [questions selected-question-index question-list-offset question-list-size]}]
  (let [visible? (< (inc selected-question-index) (+ question-list-offset question-list-size))
        question-count (count questions)]
    (-> world
        (update :selected-question-index #(min (dec question-count) (inc %)))
        (update :question-list-offset (if visible?
                                        identity
                                        #(min (- question-count question-list-size) (inc %)))))))

(defn selected-question
  ""
  [{:as world
    :keys [questions selected-question-index]}]
  (questions selected-question-index))

(defn selected-answer-index
  ""
  [{:as world
    :keys [questions selected-question-index selected-answers]}]
  (let [answers ((selected-question world) "answers")
        selected-answer (presentation/selected-answer world)]
    (->>
      answers
      (map-indexed vector)
      (filter (fn [[_ answer]] (= answer selected-answer)))
      first
      first)))

(defn selected-line-offset [world]
  (let [selected-question-id ((selected-question world) "question_id")
        active-pane (world :active-pane)]
  (get-in world [:line-offsets selected-question-id active-pane])))

(defn decrement-selected-question-index
  [{:as world
    :keys [selected-question-index question-list-offset]}]
  (let [visible? (>= (dec selected-question-index) question-list-offset)
        capped-dec #(max 0 (dec %))]
    (-> world
        (update :selected-question-index capped-dec)
        (update :question-list-offset (if visible? identity capped-dec)))))

(defn cycle-selected-answer
  ""
  [{:as world
    :keys [questions selected-question-index selected-answers]}
   direction]
  (let [selected-question (selected-question world)
        answers (selected-question "answers")
        destination-answer (->>
                             (selected-answer-index world)
                             ((case direction :forwards inc :backwards dec))
                             (max 0)
                             (min (dec (count answers)))
                             answers)]
    (update-in
      world
      [:selected-answers (selected-question "question_id")]
      (constantly (destination-answer "answer_id")))))

(defn increment-selected-question-line-offset
  ""
  [world]
  (let [selected-question (selected-question world)
        {:keys [width height]} (presentation/questions-pane-body-dimensions world)
        line-count (presentation/question-line-count selected-question world)]
      (update-in
        world
        [:line-offsets (selected-question "question_id") (world :active-pane)]
        #(min
           (max 0 (- line-count height))
           (inc %)))))

(defn update-world [world keycode]
  (let [selected-question-id ((selected-question world) "question_id")
        active-pane (world :active-pane)]
    (case keycode
      \k (update-in world [:line-offsets selected-question-id active-pane] #(max 0 (dec %)))
      \j (increment-selected-question-line-offset world)
      \K (decrement-selected-question-index world)
      \J (increment-selected-question-index world)
      \newline (assoc world :active-pane :answers-pane)
      \backspace (assoc world :active-pane :questions-pane)
      \h (cycle-selected-answer world :backwards)
      \l (cycle-selected-answer world :forwards)
      world)))

(defn unescape-html [string]
  (org.jsoup.parser.Parser/unescapeEntities string true))

(defn scrub-body
  ""
  [post]
  (update post "body_markdown" unescape-html))

(defn scrub-answer
  ""
  [{:as answer
    :strs [body_markdown comments]}]
  (assoc
    answer
    "body_markdown" (unescape-html body_markdown)
    "comments" (mapv scrub-body (vec comments))))

(defn scrub-question
  ""
  [{:as question
    :strs [title body_markdown answers comments]}]
  (assoc
    question
    "title" (unescape-html title)
    "body_markdown" (unescape-html body_markdown)
    "answers" (mapv scrub-answer (vec answers))
    "comments" (mapv scrub-body (vec comments))))

(defn initialize-world
  ""
  [items width height]
  (let [questions (mapv scrub-question (items "items"))
        question-ids (map #(% "question_id") questions)]
    {:line-offsets (reduce
                     #(assoc %1 %2 {:questions-pane 0 :answers-pane 0})
                     {}
                     question-ids)
     :selected-question-index 0
     :selected-answers (reduce
                         #(assoc %1 %2 (get-in
                                         (->>
                                           questions
                                           (filter (fn [q] (= (q "question_id") %2)))
                                           first)
                                         ["answers" 0 "answer_id"]))
                         {}
                         question-ids)
     :question-list-size 2
     :question-list-offset 0
     :questions questions
     :width width
     :height height
     :active-pane :questions-pane}))

