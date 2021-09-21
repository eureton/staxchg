(ns staxchg.presentation.flow
  (:require [clojure.string :as string])
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.presentation.state :as state])
  (:require [staxchg.presentation.render :as render])
  (:require [staxchg.post :as post])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.dev :as dev])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(defn question-list-item
  ""
  [question
   index
   {:as world
    :keys [selected-question-index question-list-offset]}
   {:as zone
    :keys [width]}]
  (let [x-offset 1
        text (render/question-list-item question (- width (* x-offset 2)))
        selected? (= index (- selected-question-index question-list-offset))]
    (flow/make {:type :string
                :raw text
                :x x-offset
                :modifiers (if selected? [SGR/REVERSE] [])})))

(def comments-left-margin 16)

(defn single-comment
  ""
  [{:as c
    :strs [body_markdown]}
   {:as zone
    :keys [width]}]
  (let [meta-text (render/comment-meta c)]
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

(defn comments
  ""
  [{:as post :strs [comments]}
   world
   zone]
  (reduce
    #(flow/add %1 flow/y-separator %2)
    flow/zero
    (map #(single-comment % zone) comments)))

(defn body
  ""
  [{:as post :strs [body_markdown]}
   world]
  (let [id (post/id post)]
    (flow/make {:type :markdown
                :raw body_markdown
                :scroll-delta (get-in world [:scroll-deltas id])
                :highlights (get-in world [:highlights id])})))

(defn answer-meta
  ""
  [post world zone]
  (let [text (render/answer-meta post)
        filler (repeat (- (zone :width) (count text)) [\space #{}])]
    (flow/make {:type :characters
                :raw (concat filler text)})))

(defn answer-acceptance
  ""
  [{:strs [is_accepted]} world]
  (let [base {:type :string :x 1}
        acceptance-text " ACCEPTED "]
    (flow/make (merge base (if is_accepted
                             {:raw acceptance-text
                              :foreground-color TextColor$ANSI/BLACK
                              :background-color frame-color}
                             {:raw (string/join (repeat (count acceptance-text)
                                                        \space))
                              :foreground-color TextColor$ANSI/DEFAULT
                              :background-color TextColor$ANSI/DEFAULT})))))

(defn questions-separator
  ""
  [world zone]
  (flow/make {:type :string
              :raw (render/questions-separator world zone)
              :foreground-color frame-color}))

(defn visible-questions
  ""
  [{:as world
    :keys [questions question-list-size question-list-offset]}]
  (subvec questions
          question-list-offset
          (min (count questions) (+ question-list-offset question-list-size))))

(defn questions-list
  ""
  [world zone]
  (reduce flow/add
          flow/zero
          (map-indexed #(question-list-item %2 %1 world zone)
                       (visible-questions world))))

(defn answers-header
  ""
  [world _]
  (let [{:strs [title]} (state/selected-question world)]
    (flow/make {:type :string
                :raw title
                :modifiers [SGR/REVERSE]})))

(defn answers-separator
  ""
  [world zone]
  (let [selected-question (state/selected-question world)
        selected-answer (state/selected-answer selected-question world)]
    (flow/make {:type :string
                :raw (render/answers-separator selected-question
                                               selected-answer
                                               zone)
                :foreground-color frame-color})))

(defn commented-post
  ""
  [post world zone]
  (flow/add (body post world)
            (comments post world zone)))

(defn questions-body
  ""
  [world zone]
  (let [post (state/selected-question world)]
    (flow/scroll-y (commented-post post world zone)
                   (- (state/line-offset post world)))))

(defn question-meta
  ""
  [world zone]
  (let [text (-> world state/selected-question render/question-meta)]
    (flow/make {:type :characters
                :raw text
                :x (- (:width zone) (count text))})))

(defn answers-body
  ""
  [world zone]
  (let [post (state/selected-answer world)]
    (flow/scroll-y (commented-post post world zone)
                   (- (state/line-offset post world)))))

(defn dummy
  ""
  [_ _]
  {:scroll-offset 0
   :items []})

