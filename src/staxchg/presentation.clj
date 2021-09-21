(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.presentation.state :as state])
  (:require [staxchg.presentation.flow :as presentation.flow])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.recipe :as recipe])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def search-legend (string/join "\n" ["         [tag] search within a tag"
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
    {:questions-header {:left question-list-left
                        :top 0
                        :width (- width (* question-list-left 2))
                        :height question-list-size
                        :clear? switched-pane?}
     :questions-separator {:left 0
                           :top question-list-size
                           :width width
                           :height 1}
     :questions-body {:left questions-body-left
                      :top questions-body-top
                      :width (- width (* questions-body-left 2))
                      :height (- height questions-body-top 1)
                      :clear? (or switched-pane? switched-question?)}
     :questions-footer {:left 0
                        :top (dec height)
                        :width width
                        :height 1
                        :clear? (or switched-pane? switched-question?)}
     :answers-body {:left answer-body-left
                    :top answer-body-top
                    :width (- width (* answer-body-left 2))
                    :height (- height answer-body-top 1)
                    :clear? (or switched-pane? switched-answer?)}
     :answers-footer-left {:left 0
                           :top (dec height)
                           :width answers-footer-left-width
                           :height 1
                           :clear? (or switched-pane? switched-answer?)}
     :answers-footer-right {:left answers-footer-left-width
                            :top (dec height)
                            :width (- width answers-footer-left-width)
                            :height 1
                            :clear? (or switched-pane? switched-answer?)}
     :answers-header {:left answers-header-left
                      :top 0
                      :width (- width (* answers-header-left 2))
                      :height answers-header-height}
     :answers-separator {:left 0
                         :top answers-header-height
                         :width width
                         :height (inc answers-separator-height)
                         :clear? switched-pane?}
     :full-screen {:left 0
                   :top 0
                   :width width
                   :height height
                   :clear? switched-pane?}}))

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
                    (contains? traits :horz) horz-bar
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
(def frame-txt #(.withForegroundColor % frame-color))
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
                    :hilite-attribute (comp bold-txt blue-txt)
                    :hilite-region-marker yellow-txt
                    :hilite-information (comp bold-txt green-txt)
                    :hilite-warning (comp bold-txt yellow-txt)
                    :hilite-alert (comp bold-txt red-txt)
                    :hilite-error (comp bold-txt red-txt)
                    :comment cyan-txt
                    :h (comp bold-txt yellow-txt)
                    :frame frame-txt
                    :meta-answers (comp bold-txt frame-txt)
                    :meta-score (comp bold-txt green-txt)
                    :meta-views (comp bold-txt white-txt)
                    :meta-reputation bold-txt])

(defn apply-markdown-traits
  ""
  [[character xy {:keys [traits] :as extras}]]
  [(apply markdown/decorate character traits trait-clauses)
   xy
   extras])

(def string-groomer (comp string/join
                          #(filter printable? %)))

(def plot-groomer (partial eduction (comp (filter (comp printable? first))
                                          (map replace-with-symbols)
                                          (map convert-to-lanterna)
                                          (map apply-markdown-traits))))

(defmulti groom-recipe-item :function)

(defmethod groom-recipe-item :staxchg.io/put-string!
  [item]
  (update-in item [:params 1] string-groomer))

(defmethod groom-recipe-item :staxchg.io/put-plot!
  [item]
  (update-in item [:params 1] plot-groomer))

(defmethod groom-recipe-item :default
  [item]
  item)

(defn groom-recipe
  ""
  [recipe]
  (map groom-recipe-item recipe))

(defn flows
  ""
  [world zone]
  (let [question (state/selected-question world)
        answer (state/selected-answer world)]
    (cond-> {:questions-separator (presentation.flow/questions-separator world zone)
             :questions-list (presentation.flow/questions-list world zone)
             :answers-header (presentation.flow/answers-header world zone)
             :answers-separator (presentation.flow/answers-separator world zone)
             :empty (presentation.flow/dummy world zone)}
      question (assoc :question-body (presentation.flow/questions-body world zone)
                      :question-meta (presentation.flow/question-meta world zone))
      answer (assoc :answer (presentation.flow/answers-body world zone)
                    :answer-meta (presentation.flow/answer-meta answer world zone)
                    :answer-acceptance (presentation.flow/answer-acceptance answer world)))))

(def consignments
  [{:pane :questions :flow-id :empty               :zone-id :full-screen}
   {:pane :questions :flow-id :questions-separator :zone-id :questions-separator}
   {:pane :questions :flow-id :questions-list      :zone-id :questions-header}
   {:pane :questions :flow-id :question-body       :zone-id :questions-body}
   {:pane :questions :flow-id :question-meta       :zone-id :questions-footer}
   {:pane   :answers :flow-id :empty               :zone-id :full-screen}
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
      (presentation.flow/commented-post question world zone)
      zone)))

(defn answer-line-count
  ""
  [answer world]
  (let [zone (zone-for-flow-id :answer world)]
    (flow/line-count
      (presentation.flow/commented-post answer world zone)
      zone)))

(defn post-line-count
  ""
  [post world]
  ((cond
     (contains? post "answer_id") answer-line-count
     (contains? post "question_id") question-line-count
     :else (constantly 0)) post world))

(def refresh-recipe [{:function :staxchg.io/refresh!
                      :params [:screen]}])

(defn recipes
  ""
  [{:as world
    :keys [active-pane]}]
  (let [zones (zones world)
        inflate-input (fn [{:keys [flow-id zone-id]}]
                        (let [zone (zones zone-id)]
                          {:flow ((flows world zone) flow-id)
                           :zone zone}))
        backbuffer-recipes (->> consignments
                                (filter (comp (partial = active-pane) :pane))
                                (map inflate-input)
                                (remove (comp nil? :flow))
                                (map recipe/make)
                                (map groom-recipe))]
    (concat backbuffer-recipes [refresh-recipe])))

