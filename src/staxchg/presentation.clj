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
    (partial + 1)
    (->>
      (question "comments")
      (map #(vector % (- width question-comments-left-margin)))
      (concat [[question width]])
      (map #(vector ((first %) "body_markdown") (second %)))
      (map #(markdown/line-count (first %) (second %))))))

