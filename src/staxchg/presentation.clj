(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.presentation.state :as state])
  (:require [staxchg.presentation.flow :as presentation.flow])
  (:require [staxchg.presentation.recipe :as presentation.recipe])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.recipe :as recipe])
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
                                (map presentation.recipe/groom))]
    (concat backbuffer-recipes [presentation.recipe/refresh])))

