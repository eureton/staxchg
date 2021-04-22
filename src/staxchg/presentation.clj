(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.recipe :as recipe])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TextCharacter)
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
        answers-footer-left-width (quot width 3)]
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

(defn line-offset
  ""
  [post
   {:as world
    :keys [active-pane]}]
  (let [id-key (case active-pane :questions "question_id" :answers "answer_id")
        post-id (get post id-key)]
   (or (get-in world [:line-offsets post-id])
       0)))

(defn selected-question
  ""
  [{:as world
    :keys [selected-question-index]}]
  (get-in world [:questions selected-question-index]))

(defn selected-answer
  ([{:as question :strs [question_id answers]}
    world]
   (let [answer-id (or (get-in world [:selected-answers question_id])
                       (-> answers (get 0) (get "answer_id")))]
     (some
       #(when (= answer-id (% "answer_id")) %)
       answers)))
  ([world]
   (selected-answer (selected-question world) world)))

(defn format-date
  ""
  [unixtime]
  (if (nil? unixtime)
    nil
    (let [datetime (java.time.LocalDateTime/ofEpochSecond unixtime 0 java.time.ZoneOffset/UTC)]
      (format
        "%4d-%02d-%02d %02d:%02d"
        (.getYear datetime)
        (.getValue (.getMonth datetime))
        (.getDayOfMonth datetime)
        (.getHour datetime)
        (.getMinute datetime)))))

(defn format-author
  ""
  [{:as author
    :strs [display_name reputation]}]
  (format "%s (%s)" display_name reputation))

(defn format-question-meta
  ""
  [{:strs [answer_count score view_count last_activity_date owner]}]
  (format
    "(A: %d) | (S: %d) | (V: %d) | %s | %s"
    answer_count
    score
    view_count
    (format-author owner)
    (format-date last_activity_date)))

(defn format-answer-meta
  ""
  [{:as answer :strs [score last_activity_date owner]}]
  (format
    "%d | %s | %s"
    score
    (format-author owner)
    (format-date last_activity_date)))

(defn format-comment-meta
  ""
  [{:as post :strs [owner score creation_date]}]
  (format
    "%d | %s | %s"
    score
    (format-author owner)
    (format-date creation_date)))

(defn format-question-list-item
  ""
  [question width]
  (format
    (str "%-" width "s")
    (question "title")))

(defn format-questions-pane-separator
  ""
  [{:keys [question-list-size question-list-offset questions]}
   {:keys [width]}]
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
  [{:as question :strs [question_id answer_count answers]}
   world
   {:as zone :keys [width]}]
  (let [{:strs [answer_id]} (selected-answer question world)
        index (->>
                answers
                (map #(get % "answer_id"))
                (map-indexed vector)
                (some (fn [[i id]] (when (= id answer_id) i))))
        hint (if (nil? index)
               "(question has no answers)"
               (format
                 "(%d of %d)"
                 (inc index)
                 answer_count))]
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
    :keys [selected-question-index question-list-offset]}
   {:as zone
    :keys [width]}]
  (let [x-offset 1
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

(def comments-left-margin 16)

(defn comment-flow
  ""
  [{:as c
    :strs [body_markdown]}
   {:as zone
    :keys [width]}]
  (let [meta-text (format-comment-meta c)]
    (flow/add
      (flow/make {:type :markdown
                  :raw body_markdown
                  :sub-zone (->
                              zone
                              (assoc :left comments-left-margin)
                              (update :width - comments-left-margin))
                  :foreground-color TextColor$ANSI/BLUE})
      (flow/make {:type :string
                  :raw meta-text
                  :x (- width (count meta-text))
                  :foreground-color TextColor$ANSI/BLUE
                  :modifiers [SGR/BOLD]}))))

(defn comments-flow
  ""
  [{:as post
    :strs [comments]}
   world
   zone]
  (reduce
    #(flow/add %1 flow/y-separator %2)
    flow/zero
    (map #(comment-flow % zone) comments)))

(defn question-flow
  ""
  [{:as question
    :strs [body_markdown question_id]
    :keys [code-highlights]} world]
  (flow/make {:type :markdown
              :raw body_markdown
              :scroll-delta (get-in world [:scroll-deltas question_id])
              :code-highlights code-highlights}))

(defn answer-flow
  ""
  [{:as answer :strs [body_markdown answer_id]} world]
  (flow/make {:type :markdown
              :raw body_markdown
              :scroll-delta (get-in world [:scroll-deltas answer_id])}))

(defn questions-body-flow
  ""
  [question world zone]
  (flow/add
    (question-flow question world)
    (comments-flow question world zone)))

(defn answer-meta-flow
  ""
  [answer world zone]
  (let [text (format-answer-meta answer)]
    (flow/make {:type :string
                :raw (format (str "%" (zone :width) "s") text)
                :foreground-color TextColor$ANSI/YELLOW})))

(defn answer-acceptance-flow
  ""
  [{:as answer :strs [is_accepted]} world]
  (let [base {:type :string :x 1}]
    (flow/make (merge base (if is_accepted
                             {:raw acceptance-text
                              :foreground-color TextColor$ANSI/BLACK
                              :background-color TextColor$ANSI/YELLOW}
                             {:raw (string/join (repeat (count acceptance-text) \space))
                              :foreground-color TextColor$ANSI/DEFAULT
                              :background-color TextColor$ANSI/DEFAULT})))))

(defn answers-body-flow
  ""
  [answer world zone]
  (flow/add
    (answer-flow answer world)
    (comments-flow answer world zone)))

(defn printable?
  ""
  [character]
  (->>
    character
    ((juxt #(TerminalTextUtils/isControlCharacter %)
           #(Character/isHighSurrogate %)))
    (every? false?)))

(defn replace-with-symbols
  ""
  [[character xy {:keys [traits] :as extras}]]
  (let [rewritten (cond
                    (contains? traits :bullet) Symbols/BULLET
                    (contains? traits :horz) Symbols/SINGLE_LINE_HORIZONTAL
                    :else character)]
    [rewritten xy extras]))

(defn convert-to-lanterna
  ""
  [plot-item]
  (update plot-item 0 #(TextCharacter. %)))

; helper variables for composing lanterna characters
(def bold-txt #(.withModifier % SGR/BOLD))
(def reverse-txt #(.withModifier % SGR/REVERSE))
(def green-txt #(.withForegroundColor % TextColor$ANSI/GREEN))
(def cyan-txt #(.withForegroundColor % TextColor$ANSI/CYAN))
(def red-txt #(.withForegroundColor % TextColor$ANSI/RED))
(def white-txt #(.withForegroundColor % TextColor$ANSI/WHITE))
(def magenta-txt #(.withForegroundColor % TextColor$ANSI/MAGENTA))
(def yellow-txt #(.withForegroundColor % TextColor$ANSI/YELLOW))
(def blue-txt #(.withForegroundColor % TextColor$ANSI/BLUE))
(def trait-clauses [:strong bold-txt
                    :em reverse-txt
                    :code green-txt
                    :standout green-txt
                    :hilite-comment cyan-txt
                    :hilite-built-in red-txt
                    :hilite-function (comp bold-txt white-txt)
                    :hilite-documentation cyan-txt
                    :hilite-keyword (comp bold-txt green-txt)
                    :hilite-data-type (comp bold-txt white-txt)
                    :hilite-dec-val (comp bold-txt magenta-txt)
                    :hilite-base-n (comp bold-txt magenta-txt)
                    :hilite-float (comp bold-txt magenta-txt)
                    :hilite-constant (comp bold-txt magenta-txt)
                    :hilite-char (comp bold-txt yellow-txt)
                    :hilite-special-char (comp bold-txt red-txt)
                    :hilite-string (comp bold-txt green-txt)
                    :hilite-verbatim-string (comp bold-txt green-txt)
                    :hilite-special-string (comp bold-txt green-txt)
                    :hilite-import red-txt
                    :hilite-annotation yellow-txt
                    :hilite-comment-var (comp bold-txt white-txt)
                    :hilite-other blue-txt
                    :hilite-variable white-txt
                    :hilite-control-flow red-txt
                    :hilite-operator yellow-txt
                    :hilite-extension blue-txt
                    :hilite-preprocessor red-txt
                    :hilite-attribute
                    :hilite-region-marker yellow-txt
                    :hilite-information (comp bold-txt green-txt)
                    :hilite-warning (comp bold-txt yellow-txt)
                    :hilite-alert (comp bold-txt red-txt)
                    :hilite-error (comp bold-txt red-txt)
                    :comment cyan-txt
                    :h (comp bold-txt magenta-txt)])

(defn apply-markdown-traits
  ""
  [[character xy {:keys [traits] :as extras}]]
  [(apply markdown/decorate character traits trait-clauses)
   xy
   extras])

(def string-groomer (comp string/join
                          (partial filter printable?)))

(def markdown-groomer (partial eduction (comp (filter (comp printable? first))
                                              (map replace-with-symbols)
                                              (map convert-to-lanterna)
                                              (map apply-markdown-traits))))

(defmulti groom-recipe-item :function)

(defmethod groom-recipe-item :staxchg.io/put-string!
  [item]
  (update-in item [:params 1] string-groomer))

(defmethod groom-recipe-item :staxchg.io/put-markdown!
  [item]
  (update-in item [:params 1] markdown-groomer))

(defmethod groom-recipe-item :default
  [item]
  item)

(defn groom-recipe
  ""
  [recipe]
  (map groom-recipe-item recipe))

(defn flows
  ""
  [world
   {:as zone
    :keys [width]}]
  (let [{:as question :strs [title]} (selected-question world)
        {:as answer :strs [answer_id]} (selected-answer world)
        question-meta-text (format-question-meta question)]
    (cond-> {:questions-separator (flow/make {:type :string
                                              :raw (format-questions-pane-separator world zone)})
             :questions-list (reduce
                               flow/add
                               flow/zero
                               (map-indexed
                                 #(question-list-item-flow %2 %1 world zone)
                                 (visible-questions world)))
             :answers-header (flow/make {:type :string
                                         :raw title
                                         :modifiers [SGR/REVERSE]})
             :answers-separator (flow/make {:type :string
                                            :raw (format-answers-pane-separator question world zone)})}
      (some? question) (assoc :question-body (flow/scroll-y
                                               (questions-body-flow question world zone)
                                               (- (line-offset question world)))
                              :question-meta (flow/make {:type :string
                                                         :raw question-meta-text
                                                         :x (- width (count question-meta-text))
                                                         :foreground-color TextColor$ANSI/YELLOW}))
      (some? answer) (assoc :answer (flow/scroll-y
                                      (answers-body-flow answer world zone)
                                      (- (line-offset answer world)))
                            :answer-meta (answer-meta-flow answer world zone)
                            :answer-acceptance (answer-acceptance-flow answer world)))))

(def consignments
  [{:pane :questions :flow-id :questions-separator :zone-id :questions-separator}
   {:pane :questions :flow-id :questions-list      :zone-id :questions-header}
   {:pane :questions :flow-id :question-body       :zone-id :questions-body}
   {:pane :questions :flow-id :question-meta       :zone-id :questions-footer}
   {:pane   :answers :flow-id :answers-separator   :zone-id :answers-separator}
   {:pane   :answers :flow-id :answer              :zone-id :answers-body}
   {:pane   :answers :flow-id :answer-meta         :zone-id :answers-footer-right}
   {:pane   :answers :flow-id :answer-acceptance   :zone-id :answers-footer-left}
   {:pane   :answers :flow-id :answers-header      :zone-id :answers-header}])

(defn zone-for-flow-id
  ""
  [id world]
  (->> consignments
       (filter (comp #(= id %) :flow-id))
       first
       :zone-id
       ((zones world))))

(defn question-line-count
  ""
  [question world]
  (let [zone (zone-for-flow-id :question-body world)]
    (flow/line-count
      (questions-body-flow question world zone)
      zone)))

(defn answer-line-count
  ""
  [answer world]
  (let [zone (zone-for-flow-id :answer world)]
    (flow/line-count
      (answers-body-flow answer world zone)
      zone)))

(defn post-line-count
  ""
  [post world]
  ((cond
     (contains? post "answer_id") answer-line-count
     (contains? post "question_id") question-line-count
     :else (constantly 0)) post world))

(defn recipes
  ""
  [{:as world
    :keys [active-pane]}]
  (let [zones (zones world)]
    (->>
      consignments
      (filter (comp (partial = active-pane) :pane))
      (map (fn [{:keys [flow-id zone-id]}]
             (let [zone (zones zone-id)]
               {:flow ((flows world zone) flow-id)
                :zone zone})))
      (remove (comp nil? :flow))
      (map recipe/make)
      (map groom-recipe))))

