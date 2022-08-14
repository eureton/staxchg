(ns staxchg.presentation.render
  (:require [clojure.string :as string]
            [flatland.useful.fn :as ufn]
            [staxchg.presentation.common :refer :all])
  (:import [java.time LocalDateTime ZoneOffset])
  (:gen-class))

(defn traitful-char-seq
  "Sequence of characters in the string representation of token, each coupled
   with traits. Coupling is done via a vector. traits is packaged as a set and
   nested within a hash under the :traits key.

   For example:
     => (traitful-char-seq 42 :abc :xyz)
     ([\\4 {:traits #{:abc :xyz}}] [\\2 {:traits #{:abc :xyz}}])"
  [token & traits]
  (map vector (str token) (repeat {:traits (set traits)})))

(defn date
  "Formats Unix timestamps to a human-readable string."
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
  "Formats post owner structures to a human-readable string."
  [{:as author
    :strs [display_name reputation]}]
  (mapcat (ufn/ap traitful-char-seq)
          [[display_name :frame]
           [" (" :frame]
           [reputation :frame :meta-reputation]
           [")" :frame]]))

(defn question-stats
  "Formats question statistics to a concise, human-readable string."
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
  "Sequence suitable for diving concatenated pieces of text."
  (traitful-char-seq " | " :frame))

(defn question-meta
  "Sequence representing the meta-information of question."
  [question]
  (when-some [{:strs [last_activity_date owner]} question]
    (concat (question-stats question)
            meta-divider
            (author owner)
            meta-divider
            (traitful-char-seq (date last_activity_date) :frame))))

(defn answer-meta
  "Sequence representing the meta-information of answer."
  [answer]
  (when-some [{:strs [score last_activity_date owner]} answer]
    (concat (traitful-char-seq score :frame)
            meta-divider
            (author owner)
            meta-divider
            (traitful-char-seq (date last_activity_date) :frame))))

(defn comment-meta
  "String representing the meta-information of post, where post is a comment."
  [{:as post :strs [owner score creation_date]}]
  (format
    "%d | %s | %s"
    score
    (->> owner author (map first) string/join)
    (date creation_date)))

(defn question-list-item
  "Single-line string summary of question using a maximum of width columns."
  [question width]
  (format (str "%-" width "s")
          (question "title")))

(defn separator
  "String for separating the header from the body in a pane. Uses width columns
   and incorporates the hint string at the end of the right side."
  [width hint]
  (format "%s%s%s"
          (string/join (repeat (- width (count hint) 1) horz-bar))
          hint
          (str horz-bar)))

(defn questions-separator
  "String for separating the header from the body in the questions pane."
  [{:keys [question-list-size question-list-offset questions]}
   {:keys [width]}]
  (let [from (inc question-list-offset)
        to (dec (+ from question-list-size))
        hint (format "(%d-%d of %d)" from to (count questions))]
    (separator width hint)))

(defn answers-separator
  "String for separating the header from the body in the answers pane."
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

