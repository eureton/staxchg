(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.api :as api])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.hilite :as hilite])
  (:require [staxchg.post :as post])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(def mark-keys #{:scroll-deltas :query? :quit? :search-term :fetch-answers
                 :no-questions :no-answers :fetch-failed :switched-pane?
                 :switched-question? :switched-answer? :snippets})

(defn make
  ""
  ([questions]
   (let [question-ids (map #(% "question_id") questions)]
     {:line-offsets (zipmap question-ids (repeat 0))
      :selected-question-index 0
      :question-list-size 2
      :question-list-offset 0
      :questions questions
      :active-pane :questions
      :code-highlights {}}))
  ([]
   (make [])))

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
  (quot (active-pane-body-height world) 2))

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
  (assoc world :switched-pane? (and (not-any? world [:search-term :fetch-answers])
                                    (#{:questions-pane :answers-pane} command))))

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

(defn update-for-screen
  ""
  [world screen]
  (update world :io/context assoc :screen screen))

(defn update-for-dimensions
  ""
  [world]
  (let [size (-> world :io/context :screen .getTerminalSize)]
    (assoc world :width (.getColumns size) :height (.getRows size))))

(defn update-for-keystroke [world keystroke]
  (let [keycode (.getCharacter keystroke)
        ctrl? (.isCtrlDown keystroke)
        command (parse-command keycode ctrl?)]
    (dev/log "[update-for-keystroke] code: '" keycode "', "
             "ctrl? " ctrl? ", "
             "command: " (if (some? command) (name command) "UNKNOWN"))
    (->
      world
      (clear-marks)
      (effect-command command)
      (set-marks command))))

(defn update-for-search-term [world term]
  (dev/log "[update-for-search-term] term: " (if (nil? term) "<canceled>" term))
  (let [blank? (clojure.string/blank? term)]
    (cond-> (clear-marks world)
      (not blank?) (assoc :search-term term)
      blank? (assoc :switched-pane? true))))

(defn update-for-new-posts
  ""
  [world posts]
  (let [snippets (->> posts
                      (map post/code-info)
                      (keep not-empty)
                      flatten)]
    (cond-> world
      (not-empty snippets) (assoc :snippets snippets))))

(defn update-for-new-questions
  ""
  [{:keys [width height io/context]}
   questions]
  (-> (make questions)
      (assoc :io/context context :width width :height height)
      (assoc :switched-question? true)
      (update-for-new-posts questions)))

(defn update-for-questions-response
  ""
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
  ""
  [answer question]
  (-> answer
      (assoc "question_id" (question "question_id"))
      (update "tags" (comp distinct concat) (question "tags"))))

(defn update-for-new-answers
  ""
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
      (update-for-new-posts answers))))

(defn update-for-answers-response
  ""
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
  ""
  [{:as world
    :keys [questions]}]
  (cond-> world
    true (clear-marks)
    (not-empty questions) (assoc :switched-pane? true)))

(defn update-for-code-highlights
  ""
  [world sh-out question-id answer-id]
  (let [hilite (hilite/parse sh-out)
        post-id (or answer-id question-id)]
    (dev/log "[update-for-code-highlights] "
             "question-id: '" question-id "', "
             "answer-id: '" answer-id "'\r\n"
             hilite)
    (-> world
        (clear-marks)
        (assoc (if answer-id :switched-answer? :switched-question?) true)
        (update-in [:code-highlights post-id] (comp vec conj) hilite))))

(defn update-world-rf
  ""
  [world
   {:keys [function values]}]
  (if-some [f (case function
                :acquire-screen! update-for-screen
                :enable-screen! update-for-dimensions
                :read-key! update-for-keystroke
                :query! update-for-search-term
                :fetch-questions! update-for-questions-response
                :fetch-answers! update-for-answers-response
                :no-questions! update-for-no-posts
                :no-answers! update-for-no-posts
                :fetch-failed! update-for-no-posts
                :highlight-code! update-for-code-highlights
                nil)]
    (do (dev/log "[update-world-rf] " function)
        (apply f world values))
    world))

(defn update-world
  ""
  [world input]
  (if (some? input)
    (-> (reduce update-world-rf world input)
        (assoc :previous world))
    nil))

(defn generated-output?
  ""
  [world-before world-after]
  (not= (clear-marks world-before)
        (clear-marks world-after)))

(defn write-output?
  ""
  [world-before
   {:as world-after
    :keys [active-pane switched-question? switched-answer? switched-pane?
           width height]}]
  (and (some? width)
       (some? height)
       (or switched-pane?
           (and (= active-pane :questions) switched-question?)
           (and (= active-pane :answers) switched-answer?)
           (generated-output? world-before world-after))))

(comment
  (def w (let [qid 12345678
               aid 87654321
               qs-raw [{"tags" ["clojure"]
                        "question_id" qid
                        "body_markdown" (->> ["```"
                                              "; lorem ipsum"
                                              "(defn foo"
                                              "  \"dolor sit amet\""
                                              "  [x]"
                                              "  (* x x))"
                                              "```"]
                                             (clojure.string/join "\r\n"))
                        "title" ""}]
               as-raw [{"tags" ["c++"]
                        "answer_id" aid
                        "question_id" qid
                        "body_markdown" ""
                        "title" ""}]
               qs (mapv api/scrub qs-raw)
               as (mapv api/scrub as-raw)
               req-ch (clojure.core.async/chan 1)
               resp-ch (clojure.core.async/chan 1)
               ctx {:screen 1234}
               w1 (-> (make)
                      (assoc :io/context ctx :width 100 :height 200)
                      ;(update-for-new-questions qs)
                      )
               in-rs (staxchg.state.recipe/input w1)
               req {:recipes in-rs :context ctx}
               _ (comment(do
                   (clojure.core.async/>!! req-ch req)
                   (staxchg.request/route {:from req-ch
                                           :to resp-ch
                                           :log-fn dev/log})))
               in (clojure.core.async/<!! resp-ch)
               w2 (update-world w1 in)
               a2 (get-in w2 [:questions 0 "answers" 0])]
           (dev/log {:context 123 :recipes (presentation/recipes w2)})
           )))

