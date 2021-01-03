(ns staxchg.presentation
  (:require [staxchg.markdown :as markdown])
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

(defn comments-plot
  ""
  [post line-offset world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)
        left comments-left-margin
        width (- width comments-left-margin)
        comments (post "comments")
        question-line-count (markdown/line-count (post "body_markdown") width)] ; TODO move this into plot/add
                                                                                ; and make comments-plot applicable
                                                                                ; to answers
    (loop [i 0
           y (- (inc question-line-count) line-offset)
           plot plot/zero]
      (if (>= i (count comments))
        plot
        (let [comm (nth comments i)
              body (comm "body_markdown")
              line-count (markdown/line-count body width)
              meta-y (+ y line-count)
              meta-text (format-comment-meta comm)
              base {:foreground-color TextColor$ANSI/BLUE
                    :viewport/left left
                    :viewport/top top
                    :viewport/width width
                    :viewport/height height}]
          (recur
            (inc i)
            (+ meta-y 2)
            (plot/add
              plot
              (plot/make (merge base {:type :markdown
                                      :payload body
                                      :x 0
                                      :y y}))
              (plot/make (merge base {:type :string
                                      :payload meta-text
                                      :x (- width (count meta-text))
                                      :y meta-y
                                      :modifiers [SGR/BOLD]})))))))))

(defn question-plot
  ""
  [question line-offset world]
  (let [{:keys [left top width height]} (question-pane-body-dimensions world)]
    (plot/add
      (plot/make {:type :markdown
                  :payload (question "body_markdown")
                  :viewport/left left
                  :viewport/top top
                  :viewport/width width
                  :viewport/height height
                  :y (- line-offset)})
      (plot/make {:type :string
                  :payload (format-question-meta question)
                  :viewport/left left
                  :viewport/top (+ top height)
                  :viewport/width width
                  :viewport/height 1
                  :foreground-color TextColor$ANSI/YELLOW}))))

(defn question-pane-plot
  ""
  [question line-offset world]
  (let [{:keys [height]} (question-pane-body-dimensions world)]
    (plot/add
      (question-plot question line-offset world)
      (comments-plot question line-offset world))))

(defn question-line-count
  ""
  [question world]
  (let [plot (plot/add
               (question-plot question 0 world)
               (comments-plot question 0 world))
        bottom-y (fn [{:keys [type y payload] :viewport/keys [width]}]
                   (+ y (condp = type
                          :markdown (markdown/line-count payload width)
                          :string 1)))]
    (->> plot (map bottom-y) (apply max))))
