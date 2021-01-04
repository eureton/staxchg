(ns staxchg.presentation
  (:require [staxchg.plot :as plot])
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

(defn comment-plot
  ""
  [c world]
  (let [{:keys [top width height]} (question-pane-body-dimensions world)
        meta-text (format-comment-meta c)
        base {:foreground-color TextColor$ANSI/BLUE
              :viewport/left comments-left-margin
              :viewport/top top
              :viewport/width (- width comments-left-margin)
              :viewport/height height}]
    (plot/add
      (plot/make (merge base {:type :markdown
                              :payload (c "body_markdown")}))
      (plot/make (merge base {:type :string
                              :payload meta-text
                              :x (- width (count meta-text))
                              :modifiers [SGR/BOLD]})))))

(defn comments-plot
  ""
  [post world]
  (reduce
    #(plot/add %1 plot/y-separator %2)
    plot/zero
    (map
      #(comment-plot % world)
      (post "comments"))))

(defn question-plot
  ""
  [question world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)]
    (plot/make {:type :markdown
                :payload (question "body_markdown")
                :viewport/left left
                :viewport/top top
                :viewport/width width
                :viewport/height height})))

(defn question-meta-plot
  ""
  [question world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)
        text (format-question-meta question)]
    (plot/make {:type :string
                :payload text
                :x (- width (count text))
                :viewport/left left
                :viewport/top (+ top height)
                :viewport/width width
                :viewport/height 1
                :foreground-color TextColor$ANSI/YELLOW})))

(defn question-pane-body-plot
  ""
  [question line-offset world]
  (plot/y-offset
    (plot/add
      (question-plot question world)
      (comments-plot question world))
    (- line-offset)))

(defn question-line-count
  ""
  [question world]
  (plot/line-count
    (plot/add
      (question-plot question world)
      (comments-plot question world))))

