(ns staxchg.state
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.presentation.state :as presentation.state])
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
      :highlights {}}))
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
   (selected-answer-index (presentation.state/selected-question world) world)))

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
         :as question} (presentation.state/selected-question world)
        index (selected-answer-index question world)]
    (cond-> world
      (some? index) (assoc-in
                      [:selected-answers question_id]
                      (-> index dec (max 0) answers (get "answer_id"))))))

(defn increment-selected-answer
  ""
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
  ""
  [line-offset post world]
  (let [post-height (presentation/post-line-count post world)
        container-height (presentation/active-pane-body-height world)]
    (min
      (max 0 (- post-height container-height))
      (max 0 line-offset))))

(defn update-selected-post-line-offset
  ""
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
  (quot (presentation/active-pane-body-height world) 2))

(defn full-screen [world]
  (dec (presentation/active-pane-body-height world)))

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

(defn has-highlights?
  ""
  [world post]
  (contains? (:highlights world) (post/id post)))

(defn mark-hilite-pending
  ""
  [{:as world :keys [active-pane]}]
  (let [question (presentation.state/selected-question world)
        answer (presentation.state/selected-answer question world)
        question? (and (= active-pane :questions)
                       (not (has-highlights? world question)))
        answer? (and (some? answer)
                     (= active-pane :answers)
                     (not (has-highlights? world answer)))
        snippets (cond
                   question? (post/code-info question)
                   answer? (post/code-info answer))]
    (cond-> world
      (not-empty snippets) (assoc :snippets snippets))))

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
         :strs [question_id]} (presentation.state/selected-question world)
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
    (mark-answer-switch command)
    (mark-hilite-pending)))

(defn update-for-screen
  ""
  [world screen]
  (update world :io/context assoc :screen screen))

(defn update-for-highlighter
  ""
  [world value]
  (let [highlighter (or (-> value keyword hilite/tools)
                        :skylighting)]
    (dev/log "[update-for-highlighter] '" value "' -> " highlighter)
    (update world :io/context assoc :highlighter highlighter)))

(defn update-for-dimensions
  ""
  [world]
  (let [size (-> world :io/context :screen .getTerminalSize)]
    (assoc world :width (.getColumns size) :height (.getRows size))))

(defn update-for-keystroke [world keystroke]
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
  ""
  [world size]
  (when (some? size)
    (let [width (.getColumns size)
          height (.getRows size)]
      (dev/log "[update-for-resize] width: " width ", height: " height)))
  (cond-> (clear-marks world)
    size (assoc :width (.getColumns size)
                :height (.getRows size)
                :switched-pane? true)))

(defn update-for-search-term [world term]
  (dev/log "[update-for-search-term] term: " (if (nil? term) "<canceled>" term))
  (let [blank? (clojure.string/blank? term)]
    (cond-> (clear-marks world)
      (not blank?) (assoc :search-term term)
      blank? (assoc :switched-pane? true))))

(defn update-for-new-questions
  ""
  [{:keys [width height io/context]}
   questions]
  (-> (make questions)
      (assoc :io/context context :width width :height height)
      (assoc :switched-question? true)
      mark-hilite-pending))

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
      mark-hilite-pending)))

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

(defn update-for-highlights
  ""
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

(defn update-world-rf
  ""
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
  ""
  [world input]
  (when (some? input)
    (-> (reduce update-world-rf world input)
        (assoc :previous world))))

(defn dirty?
  "Returns true if a user-visible change has occurred, false otherwise."
  [world-before world-after]
  (let [sanitize (comp clear-marks
                       #(dissoc % :previous :io/context))]
    (not= (sanitize world-before)
          (sanitize world-after))))

(defn render?
  "Returns true if both conditions are fulfilled, false otherwise:
     1. can be rendered
     2. must be rendered

   Condition (1) implies that preparatory I/O has been completed.
   Condition (2) implies that new, display-worthy information has arrived."
  [{:as world
    :keys [active-pane switched-question? switched-answer?  width height]
    pane? :switched-pane?}]
  (let [question? (and (= active-pane :questions) switched-question?)
        answer? (and (= active-pane :answers) switched-answer?)
        change? (dirty? (:previous world) world)
        render? (and (some? width)
                     (some? height)
                     (or pane? question? answer? change?))]
    (if render?
      (do (when pane? (dev/log "[render?] switched pane"))
          (when question? (dev/log "[render?] switched question"))
          (when answer? (dev/log "[render?] switched answer"))
          (when change? (dev/log "[render?] state change")))
      (dev/log "[render?] none"))
    render?))

(comment
  (let [qid 12345678
        aid 87654321
        md (->> ["```"
                 "; lorem ipsum"
                 "(defn foo"
                 "  \"dolor sit amet\""
                 "  [x]"
                 "  (* x x))"
                 "```"]
                (clojure.string/join "\r\n"))
        q2id 11223344
        md2 (->> ["Have some Haskell:"
                  ""
                  "    ws <- getLine >>= return . words  -- Monad"
                  "    ws <- words <$> getLine           -- Functor (much nicer)"
                  ""
                  "Now have some JavaScript :D"
                  ""
                  "```"
                  "console.log(\"Functor\");"
                  "{"
                  "  const unit = (val) => ({"
                  "    // contextValue: () => val,"
                  "    fmap: (f) => unit((() => {"
                  "      //you can do pretty much anything here"
                  "      const newVal = f(val);"
                  "      //  console.log(newVal); //IO in the functional context"
                  "      return newVal;"
                  "    })()),"
                  "  });"
                  ""
                  "  const a = unit(3)"
                  "    .fmap(x => x * 2)  //6"
                  "    .fmap(x => x + 1); //7"
                  "}"
                  "```"]
                 (clojure.string/join "\r\n"))

        qs-raw [{"tags" ["haskell" "javascript"]
                 "question_id" q2id
                 "body_markdown" md2
                 "title" "javascript :("}
                {"tags" ["clojure"]
                 "question_id" qid
                 "body_markdown" md
                 "title" "haskell !!"}]
        as-raw [{"tags" ["c++"]
                 "answer_id" aid
                 "question_id" qid
                 "body_markdown" ""
                 "title" ""}]
        qs (mapv api/scrub qs-raw)
        as (mapv api/scrub as-raw)
        req-ch (clojure.core.async/chan 1)
        resp-ch (clojure.core.async/chan 1)
        ctx {:screen 1234 :highlighter :highlight.js}
        w1 (-> (make)
               (assoc :io/context ctx :width 100 :height 200)
               (update-for-new-questions qs)
               )
        in-rs (staxchg.state.recipe/input w1)
        out-rs (presentation/recipes w1)
        req {:recipes in-rs :context ctx}
        _ (do
            (clojure.core.async/>!! req-ch req)
            (cookbook.core/route {:from req-ch :to resp-ch :log-fn dev/log}))
        in (clojure.core.async/<!! resp-ch)
        w2 (update-world w1 in)
        plot (staxchg.markdown/plot (get-in w2 [:questions 0 "body_markdown"]) {:width 118})
        hilites (get-in w2 [:highlights q2id])
        a2 (get-in w2 [:questions 0 "answers" 0])
        ]
    (cookbook.step/inflate-param (first (:params (first (second out-rs)))) (:io/context w2))
;   (staxchg.flow.item/highlight-code {:plot plot :highlights hilites})
;   {:state (select-keys w2 [:snippets :highlights])
;    :incoming in-rs
;    :outgoing out-rs
;    :render? (render? w1)
;    }
    ))

