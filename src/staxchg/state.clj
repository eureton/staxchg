(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.api :as api])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(def mark-keys #{:scroll-deltas :query? :quit? :search-term :fetch-answers
                 :no-answers :fetch-failed})

(defn initialize-world
  ""
  [response-body width height]
  (let [questions (mapv api/scrub-question (response-body "items"))
        question-ids (map #(% "question_id") questions)]
    {:line-offsets (zipmap question-ids (repeat 0))
     :selected-question-index 0
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

(defn question-id-to-index
  ""
  [question-id world]
  (->>
    (get world :questions)
    (map-indexed vector)
    (filter (fn [[_ x]] (= (x "question_id") question-id)))
    first
    first))

(defn selected-answer-index
  ""
  ([{:strs [question_id answers]}
    world]
   (let [answer-id (get-in world [:selected-answers question_id])]
     (->>
       answers
       (map-indexed vector)
       (filter (fn [[_ x]] (= (x "answer_id") answer-id)))
       first
       first)))
  ([world]
   (selected-answer-index (presentation/selected-question world) world)))

(defn fetch-answers?
  ""
  [{:strs [question_id answer_count answers]
    :keys [more-answers-to-fetch?]
    :as question}
   world]
  (let [fetched-answer-count (count answers)
        index (selected-answer-index question world)]
    (or (and (not (zero? (or answer_count 0)))
             (not (contains? question "answers")))
        (and more-answers-to-fetch?
             (= index (dec fetched-answer-count))))))

(defn next-answers-page
  ""
  [{:strs [answers]}]
  (-> answers count (/ api/answers-page-size) int inc))

(defn decrement-selected-question-index
  [{:as world
    :keys [selected-question-index question-list-offset]}]
  (let [visible? (>= (dec selected-question-index) question-list-offset)
        capped-dec #(max 0 (dec %))]
    (-> world
        (update :selected-question-index capped-dec)
        (update :question-list-offset (if visible? identity capped-dec)))))

(defn decrement-selected-answer
  ""
  [world]
  (let [{:strs [question_id answers]
         :as question} (presentation/selected-question world)
        index (selected-answer-index question world)]
    (cond-> world
      (some? index) (assoc-in
                      [:selected-answers question_id]
                      (-> index dec (max 0) answers (get "answer_id"))))))

(defn increment-selected-answer
  ""
  [world]
  (let [{:strs [question_id answers]
         :as question} (presentation/selected-question world)
        index (selected-answer-index question world)
        fetch? (fetch-answers? question world)
        increment? (and (not fetch?) (some? index))
        page (next-answers-page question)]
    (cond-> world
      fetch? (assoc :fetch-answers {:question-id question_id :page page})
      increment? (assoc-in
                   [:selected-answers question_id]
                   (-> index
                       inc
                       (min (dec (count answers)))
                       answers
                       (get "answer_id"))))))

(defn active-pane-body-height
  ""
  [{:as world :keys [active-pane]}]
  (->>
    (presentation/zones world)
    ((case active-pane :questions :questions-body :answers :answers-body))
    :height))

(defn clamp-line-offset
  ""
  [line-offset post world]
  (let [post-height (presentation/post-line-count post world)
        container-height (active-pane-body-height world)]
    (min
      (max 0 (- post-height container-height))
      (max 0 line-offset))))

(defn update-selected-post-line-offset
  ""
  [scrollf
   {:as world :keys [active-pane]}]
  (let [post (case active-pane
               :questions (presentation/selected-question world)
               :answers (presentation/selected-answer world))
        post-id (get post (case active-pane
                            :questions "question_id"
                            :answers "answer_id"))
        previous (presentation/line-offset post world)
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
  (apply dissoc world mark-keys))

(defn effect-answers-pane
  ""
  [world]
  (let [{:as question
         :strs [question_id]} (presentation/selected-question world)
        fetch? (fetch-answers? question world)
        page (next-answers-page question)]
    (if fetch?
      (assoc world :fetch-answers {:question-id question_id :page page})
      (assoc world :active-pane :answers))))

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
    :answers-pane (effect-answers-pane world)
    :questions-pane (assoc world :active-pane :questions)
    :previous-answer (decrement-selected-answer world)
    :next-answer (increment-selected-answer world)
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

(defn update-for-keystroke [world keycode ctrl?]
  (let [command (parse-command keycode ctrl?)]
    (dev/log "command: " (if (some? command) (name command) "UNKNOWN"))
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
  (->
    response
    api/parse-response
    (initialize-world width height)
    (assoc :switched-question? true)))

(defn update-for-new-answers
  ""
  [world answers more? question-id]
  (let [index (question-id-to-index question-id world)]
    (->
      world
      (update-in [:questions index "answers"] (comp vec concat) answers)
      (assoc-in [:questions index :more-answers-to-fetch?] more?)
      (assoc-in [:selected-answers question-id] (-> answers first (get "answer_id")))
      (assoc :active-pane :answers :switched-pane? true))))

(defn update-for-answers-response
  ""
  [world response question-id]
  (let [index (question-id-to-index question-id world)
        {:strs [items has_more error]} (api/parse-response response)
        answers (map api/scrub-answer items)]
    (cond->
      world
      true (clear-marks)
      error (assoc :fetch-failed true)
      (and (nil? error) (empty? answers)) (assoc :no-answers true)
      (not-empty answers) (update-for-new-answers answers has_more question-id))))

(defn update-for-no-answers
  ""
  [world]
  (->
    world
    (clear-marks)
    (assoc :switched-pane? true)))

(defn update-world
  ""
  [world
   {:as input :keys [function params]}]
  (dev/log "[update-world] '" function "'")
  (let [f (case function
            :read-key! update-for-keystroke
            :query! update-for-search-term
            :fetch-questions! update-for-query-response
            :fetch-answers! update-for-answers-response
            :no-answers! update-for-no-answers
            :fetch-failed! update-for-no-answers)]
    (apply f world params)))

(defn generated-output?
  ""
  [world-before world-after]
  (= (clear-marks world-before)
     (clear-marks world-after)))

(def w (-> (initialize-world dev/response-body 118 37)
           (update-for-keystroke \J false)
           (update-for-keystroke \J false)
           (update-for-keystroke \j false)
           (update-for-keystroke \j false)))

