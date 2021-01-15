(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.dev :as dev])
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

(defn active-pane-body-height
  ""
  [world]
  ((case (world :active-pane)
     :questions (presentation/questions-pane-body-dimensions world)
     :answers (presentation/answers-pane-body-dimensions world)) :height))

(defn line-offset
  ""
  [question
   {:as world
    :keys [active-pane]}]
  (get-in world [:line-offsets (question "question_id") active-pane]))

(defn clamp-line-offset
  ""
  [line-offset post world]
  (let [countf (if (contains? post "answer_id")
                 presentation/answer-line-count
                 presentation/question-line-count)
        line-count (countf post world)]
    (min
      (max 0 (- line-count (active-pane-body-height world)))
      (max 0 line-offset))))

(defn update-selected-post-line-offset
  ""
  [scrollf
   {:as world
    :keys [active-pane]}]
  (let [[countf post] (case active-pane
                        :questions [presentation/question-line-count
                                    (selected-question world)]
                        :answers [presentation/answer-line-count
                                  (presentation/selected-answer world)])
        id (post "question_id")
        previous (line-offset post world)
        current (clamp-line-offset (scrollf previous world) post world)]
    (dev/log "scroll-delta[" id "]: " (- current previous))
    (dev/log " line-offset[" id "]: " current)
    (->
      world
      (assoc-in [:scroll-deltas id] (- current previous))
      (assoc-in [:line-offsets id active-pane] current))))

(defn half-screen [world]
  (/ (active-pane-body-height world) 2))

(defn full-screen [world]
  (dec (active-pane-body-height world)))

(defn one-line-down [n _]
  (inc n))

(defn one-line-up [n _]
  (dec n))

(defn half-screen-down [n world]
  (+ n (half-screen world)))

(defn half-screen-up [n world]
  (- n (half-screen world)))

(defn one-screen-down [n world]
  (+ n (full-screen world)))

(defn one-screen-up [n world]
  (- n (full-screen world)))

(defn mark-clear-pane-body
  ""
  [world command]
  (let [pane-clear-commands #{:previous-question
                              :next-question
                              :previous-answer
                              :next-answer}]
    (->
      world
      (assoc :clear-questions-pane-body? (#{:previous-question :next-question} command))
      (assoc :clear-answers-pane-body? (#{:previous-answer :next-answer} command)))))

(defn parse-command
  ""
  [keycode ctrl?]
  (case keycode
      \k :one-line-up
      \j :one-line-down
      \b :one-screen-up
      \space :one-screen-down
      \u (when ctrl? :half-screen-up)
      \d (when ctrl? :half-screen-down)
      \K :previous-question
      \J :next-question
      \newline :answers-pane
      \backspace :questions-pane
      \h :previous-answer
      \l :next-answer
      nil))

(defn clear-marks
  ""
  [world]
  (->
    world
    (dissoc :scroll-deltas)))

(defn effect-command
  ""
  [world command]
  (case command
    :one-line-up (update-selected-post-line-offset one-line-up world)
    :one-line-down (update-selected-post-line-offset one-line-down world)
    :half-screen-up (update-selected-post-line-offset half-screen-up world)
    :half-screen-down (update-selected-post-line-offset half-screen-down world)
    :one-screen-up (update-selected-post-line-offset one-screen-up world)
    :one-screen-down (update-selected-post-line-offset one-screen-down world)
    :previous-question (decrement-selected-question-index world)
    :next-question (increment-selected-question-index world)
    :answers-pane (assoc world :active-pane :answers)
    :questions-pane (assoc world :active-pane :questions)
    :previous-answer (cycle-selected-answer world :backwards)
    :next-answer (cycle-selected-answer world :forwards)
    world))

(defn set-marks
  ""
  [world command]
  (->
    world
    (mark-clear-pane-body command)))

(defn update-world [world keycode ctrl?]
  (let [command (parse-command keycode ctrl?)]
    (->
      world
      (clear-marks)
      (effect-command command)
      (set-marks command))))

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
                     #(assoc %1 %2 {:questions 0 :answers 0})
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
     :active-pane :questions}))

