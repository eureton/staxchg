(ns staxchg.state
  (:require [flatland.useful.fn :as ufn])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.presentation.state :as presentation.state])
  (:require [staxchg.api :as api])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.hilite :as hilite])
  (:require [staxchg.post :as post])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(def ^:const MIN_QUESTIONS_LIST_SIZE 1)

(def ^:const MAX_QUESTIONS_LIST_SIZE 12)

(def mark-keys
  "Set of keys used in the world hash to mark requirement for change of state."
  #{:scroll-deltas :query? :quit? :search-term :fetch-answers :no-questions
    :no-answers :fetch-failed :switched-pane? :switched-question?
    :switched-answer? :snippets})

(defn make
  "Hash representing a cold state, loaded with the given questions."
  ([questions]
   (let [question-ids (map #(% "question_id") questions)]
     {:line-offsets (zipmap question-ids (repeat 0))
      :selected-question-index 0
      :question-list-size (-> questions
                              count
                              (min MAX_QUESTIONS_LIST_SIZE)
                              (max MIN_QUESTIONS_LIST_SIZE))
      :question-list-offset 0
      :questions questions
      :active-pane :questions
      :highlights {}}))
  ([]
   (make [])))

(defn increment-selected-question-index
  "If possible, shifts the selection mark to the question after the one which is
   currently selected."
  [{:as world
    :keys [questions selected-question-index question-list-size]}]
  (let [outside? #(>= (inc selected-question-index)
                      (+ % question-list-size))
        question-count (count questions)
        excess (- question-count question-list-size)]
    (-> world
        (update :selected-question-index #(min (dec question-count) (inc %)))
        (update :question-list-offset (ufn/to-fix outside? (comp #(min excess %)
                                                                 inc))))))

(defn question-id-to-index
  "Index of the question with the given id in the collection of questions."
  [question-id world]
  (->>
    (get world :questions)
    (map-indexed vector)
    (filter (fn [[_ x]] (= (x "question_id") question-id)))
    first
    first))

(defn selected-answer-index
  "Index of the currently selected answer in the collection of answers to the
   currently selected question."
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
   (selected-answer-index (presentation.state/selected-question world) world)))

(defn fetch-answers?
  "True if fetching answers is required, false otherwise."
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
  "Page number to fetch answers for with respect to the given question."
  [{:strs [answers]}]
  (-> answers count (/ api/answers-page-size) int inc))

(defn decrement-selected-question-index
  "If possible, shifts the selection mark to the question before the one which
   is currently selected."
  [{:as world
    :keys [selected-question-index question-list-offset]}]
  (let [inside? (< (dec selected-question-index) question-list-offset)
        capped-dec #(max 0 (dec %))]
    (-> world
        (update :selected-question-index capped-dec)
        (update :question-list-offset (ufn/to-fix inside? capped-dec)))))

(defn decrement-selected-answer
  "If possible, shifts the selection mark to the answer before the one which
   is currently selected."
  [world]
  (let [{:strs [question_id answers]
         :as question} (presentation.state/selected-question world)
        index (selected-answer-index question world)]
    (cond-> world
      (some? index) (assoc-in
                      [:selected-answers question_id]
                      (-> index dec (max 0) answers (get "answer_id"))))))

(defn increment-selected-answer
  "If possible, shifts the selection mark to the answer after the one which
   is currently selected."
  [world]
  (let [{:strs [question_id answers]
         :as question} (presentation.state/selected-question world)
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

(defn clamp-line-offset
  "Clamps line-offset within the range permitted by post and the pane into which
   the latter is rendered."
  [line-offset post world]
  (let [post-height (presentation/post-line-count post world)
        container-height (presentation/active-pane-body-height world)]
    (min
      (max 0 (- post-height container-height))
      (max 0 line-offset))))

(defn update-selected-post-line-offset
  "Marks the new line offset of the currently selected post as appropriate. This
   involves applying scrollf to the existing value and then clamping the result
   to the corresponding range."
  [scrollf
   {:as world :keys [active-pane]}]
  (let [post (case active-pane
               :questions (presentation.state/selected-question world)
               :answers (presentation.state/selected-answer world))
        post-id (post/id post)
        previous (presentation.state/line-offset post world)
        current (clamp-line-offset (scrollf previous world) post world)]
    (->
      world
      (assoc-in [:scroll-deltas post-id] (- current previous))
      (assoc-in [:line-offsets post-id] current))))

(defn half-screen [world]
  "Number of lines the body of currently active pane spans, divided by 2."
  (quot (presentation/active-pane-body-height world) 2))

(defn full-screen [world]
  "Number of lines the body of currently active pane spans. Decrements one to
   preserve context."
  (dec (presentation/active-pane-body-height world)))

(defn one-line-down [n _]
  "Scroll function for going down by 1.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (inc n))

(defn one-line-up [n _]
  "Scroll function for going up by 1.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (dec n))

(defn half-screen-down [n world]
  "Scroll function for going down half a screen.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (+ n (half-screen world)))

(defn half-screen-up [n world]
  "Scroll function for going up half a screen.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (- n (half-screen world)))

(defn one-screen-down [n world]
  "Scroll function for going down a full screen.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (+ n (full-screen world)))

(defn one-screen-up [n world]
  "Scroll function for going up a full screen.
   Meant for use with staxchg.state/update-selected-post-line-offset"
  (- n (full-screen world)))

(defn mark-pane-switch
  "Marks world as having switched its active pane, if command requires it."
  [world command]
  (assoc world :switched-pane? (and (not-any? world [:search-term :fetch-answers])
                                    (#{:questions-pane :answers-pane} command))))

(defn mark-question-switch
  "Marks world as having switched its selected question, if command requires
   it."
  [world command]
  (assoc
    world
    :switched-question?
    (#{:previous-question :next-question} command)))

(defn mark-answer-switch
  "Marks world as having switched its selected answer, if command requires it."
  [world command]
  (assoc world :switched-answer? (#{:previous-answer :next-answer} command)))

(defn highlights-fetched?
  "True if fetching syntax highlights for post has been completed, false
   otherwise. Completion does not imply success, i.e. returns true for posts for
   which fetching failed."
  [world post]
  (contains? (:highlights world) (post/id post)))

(defn mark-hilite-pending
  "Marks world as requiring syntax highlights to be fetched."
  [{:as world :keys [active-pane]}]
  (let [question (presentation.state/selected-question world)
        answer (presentation.state/selected-answer question world)
        question? (and (= active-pane :questions)
                       (not (highlights-fetched? world question)))
        answer? (and (some? answer)
                     (= active-pane :answers)
                     (not (highlights-fetched? world answer)))
        snippets (cond
                   question? (post/code-info question)
                   answer? (post/code-info answer))]
    (cond-> world
      (not-empty snippets) (assoc :snippets snippets))))

(defn parse-command
  "Translates user keypress events into commands the application supports."
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
  "Clears all marks denoting requirement for change of state."
  [world]
  (apply dissoc world mark-keys))

(defn effect-answers-pane
  "Executes the :answers-pane command."
  [world]
  (let [{:as question
         :strs [question_id]} (presentation.state/selected-question world)
        fetch? (fetch-answers? question world)
        page (next-answers-page question)]
    (if fetch?
      (assoc world :fetch-answers {:question-id question_id :page page})
      (assoc world :active-pane :answers))))

(defn effect-command
  "Applies change of state to world, as required by command."
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
  "Marks world with the required changes in state."
  [world command]
  (->
    world
    (mark-pane-switch command)
    (mark-question-switch command)
    (mark-answer-switch command)
    (mark-hilite-pending)))

(defn update-for-screen
  "Applies the result of staxchg.io/acquire-screen! to world."
  [world screen]
  (update world :io/context assoc :screen screen))

(defn update-for-highlighter
  "Applies the result of staxchg.io/resolve-highlighter! to world."
  [world value]
  (let [highlighter (or (-> value keyword hilite/tools)
                        :skylighting)]
    (dev/log "[update-for-highlighter] '" value "' -> " highlighter)
    (update world :io/context assoc :highlighter highlighter)))

(defn update-for-dimensions
  "Applies the result of staxchg.io/enable-screen! to world."
  [world]
  (let [size (-> world :io/context :screen .getTerminalSize)]
    (assoc world :width (.getColumns size) :height (.getRows size))))

(defn update-for-keystroke
  "Applies the result of staxchg.io/poll-key! to world."
  [world keystroke]
  (if (some? keystroke)
    (let [keycode (.getCharacter keystroke)
          ctrl? (.isCtrlDown keystroke)
          command (parse-command keycode ctrl?)]
      (dev/log "[update-for-keystroke] code: '" keycode "', "
               "ctrl? " ctrl? ", "
               "command: " (if (some? command) (name command) "UNKNOWN"))
      (-> world
          (clear-marks)
          (effect-command command)
          (set-marks command)))
    world))

(defn update-for-resize
  "Applies the result of staxchg.io/poll-resize! to world."
  [world size]
  (when (some? size)
    (let [width (.getColumns size)
          height (.getRows size)]
      (dev/log "[update-for-resize] width: " width ", height: " height)))
  (cond-> (clear-marks world)
    size (assoc :width (.getColumns size)
                :height (.getRows size)
                :switched-pane? true)))

(defn update-for-search-term
  "Applies the result of staxchg.io/query! to world."
  [world term]
  (dev/log "[update-for-search-term] term: " (if (nil? term) "<canceled>" term))
  (let [blank? (clojure.string/blank? term)]
    (cond-> (clear-marks world)
      (not blank?) (assoc :search-term term)
      blank? (assoc :switched-pane? true))))

(defn update-for-new-questions
  "Applies the result of staxchg.io/fetch-questions! to world, if more than one
   question has been fetched."
  [{:keys [width height io/context]}
   questions]
  (-> questions
      make
      (assoc :io/context context
             :width width
             :height height
             :switched-question? true)
      mark-hilite-pending))

(defn update-for-questions-response
  "Applies the result of staxchg.io/fetch-questions! to world."
  [world response]
  (let [{:strs [items error quota_remaining]} (api/parse-response response)]
    (dev/log "[update-for-questions-response] " quota_remaining " quota left")
    (cond->
      world
      true (clear-marks)
      error (assoc :fetch-failed true)
      (and (nil? error) (empty? items)) (assoc :no-questions true)
      (not-empty items) (update-for-new-questions items))))

(defn supplement-answer
  "Normalizes hash fetched by an answer query."
  [answer question]
  (-> answer
      (assoc "question_id" (question "question_id"))
      (update "tags" (comp distinct concat) (question "tags"))))

(defn update-for-new-answers
  "Applies the result of staxchg.io/fetch-answers! to world, if more than one
   answer has been fetched."
  [world answers more? question-id]
  (let [index (question-id-to-index question-id world)
        question (get-in world [:questions index])
        answers (map supplement-answer answers (repeat question))]
    (->
      world
      (update-in [:questions index "answers"] (comp vec concat) answers)
      (assoc-in [:questions index :more-answers-to-fetch?] more?)
      (assoc-in [:selected-answers question-id] (-> answers first (get "answer_id")))
      (assoc :active-pane :answers :switched-pane? true)
      mark-hilite-pending)))

(defn update-for-answers-response
  "Applies the result of staxchg.io/fetch-answers! to world"
  [world response question-id]
  (let [index (question-id-to-index question-id world)
        {:strs [items has_more
                error quota_remaining]} (api/parse-response response)]
    (dev/log "[update-for-answers-response] " quota_remaining " quota left")
    (cond->
      world
      true (clear-marks)
      error (assoc :fetch-failed true)
      (and (nil? error) (empty? items)) (assoc :no-answers true)
      (not-empty items) (update-for-new-answers items has_more question-id))))

(defn update-for-no-posts
  "Applies the result of staxchg.io/fetch-questions! or
   staxchg.io/fetch-answers! to world, if no posts have been fetched."
  [{:as world
    :keys [questions]}]
  (cond-> world
    true (clear-marks)
    (not-empty questions) (assoc :switched-pane? true)))

(defn update-for-highlights
  "Applies the result of staxchg.io/highlight-code! to world."
  [world sh-out question-id answer-id]
  (let [hilite (hilite/parse sh-out)
        post-id (or answer-id question-id)]
    (dev/log "[update-for-highlights] "
             "question-id: '" question-id "', "
             "answer-id: '" answer-id "'\r\n"
             hilite)
    (-> world
        (clear-marks)
        (assoc (if answer-id :switched-answer? :switched-question?) true)
        (update-in [:highlights post-id]
                   (comp vec #(remove nil? %) conj)
                   hilite))))

(defn update-for-config
  "Applies the result of staxchg.io/read-config! to world."
  [world config]
  (let [{:strs [SITE MAX_QUESTIONS_LIST_SIZE LOGFILE]} config]
    (assoc world :config/site SITE
                 :config/max-questions-list-size MAX_QUESTIONS_LIST_SIZE
                 :config/logfile LOGFILE)))

(defn update-world-rf
  "Reducer for use with staxchg.state/update-world"
  [world
   {:keys [function values]}]
  (if-some [f (case function
                :acquire-screen! update-for-screen
                :resolve-highlighter! update-for-highlighter
                :enable-screen! update-for-dimensions
                :poll-key! update-for-keystroke
                :poll-resize! update-for-resize
                :query! update-for-search-term
                :fetch-questions! update-for-questions-response
                :fetch-answers! update-for-answers-response
                :no-questions! update-for-no-posts
                :no-answers! update-for-no-posts
                :fetch-failed! update-for-no-posts
                :highlight-code! update-for-highlights
                nil)]
    (do (dev/log "[update-world-rf] " function)
        (apply f world values))
    world))

(defn update-world
  "Applies I/O responses to world."
  [world input]
  (when input
    (let [updated (-> (reduce update-world-rf world input)
                      (assoc :previous (dissoc world :previous)))]
      (dev/log updated)
      updated)))

(def sanitize
  "Purges world of keys which don't reflect state."
  (comp clear-marks
        #(dissoc % :previous :io/context)))

(defn dirty?
  "True if a user-visible change has occurred, false otherwise."
  [world-before world-after]
  (not= (sanitize world-before)
        (sanitize world-after)))

(defn render?
  "True if both conditions are fulfilled, false otherwise:
     1. can be rendered
     2. must be rendered

   Condition (1) implies that preparatory I/O has been completed.
   Condition (2) implies that new, display-worthy information has arrived."
  [{:as world
    :keys [active-pane switched-question? switched-answer? width height]
    pane? :switched-pane?}]
  (let [question? (and (= active-pane :questions) switched-question?)
        answer? (and (= active-pane :answers) switched-answer?)]
    (and width height (or pane?
                          question?
                          answer?
                          (dirty? (:previous world) world)))))

