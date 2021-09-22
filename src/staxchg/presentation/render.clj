(ns staxchg.presentation.render
  (:require [clojure.string :as string])
  (:require [flatland.useful.fn :as ufn])
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.dev :as dev])
  (:import java.time.LocalDateTime)
  (:import java.time.ZoneOffset)
  (:gen-class))

(defn traitful-char-seq
  ""
  [token & traits]
  (map vector (str token) (repeat {:traits (set traits)})))

(defn date
  ""
  [unixtime]
  (when unixtime
    (let [datetime (LocalDateTime/ofEpochSecond unixtime 0 ZoneOffset/UTC)]
      (format "%4d-%02d-%02d %02d:%02d"
              (.getYear datetime)
              (.getValue (.getMonth datetime))
              (.getDayOfMonth datetime)
              (.getHour datetime)
              (.getMinute datetime)))))

(defn author
  ""
  [{:as author
    :strs [display_name reputation]}]
  (mapcat (ufn/ap traitful-char-seq)
          [[display_name :frame]
           [" (" :frame]
           [reputation :frame :meta-reputation]
           [")" :frame]]))

(defn question-stats
  ""
  [{:strs [answer_count score view_count]}]
  (let [divider ["/" :frame]]
    (mapcat (ufn/ap traitful-char-seq)
            [["A" :meta-answers] divider
             ["S" :meta-score] divider
             ["V" :meta-views] [": " :frame]
             [answer_count :meta-answers] divider
             [score :meta-score] divider
             [view_count :meta-views]])))

(def meta-divider
  ""
  (traitful-char-seq " | " :frame))

(defn question-meta
  ""
  [question]
  (when-some [{:strs [last_activity_date owner]} question]
    (concat (question-stats question)
            meta-divider
            (author owner)
            meta-divider
            (traitful-char-seq (date last_activity_date) :frame))))

(defn answer-meta
  ""
  [answer]
  (when-some [{:strs [score last_activity_date owner]} answer]
    (concat (traitful-char-seq score :frame)
            meta-divider
            (author owner)
            meta-divider
            (traitful-char-seq (date last_activity_date) :frame))))

(defn comment-meta
  ""
  [{:as post :strs [owner score creation_date]}]
  (format
    "%d | %s | %s"
    score
    (->> owner author (map first) string/join)
    (date creation_date)))

(defn question-list-item
  ""
  [question width]
  (format (str "%-" width "s")
          (question "title")))

(defn separator
  ""
  [width hint]
  (format "%s%s%s"
          (string/join (repeat (- width (count hint) 1) horz-bar))
          hint
          (str horz-bar)))

(defn questions-separator
  ""
  [{:keys [question-list-size question-list-offset questions]}
   {:keys [width]}]
  (let [from (inc question-list-offset)
        to (dec (+ from question-list-size))
        hint (format "(%d-%d of %d)" from to (count questions))]
    (separator width hint)))

(defn answers-separator
  ""
  [{:as question :strs [answer_count answers]}
   {:strs [answer_id]}
   {:as zone :keys [width]}]
  (let [index (->> answers
                   (map #(get % "answer_id"))
                   (map-indexed vector)
                   (some (fn [[i id]] (when (= id answer_id) i))))
        hint (if (nil? index)
               "(question has no answers)"
               (format "(%d of %d)"
                       (inc index)
                       answer_count))]
    (separator width hint)))

