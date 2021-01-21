(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.recipe :as recipe])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def acceptance-text " ACCEPTED ")

(def search-legend (clojure.string/join \newline ["         [tag] search within a tag"
                                                  "     user:1234 seach by author"
                                                  "  \"words here\" exact phrase"
                                                  "     answers:0 unanswered questions"
                                                  "       score:3 posts with a 3+ score"
                                                  "isaccepted:yes seach within status"]))

(defn zones
  ""
  [{:as world
    :keys [width height switched-pane? switched-question? switched-answer?]}]
  (let [question-list-left 0
        question-list-size 2
        questions-body-left 1
        questions-body-top (inc question-list-size)
        answers-header-left 1
        answers-header-height 1
        answers-separator-height 1
        answer-body-left 1
        answer-body-top (+ answers-header-height answers-separator-height)
        answers-footer-left-width (/ width 3)]
    {:questions-header {:id :questions-header
                        :left question-list-left
                        :top 0
                        :width (- width (* question-list-left 2))
                        :height question-list-size
                        :clear? switched-pane?}
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
                      :clear? (or switched-pane? switched-question?)}
     :questions-footer {:id :questions-footer
                        :left 0
                        :top (dec height)
                        :width width
                        :height 1
                        :clear? (or switched-pane? switched-question?)}
     :answers-body {:id :answers-body
                    :left answer-body-left
                    :top answer-body-top
                    :width (- width (* answer-body-left 2))
                    :height (- height answer-body-top 1)
                    :clear? (or switched-pane? switched-answer?)}
     :answers-footer-left {:id :answers-footer-left
                           :left 0
                           :top (dec height)
                           :width answers-footer-left-width
                           :height 1
                           :clear? (or switched-pane? switched-answer?)}
     :answers-footer-right {:id :answers-footer-right
                            :left answers-footer-left-width
                            :top (dec height)
                            :width (- width answers-footer-left-width)
                            :height 1
                            :clear? (or switched-pane? switched-answer?)}
     :answers-header {:id :answers-header
                      :left answers-header-left
                      :top 0
                      :width (- width (* answers-header-left 2))
                      :height answers-header-height}
     :answers-separator {:id :answers-separator
                         :left 0
                         :top answers-header-height
                         :width width
                         :height (inc answers-separator-height)
                         :clear? switched-pane?}}))

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
  [{:as question :strs [answers score view_count last_activity_date]}]
  (format
    "(A: %d) | (S: %d) | (V: %d) | %s | %s"
    (count answers)
    score
    view_count
    (format-author question)
    (format-date last_activity_date)))

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
      (apply str (repeat (- width (count hint) 1) Symbols/SINGLE_LINE_HORIZONTAL))
      hint
      (str Symbols/SINGLE_LINE_HORIZONTAL))))

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
      (apply str (repeat (- width (count hint) 1) Symbols/SINGLE_LINE_HORIZONTAL))
      hint
      (str Symbols/SINGLE_LINE_HORIZONTAL))))

(defn question-list-item-flow
  ""
  [question
   index
   {:as world
    :keys [selected-question-index question-list-offset]}]
  (let [x-offset 1
        {:keys [width]} ((zones world) :questions-header)
        text (format-question-list-item question (- width (* x-offset 2)))
        selected? (= index (- selected-question-index question-list-offset))]
    (flow/make {:type :string
                :raw text
                :x x-offset
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

(defn answer-meta-flow
  ""
  [answer world]
  (let [text (format-answer-meta answer)
        zone ((zones world) :answers-footer-right)] ; TODO remove this and all others like it
                                                    ;      flows must know nothing about which
                                                    ;      zone they will be rendered in!
    (flow/make {:type :string
                :raw (format (str "%" (zone :width) "s") text)
                :foreground-color TextColor$ANSI/YELLOW})))

(defn answer-acceptance-flow
  ""
  [answer world]
  (let [base {:type :string :x 1}]
    (flow/make (merge base (if (answer "is_accepted")
                             {:raw acceptance-text
                              :foreground-color TextColor$ANSI/BLACK
                              :background-color TextColor$ANSI/YELLOW}
                             {:raw (string/join (repeat (count acceptance-text) \space))
                              :foreground-color TextColor$ANSI/DEFAULT
                              :background-color TextColor$ANSI/DEFAULT})))))

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
        question-meta-text (format-question-meta question)]
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
     :answer-meta (answer-meta-flow answer world)
     :answer-acceptance (answer-acceptance-flow answer world)
     :answers-header (flow/make {:type :string
                                 :raw (question "title")
                                 :modifiers [SGR/REVERSE]})
     :answers-separator (flow/make {:type :string
                                    :raw (format-answers-pane-separator question world)})}))

(def consignments
  [{:pane :questions :flow :questions-separator :zone :questions-separator}
   {:pane :questions :flow :questions-list      :zone :questions-header}
   {:pane :questions :flow :question-body       :zone :questions-body}
   {:pane :questions :flow :question-meta       :zone :questions-footer}
   {:pane   :answers :flow :answers-separator   :zone :answers-separator}
   {:pane   :answers :flow :answer              :zone :answers-body}
   {:pane   :answers :flow :answer-meta         :zone :answers-footer-right}
   {:pane   :answers :flow :answer-acceptance   :zone :answers-footer-left}
   {:pane   :answers :flow :answers-header      :zone :answers-header}])

(defn recipes [world]
  (let [flows (flows world)
        zones (zones world)]
    (->>
      consignments
      (filter (comp (partial = (world :active-pane)) :pane))
      (map #(hash-map :flow (flows (% :flow)) :zone (zones (% :zone))))
      (map recipe/make))))

