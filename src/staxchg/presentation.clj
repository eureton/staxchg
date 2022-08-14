(ns staxchg.presentation
  (:require [clojure.string :as string]
            [flatland.useful.fn :as ufn]
            [staxchg.flow :as flow]
            [staxchg.presentation.flow :as presentation.flow]
            [staxchg.presentation.recipe :as presentation.recipe]
            [staxchg.presentation.state :as state]
            [staxchg.presentation.zone :as zone]
            [staxchg.post :as post]
            [staxchg.recipe :as recipe])
  (:gen-class))

(def search-legend
  "Usage guide for querying StackExchange."
  (string/join "\n" ["         [tag] search within a tag"
                     "     user:1234 seach by author"
                     "  \"words here\" exact phrase"
                     "     answers:0 unanswered questions"
                     "       score:3 posts with a 3+ score"
                     "isaccepted:yes seach within status"]))

(def consignments
  "Associates flows to zones."
  [{:pane :questions :flow-id :questions-separator :zone-id :questions-separator}
   {:pane :questions :flow-id :questions-list      :zone-id :questions-header}
   {:pane :questions :flow-id :questions-body      :zone-id :questions-body}
   {:pane :questions :flow-id :question-meta       :zone-id :questions-footer}
   {:pane   :answers :flow-id :answers-separator   :zone-id :answers-separator}
   {:pane   :answers :flow-id :answers-body        :zone-id :answers-body}
   {:pane   :answers :flow-id :answer-meta         :zone-id :answers-footer-right}
   {:pane   :answers :flow-id :answer-acceptance   :zone-id :answers-footer-left}
   {:pane   :answers :flow-id :answers-header      :zone-id :answers-header}])

(defn zone-by-id
  "Zone info for the given id."
  [id world]
  (->> id
       name
       (symbol "staxchg.presentation.zone")
       find-var
       var-get
       (ufn/thrush world)))

(defn flow-by-id
  "Flow info for the given id."
  [id world zone]
  (->> id
       name
       (symbol "staxchg.presentation.flow")
       find-var
       var-get
       ufn/ap
       (ufn/thrush [world zone])))

(defn post-line-count
  "Number of lines post spans, when rendered into the body of the appropriate
   pane. Assumes that horizontal and vertical offsets are zero."
  [post world]
  (if-some [zone (zone-by-id (cond (post/answer? post)   :answers-body
                                   (post/question? post) :questions-body)
                             world)]
    (flow/line-count (presentation.flow/commented-post post world zone)
                     zone)
    0))

(def active-pane-body-height
  "Number of lines the body of the currently active pane (either questions pane
   or answers pane) spans."
  (comp :height
        (ufn/ap zone-by-id)
        (juxt (comp #(case % :questions :questions-body :answers :answers-body)
                    :active-pane)
              identity)))

(defn recipes
  "Sequence of recipes for rendering world."
  [{:as world
    :keys [active-pane]}]
  (let [inflate (fn [{:keys [flow-id zone-id]}]
                  (let [zone (zone-by-id zone-id world)]
                    [(flow-by-id flow-id world zone) zone]))
        backbuffer-recipes (->> consignments
                                (filter (comp (partial = active-pane) :pane))
                                (map inflate)
                                (remove (comp nil? first))
                                (map (ufn/ap recipe/make))
                                (map presentation.recipe/groom))]
    (concat backbuffer-recipes [presentation.recipe/refresh])))

