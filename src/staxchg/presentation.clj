(ns staxchg.presentation
  (:require [staxchg.flow :as flow])
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

(defn format-question-pane-separator
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
                :payload text
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

(defn question-list-flow
  ""
  [world]
  (reduce
    flow/add
    flow/zero
    (map-indexed
      #(question-list-item-flow %2 %1 world)
      (visible-questions world))))

(defn question-pane-separator-flow
  ""
  [{:as world
    :keys [width question-list-size]}]
  (flow/make {:type :string
              :payload (format-question-pane-separator world)
              :viewport/top question-list-size
              :viewport/width width
              :viewport/height 1}))

(defn question-pane-body-dimensions
  [{:as world
    :keys [width height question-list-size]}]
  (let [left 1
        top (inc question-list-size)]
    {:left left
     :top top
     :width (- width (* left 2))
     :height (- height top 1)}))

(def comments-left-margin 4)

(defn comment-flow
  ""
  [c world]
  (let [{:keys [top width height]} (question-pane-body-dimensions world)
        meta-text (format-comment-meta c)
        base {:foreground-color TextColor$ANSI/BLUE
              :viewport/left comments-left-margin
              :viewport/top top
              :viewport/width (- width comments-left-margin)
              :viewport/height height}]
    (flow/add
      (flow/make (merge base {:type :markdown
                              :payload (c "body_markdown")}))
      (flow/make (merge base {:type :string
                              :payload meta-text
                              :x (- width (count meta-text))
                              :modifiers [SGR/BOLD]})))))

(defn comments-flow
  ""
  [post world]
  (reduce
    #(flow/add %1 flow/y-separator %2)
    flow/zero
    (map
      #(comment-flow % world)
      (post "comments"))))

(defn question-flow
  ""
  [question world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)]
    (flow/make {:type :markdown
                :payload (question "body_markdown")
                :viewport/left left
                :viewport/top top
                :viewport/width width
                :viewport/height height})))

(defn question-meta-flow
  ""
  [question world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)
        text (format-question-meta question)]
    (flow/make {:type :string
                :payload text
                :x (- width (count text))
                :viewport/left left
                :viewport/top (+ top height)
                :viewport/width width
                :viewport/height 1
                :foreground-color TextColor$ANSI/YELLOW})))

(defn question-pane-body-flow
  ""
  [question line-offset world]
  (flow/y-offset
    (flow/add
      (question-flow question world)
      (comments-flow question world))
    (- line-offset)))

(defn question-line-count
  ""
  [question world]
  (flow/line-count
    (flow/add
      (question-flow question world)
      (comments-flow question world))))

