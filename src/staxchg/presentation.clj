(ns staxchg.presentation
  (:require [staxchg.markdown :as markdown])
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

(defn question-pane-body-dimensions
  [{:as world
    :keys [width height question-list-size]}]
  (let [left 1
        top (inc question-list-size)]
    {:left left
     :top top
     :width (- width (* left 2))
     :height (- height top 1)}))

(def question-comments-left-margin 4)

(defn question-line-count
  ""
  [question width]
  (reduce
    (partial + 2)
    (->>
      (question "comments")
      (map #(vector % (- width question-comments-left-margin)))
      (concat [[question width]])
      (map #(vector ((first %) "body_markdown") (second %)))
      (map #(markdown/line-count (first %) (second %))))))

