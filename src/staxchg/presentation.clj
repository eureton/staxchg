(ns staxchg.presentation
  (:require [staxchg.flow :as flow])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.markdown :as markdown])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(defn zones
  ""
  [{:as world
    :keys [width height clear-questions-pane-body? clear-answers-pane-body?]}]
  (let [question-list-left 1
        question-list-size 2
        questions-body-left 1
        questions-body-top (inc question-list-size)
        answers-header-left 1
        answers-header-height 1
        answers-separator-height 1
        answer-body-left 1
        answer-body-top (+ answers-header-height answers-separator-height)]
    {:questions-header {:id :questions-header
                        :left question-list-left
                        :top 0
                        :width (- width (* question-list-left 2))
                        :height question-list-size}
     :questions-separator {:id :questions-separator
                           :left 0
                           :top question-list-size
                           :width width
                           :height 1}
     :questions-body {:id :questions-body
                      :left questions-body-left
                      :top questions-body-top
                      :width (- width (* questions-body-left 2))
                      :height (- height questions-body-top 1)
                      :clear? clear-questions-pane-body?}
     :questions-footer {:id :questions-footer
                        :left 0
                        :top (dec height)
                        :width width
                        :height 1}
     :answers-body {:id :answers-body
                    :left answer-body-left
                    :top answer-body-top
                    :width (- width (* answer-body-left 2))
                    :height (- height answer-body-top 1)
                    :clear? clear-answers-pane-body?}
     :answers-footer {:id :answers-footer
                      :left 0
                      :top (dec height)
                      :width width
                      :height 1}
     :answers-header {:id :answers-header
                      :left answers-header-left
                      :top 0
                      :width (- width (* answers-header-left 2))
                      :height answers-header-height}
     :answers-separator {:id :answers-separator
                         :left 0
                         :top answers-header-height
                         :width width
                         :height answers-separator-height}}))

(defn format-date
  ""
  [unixtime]
  (let [datetime (java.time.LocalDateTime/ofEpochSecond unixtime 0 java.time.ZoneOffset/UTC)]
    (format
      "%4d-%02d-%02d %02d:%02d"
      (.getYear datetime)
      (.getValue (.getMonth datetime))
      (.getDayOfMonth datetime)
      (.getHour datetime)
      (.getMinute datetime))))

(defn format-author
  ""
  [post]
  (format
    "%s (%s)"
    (get-in post ["owner" "display_name"])
    (get-in post ["owner" "reputation"])))

(defn format-question-meta
  ""
  [question]
  (format
    "(S: %d) | (V: %d) | %s | %s"
    (question "score")
    (question "view_count")
    (format-author question)
    (format-date (question "last_activity_date"))))

(defn format-answer-meta
  ""
  [answer]
  (format
    "%d | %s | %s"
    (answer "score")
    (format-author answer)
    (format-date (answer "last_activity_date"))))

(defn format-comment-meta
  ""
  [post]
  (format
    "%d | %s | %s"
    (post "score")
    (format-author post)
    (format-date (post "creation_date"))))

(defn format-question-list-item
  ""
  [question width]
  (format
    (str "%-" width "s")
    (question "title")))

(defn format-questions-pane-separator
  ""
  [{:keys [width question-list-size question-list-offset questions]}]
  (let [from (inc question-list-offset)
        to (dec (+ from question-list-size))
        hint (format "(%d-%d of %d)" from to (count questions))]
    (format
      "%s%s%s"
      (apply str (repeat (- width (count hint) 1) \-))
      hint
      "-")))

(defn format-answers-pane-separator
  ""
  [{:as question :strs [question_id answers]}
   {:as world :keys [width selected-answers]}]
  (let [answer-id (selected-answers question_id)
        index (->>
                answers
                (map-indexed vector)
                (filter (fn [[i a]] (= (a "answer_id") answer-id)))
                first
                first)
        hint (if (nil? index)
               "(question has no answers)"
               (format
                 "(%d of %d)"
                 (inc index)
                 (count answers)))]
    (format
      "%s%s%s"
      (apply str (repeat (- width (count hint) 1) \-))
      hint
      "-")))

(defn question-list-item-flow
  ""
  [question
   index
   {:as world
    :keys [selected-question-index question-list-offset]}]
  (let [{:keys [width]} ((zones world) :questions-header)
        text (format-question-list-item question width)
        selected? (= index (- selected-question-index question-list-offset))]
    (flow/make {:type :string
                :raw text
                :modifiers (if selected? [SGR/REVERSE] [])})))

(defn visible-questions
  ""
  [{:as world
    :keys [questions question-list-size question-list-offset]}]
  (subvec
    questions
    question-list-offset
    (min (count questions) (+ question-list-offset question-list-size))))

(defn selected-answer
  [{:as world
    :keys [questions selected-question-index selected-answers]}]
  (let [selected-question (questions selected-question-index)
        answer-id (selected-answers (selected-question "question_id"))]
    (->>
      (selected-question "answers")
      (filter #(= (% "answer_id") answer-id))
      first)))

(def comments-left-margin 16)

(defn comment-flow
  ""
  [c rect]
  (let [meta-text (format-comment-meta c)]
    (flow/add
      (flow/make {:type :markdown
                  :raw (c "body_markdown")
                  :sub-zone (->
                              rect
                              (assoc :left comments-left-margin)
                              (update :width - comments-left-margin))
                  :foreground-color TextColor$ANSI/BLUE})
      (flow/make {:type :string
                  :raw meta-text
                  :x (- (rect :width) (count meta-text))
                  :foreground-color TextColor$ANSI/BLUE
                  :modifiers [SGR/BOLD]}))))

(defn comments-flow
  ""
  [post world]
  (reduce
    #(flow/add %1 flow/y-separator %2)
    flow/zero
    (map
      #(comment-flow
         %
         ((if (contains? post "answer_id")
            :questions-body
            :answers-body) (zones world)))
      (post "comments"))))

(defn question-flow
  ""
  [question world]
  (flow/make {:type :markdown
              :raw (question "body_markdown")
              :scroll-delta (get-in world [:scroll-deltas (question "question_id")])}))

(defn answer-flow
  ""
  [answer world]
  (flow/make {:type :markdown
              :raw (answer "body_markdown")
              :scroll-delta (get-in world [:scroll-deltas (answer "answer_id")])}))

(defn questions-body-flow
  ""
  [question world]
  (flow/add
    (question-flow question world)
    (comments-flow question world)))

(defn question-line-count
  ""
  [question world]
  (flow/line-count
    (questions-body-flow question world)
    ((zones world) :questions-body)))

(defn answers-body-flow
  ""
  [answer world]
  (flow/add
    (answer-flow answer world)
    (comments-flow answer world)))

(defn answer-line-count
  ""
  [answer world]
  (flow/line-count
    (answers-body-flow answer world)
    ((zones world) :answers-body)))

(defn flows
  ""
  [{:as world
    :keys [width height question-list-size questions selected-question-index
           active-pane line-offsets]}]
  (let [question (questions selected-question-index)
        answer (selected-answer world)
        question-meta-text (format-question-meta question)
        answer-meta-text (format-answer-meta answer)]
    {:questions-separator (flow/make {:type :string
                                      :raw (format-questions-pane-separator world)})
     :questions-list (reduce
                       flow/add
                       flow/zero
                       (map-indexed
                         #(question-list-item-flow %2 %1 world)
                         (visible-questions world)))
     :question-body (flow/scroll-y
                      (questions-body-flow question world)
                      (- (line-offsets (question "question_id"))))
     :question-meta (flow/make {:type :string
                                :raw question-meta-text
                                :x (- width (count question-meta-text))
                                :foreground-color TextColor$ANSI/YELLOW})
     :answer (flow/scroll-y
               (answers-body-flow answer world)
               (- (line-offsets (answer "answer_id"))))
     :answer-meta (flow/make {:type :string
                              :raw answer-meta-text
                              :x (- width (count answer-meta-text))
                              :foreground-color TextColor$ANSI/YELLOW})
     :answer-acceptance (flow/make {:type :string
                                    :raw (if (answer "is_accepted") " ACCEPTED " "")
                                    :x 1
                                    :foreground-color TextColor$ANSI/BLACK
                                    :background-color TextColor$ANSI/YELLOW})
     :answers-header (flow/make {:type :string
                                 :raw (question "title")
                                 :modifiers [SGR/REVERSE]})
     :answers-separator (flow/make {:type :string
                                    :raw (format-answers-pane-separator question world)})}))

