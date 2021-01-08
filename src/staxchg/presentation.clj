(ns staxchg.presentation
  (:require [staxchg.flow :as flow])
  (:require [staxchg.markdown :as markdown])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

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

(def question-list-left-margin 1)

(defn format-question-list-item
  ""
  [question width]
  (format
    (str "%-" (- width (* question-list-left-margin 2)) "s")
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

(defn question-list-dimensions
  [{:as world
    :keys [width height question-list-size]}]
  (let [left 1
        top 0]
    {:left left
     :top top
     :width (- width (* left 2))
     :height question-list-size}))

(defn question-list-item-flow
  ""
  [question
   index
   {:as world
    :keys [selected-question-index question-list-offset]}]
  (let [{:keys [left top width height]} (question-list-dimensions world)
        text (format-question-list-item question width)
        selected? (= index (- selected-question-index question-list-offset))]
    (flow/make {:type :string
                :raw text
                :x question-list-left-margin
                :viewport/left left
                :viewport/top top
                :viewport/width width
                :viewport/height height
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

(defn question-list-flow
  ""
  [world]
  (reduce
    flow/add
    flow/zero
    (map-indexed
      #(question-list-item-flow %2 %1 world)
      (visible-questions world))))

(defn questions-pane-separator-flow
  ""
  [{:as world
    :keys [width question-list-size]}]
  (flow/make {:type :string
              :raw (format-questions-pane-separator world)
              :viewport/top question-list-size
              :viewport/width width
              :viewport/height 1}))

(defn questions-pane-body-dimensions
  [{:as world
    :keys [width height question-list-size]}]
  (let [left 1
        top (inc question-list-size)]
    {:left left
     :top top
     :width (- width (* left 2))
     :height (- height top 1)}))

(defn answers-pane-body-dimensions
  ""
  [world]
  (let [left 1 top 2]
    {:left left
     :top top
     :width (- (world :width) (* left 2))
     :height (- (world :height) top 1)}))


(def comments-left-margin 4)

(defn comment-flow
  ""
  [c viewport]
  (let [{:keys [top width height]} viewport
        meta-text (format-comment-meta c)
        base {:foreground-color TextColor$ANSI/BLUE
              :viewport/left comments-left-margin
              :viewport/top top
              :viewport/width (- width comments-left-margin)
              :viewport/height height}]
    (flow/add
      (flow/make (merge base {:type :markdown
                              :raw (c "body_markdown")}))
      (flow/make (merge base {:type :string
                              :raw meta-text
                              :x (- width (count meta-text))
                              :modifiers [SGR/BOLD]})))))

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
            answers-pane-body-dimensions
            questions-pane-body-dimensions) world))
      (post "comments"))))

(defn question-flow
  ""
  [question world]
  (let [{:keys [left top width height]} (questions-pane-body-dimensions world)]
    (flow/make {:type :markdown
                :raw (question "body_markdown")
                :viewport/left left
                :viewport/top top
                :viewport/width width
                :viewport/height height
                :scrolled-by (get-in world [:scroll-deltas (question "question_id")])})))

(defn question-meta-flow
  ""
  [{:as world
    :keys [questions selected-question-index]}]
  (let [{:keys [left top width height]} (questions-pane-body-dimensions world)
        question (questions selected-question-index)
        text (format-question-meta question)]
    (flow/make {:type :string
                :raw text
                :x (- width (count text))
                :viewport/left left
                :viewport/top (+ top height)
                :viewport/width width
                :viewport/height 1
                :foreground-color TextColor$ANSI/YELLOW})))

(defn questions-pane-body-flow
  ""
  [{:as world
    :keys [questions selected-question-index active-pane]}]
  (let [question (questions selected-question-index)
        offset (get-in world [:line-offsets (question "question_id") active-pane])]
    (flow/y-scroll
      (flow/add
        (question-flow question world)
        (comments-flow question world))
      (- offset))))

(defn answers-pane-frame-flow
  ""
  [{:as world
    :keys [width questions selected-question-index]}]
  (let [{:keys [top]} (answers-pane-body-dimensions world)
        question (questions selected-question-index)]
    (flow/add
      (flow/make {:type :string
                  :raw (question "title")
                  :viewport/left 2
                  :viewport/width (- width 4)
                  :viewport/height 2
                  :modifiers [SGR/REVERSE]})
      (flow/make {:type :string
                  :raw (format-answers-pane-separator question world)
                  :viewport/width width
                  :viewport/height 2}))))

(defn answer-flow
  ""
  [answer
   {:as world
    :keys [questions selected-question-index active-pane]}]
  (let [{:keys [left top width height]} (answers-pane-body-dimensions world)]
    (flow/make {:type :markdown
                :raw (answer "body_markdown")
                :viewport/left left
                :viewport/top top
                :viewport/width width
                :viewport/height height})))

(defn answer-meta-flow
  ""
  [world]
  (let [{:keys [top height]} (answers-pane-body-dimensions world)
        answer (selected-answer world)
        text (format-answer-meta answer)
        length (count text)]
    (flow/make {:type :string
                :raw text
                :viewport/left (- (world :width) length 1)
                :viewport/top (+ top height)
                :viewport/width length
                :viewport/height 1
                :foreground-color TextColor$ANSI/YELLOW})))

(defn answer-acceptance-flow
  ""
  [world]
  (let [{:keys [top height]} (answers-pane-body-dimensions world)
        text " ACCEPTED "]
    (flow/make {:type :string
                :raw (if ((selected-answer world) "is_accepted") text "")
                :viewport/left 1
                :viewport/top (+ top height)
                :viewport/width (count text)
                :viewport/height 1
                :foreground-color TextColor$ANSI/BLACK
                :background-color TextColor$ANSI/YELLOW})))

(defn answers-pane-body-flow
  ""
  [{:as world
    :keys [questions selected-question-index active-pane]}]
  (let [question (questions selected-question-index)
        answer (selected-answer world)
        offset (get-in world [:line-offsets (question "question_id") active-pane])]
    (flow/y-scroll
      (flow/add
        (answer-flow answer world)
        (comments-flow answer world))
      (- offset))))

(defn question-line-count
  ""
  [question world]
  (flow/line-count
    (flow/add
      (question-flow question world)
      (comments-flow question world))))

(defn answer-line-count
  ""
  [answer world]
  (flow/line-count
    (flow/add
      (answer-flow answer world)
      (comments-flow answer world))))

