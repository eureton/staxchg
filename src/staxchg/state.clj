(ns staxchg.state
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

(defn selected-line-offset [world]
  (let [question-id (get-in world [:questions (world :selected-question-index) "question_id"])
        active-pane (world :active-pane)]
  (get-in world [:line-offsets question-id active-pane])))

(defn selected-answer
  [{:as world
    :keys [questions selected-question-index selected-answers]}]
  (let [question-id (get-in questions [selected-question-index "question_id"])
        answer-id (selected-answers question-id)]
    (->>
      (get-in questions [selected-question-index "answers"])
      (filter #(= (% "answer_id") answer-id))
      first)))

(defn selected-answer-index
  ""
  [{:as world
    :keys [questions selected-question-index selected-answers]}]
  (let [selected-question (questions selected-question-index)
        answers (selected-question "answers")
        selected-answer-id ((selected-answer world) "answer_id")]
        ;selected-answer-id ((selected-answer world) "answer_id")]
    (->>
      answers
      (map-indexed vector)
      (filter (fn [[_ a]] (= (a "answer_id") selected-answer-id)))
      first
      first)))

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
  (let [selected-question (questions selected-question-index)
        selected-question-id (selected-question "question_id")
        answers (selected-question "answers")
        selected-answer-id (selected-answers selected-question-id)
        selected-answer-index (->>
                                answers
                                (map-indexed vector)
                                (filter (fn [[_ a]] (= (a "answer_id") selected-answer-id)))
                                first
                                first)
        destination-answer (->>
                             selected-answer-index
                             ((condp = direction :forwards inc :backwards dec))
                             (max 0)
                             (min (dec (count answers)))
                             answers)]
    (update-in
      world
      [:selected-answers selected-question-id]
      (constantly (destination-answer "answer_id")))))

(defn update-world [world keycode]
  (let [question-index (world :selected-question-index)
        question-id (get-in world [:questions question-index "question_id"])
        active-pane (world :active-pane)]
    (case keycode
      \k (update-in world [:line-offsets question-id active-pane] #(max 0 (dec %)))
      \j (update-in world [:line-offsets question-id active-pane] inc)
      \K (decrement-selected-question-index world)
      \J (increment-selected-question-index world)
      \newline (assoc world :active-pane :answers-pane)
      \backspace (assoc world :active-pane :questions-pane)
      \h (cycle-selected-answer world :backwards)
      \l (cycle-selected-answer world :forwards)
      world)))

(defn unescape-html [string]
  (org.jsoup.parser.Parser/unescapeEntities string true))

(defn scrub-question [question]
  (reduce
    (fn [h [k f]] (update h k f))
    question
    [["title" unescape-html]
     ["body_markdown" unescape-html]
     ["answers" (fn [answers] (mapv #(update % "body_markdown" unescape-html) (vec answers)))]]))

(defn initialize-world [items screen]
  (let [questions (mapv scrub-question (items "items"))
        question-ids (map #(% "question_id") questions)
        size (.getTerminalSize screen)]
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
     :width (.getColumns size)
     :height (.getRows size)
     :active-pane :questions-pane}))

