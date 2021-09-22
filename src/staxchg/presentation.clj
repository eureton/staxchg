(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [flatland.useful.fn :as ufn])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.presentation.state :as state])
  (:require [staxchg.presentation.flow :as presentation.flow])
  (:require [staxchg.presentation.zone :as zone])
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

(def consignments
  [{:pane :questions :flow-id :dummy               :zone-id :full-screen}
   {:pane :questions :flow-id :questions-separator :zone-id :questions-separator}
   {:pane :questions :flow-id :questions-list      :zone-id :questions-header}
   {:pane :questions :flow-id :questions-body      :zone-id :questions-body}
   {:pane :questions :flow-id :question-meta       :zone-id :questions-footer}
   {:pane   :answers :flow-id :dummy               :zone-id :full-screen}
   {:pane   :answers :flow-id :answers-separator   :zone-id :answers-separator}
   {:pane   :answers :flow-id :answers-body        :zone-id :answers-body}
   {:pane   :answers :flow-id :answer-meta         :zone-id :answers-footer-right}
   {:pane   :answers :flow-id :answer-acceptance   :zone-id :answers-footer-left}
   {:pane   :answers :flow-id :answers-header      :zone-id :answers-header}])

(defn zone-by-id
  ""
  [id world]
  (->> id
       name
       (symbol "staxchg.presentation.zone")
       find-var
       var-get
       (ufn/thrush world)))

(defn flow-by-id
  ""
  [id world zone]
  (->> id
       name
       (symbol "staxchg.presentation.flow")
       find-var
       var-get
       ufn/ap
       (ufn/thrush [world zone])))

(defn zone-by-flow-id
  ""
  [id world]
  (zone-by-id (->> consignments
                   (filter (comp #{id} :flow-id))
                   first
                   :zone-id)
              world))

(defn question-line-count
  ""
  [question world]
  (let [zone (zone-by-flow-id :questions-body world)]
    (flow/line-count
      (presentation.flow/commented-post question world zone)
      zone)))

(defn answer-line-count
  ""
  [answer world]
  (let [zone (zone-by-flow-id :answers-body world)]
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

(def active-pane-body-height
  ""
  (comp :height
        (ufn/ap zone-by-id)
        (juxt (comp #(case % :questions :questions-body :answers :answers-body)
                    :active-pane)
              identity)))

(defn recipes
  ""
  [{:as world
    :keys [active-pane]}]
  (let [inflate-input (fn [{:keys [flow-id zone-id]}]
                        (let [zone (zone-by-id zone-id world)]
                          {:flow (flow-by-id flow-id world zone)
                           :zone zone}))
        backbuffer-recipes (->> consignments
                                (filter (comp (partial = active-pane) :pane))
                                (map inflate-input)
                                (remove (comp nil? :flow))
                                (map recipe/make)
                                (map presentation.recipe/groom))]
    (concat backbuffer-recipes [presentation.recipe/refresh])))

