(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.api :as api])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn initialize-world
  ""
  [response-body width height]
  (let [questions (mapv api/scrub-question (response-body "items"))
        question-ids (map #(% "question_id") questions)
        answer-ids (->> questions (map #(% "answers")) flatten (map #(% "answer_id")))]
    {:line-offsets (->>
                     (concat question-ids answer-ids)
                     (map vector (repeat 0))
                     (map reverse)
                     (flatten)
                     (apply hash-map))
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

(defn selected-line-offset [{:as world :keys [line-offsets active-pane]}]
  (let [post-id (case active-pane
                  :questions ((selected-question world) "question_id")
                  :answers ((presentation/selected-answer world) "answer_id"))]
    (line-offsets post-id)))

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
  [{:as world :keys [active-pane]}]
  (->>
    (presentation/zones world)
    ((case active-pane :questions :questions-body :answers :answers-body))
    :height))

(defn line-offset
  ""
  [post
   {:as world
    :keys [line-offsets active-pane]}]
  (->>
    (case active-pane :questions "question_id" :answers "answer_id")
    post
    line-offsets))

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
   {:as world :keys [active-pane]}]
  (let [[countf post] (case active-pane
                        :questions [presentation/question-line-count
                                    (selected-question world)]
                        :answers [presentation/answer-line-count
                                  (presentation/selected-answer world)])
        post-id (post (case active-pane
                        :questions "question_id"
                        :answers "answer_id"))
        previous (line-offset post world)
        current (clamp-line-offset (scrollf previous world) post world)]
    (dev/log "scroll-delta[" post-id "]: " (- current previous))
    (dev/log " line-offset[" post-id "]: " current)
    (->
      world
      (assoc-in [:scroll-deltas post-id] (- current previous))
      (assoc-in [:line-offsets post-id] current))))

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

(defn mark-pane-switch
  ""
  [world command]
  (assoc world :switched-pane? (#{:questions-pane :answers-pane} command)))

(defn mark-question-switch
  ""
  [world command]
  (assoc
    world
    :switched-question?
    (#{:previous-question :next-question} command)))

(defn mark-answer-switch
  ""
  [world command]
  (assoc world :switched-answer? (#{:previous-answer :next-answer} command)))

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
      \/ :query
      \q :quit
      nil))

(defn clear-marks
  ""
  [world]
  (dissoc world :scroll-deltas :query? :quit? :search-term))

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
    :query (assoc world :query? true)
    :quit (assoc world :quit? true)
    world))

(defn set-marks
  ""
  [world command]
  (->
    world
    (mark-pane-switch command)
    (mark-question-switch command)
    (mark-answer-switch command)))

(defn update-for-keystroke [world keystroke]
  (let [keycode (.getCharacter keystroke)
        ctrl? (.isCtrlDown keystroke)
        command (parse-command keycode ctrl?)]
    (->
      world
      (clear-marks)
      (effect-command command)
      (set-marks command))))

(defn update-for-keystroke [world keycode ctrl?]
  (let [command (parse-command keycode ctrl?)]
    (->
      world
      (clear-marks)
      (effect-command command)
      (set-marks command))))

(defn update-for-search-term [world term]
  (cond->
    world
    true (clear-marks)
    (not (clojure.string/blank? term)) (assoc :search-term term)))

(defn update-for-query-response
  ""
  [{:as world :keys [width height]}
   response]
  (initialize-world (api/parse-response response) width height))

(defn update-world
  ""
  [world
   {:as input :keys [function params]}]
  (let [f (case function
            :read-key! update-for-keystroke
            :query! update-for-search-term
            :fetch! update-for-query-response)]
    (apply f world params)))

(def w (-> (initialize-world dev/response-body 118 37)
           (update-for-keystroke \J false)
           (update-for-keystroke \J false)
           (update-for-keystroke \j false)
           (update-for-keystroke \j false)))

