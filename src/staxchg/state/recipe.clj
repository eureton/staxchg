(ns staxchg.state.recipe
  (:require [clojure.string :as string])
  (:require [staxchg.api :as api])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.recipe.step :as recipe.step])
  (:gen-class))

(def poll-loop-latency
  "The number of milliseconds to sleep for between poll runs."
  500)

(defn highlight-code-step
  ""
  [{:keys [string syntax question-id answer-id]}]
  {:function :staxchg.io/run-highlight.js!
   :params [string syntax question-id answer-id]})

(defn input-df
  ""
  [{:as world
    :keys [query? questions search-term fetch-answers no-questions no-answers
           fetch-failed snippets quit? width]
    {:keys [screen]} :io/context}]
  (cond
    (nil? screen) :initialize
    (nil? width) :enable-screen
    snippets :snippets
    search-term :search-term
    fetch-answers :fetch-answers
    no-questions :no-questions
    no-answers :no-answers
    fetch-failed :fetch-failed
    (or query? (empty? questions)) :query
    quit? :quit
    :else :poll-loop))

(defmulti input input-df)

(defmethod input :initialize
  [_]
  [[{:function :staxchg.io/register-theme!
     :params ["staxchg" "lanterna-theme.properties"]}
    {:function :staxchg.io/acquire-screen!
     :params []}]])

(defmethod input :enable-screen
  [_]
  [[{:function :staxchg.io/enable-screen!
     :params [:screen]}]])

(defmethod input :snippets
  [{:keys [snippets]}]
  (list (map highlight-code-step snippets)))

(defmethod input :search-term
  [{:keys [search-term]}]
  [[{:function :staxchg.io/fetch-questions!
     :params [:screen
              (api/questions-url)
              (api/questions-query-params search-term)]}]])

(defmethod input :fetch-answers
  [{:keys [fetch-answers]}]
  [[{:function :staxchg.io/fetch-answers!
     :params [:screen
              (api/answers-url (fetch-answers :question-id))
              (api/answers-query-params (fetch-answers :page))
              (fetch-answers :question-id)]}]])

(defmethod input :no-questions
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "No matches found"}
              {:function :no-questions! :values []}]}]])

(defmethod input :no-answers
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "Question has no answers"}
              {:function :no-answers! :values []}]}]])

(defmethod input :fetch-failed
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:title "Error" :text "Could not fetch data"}
              {:function :fetch-failed! :values []}]}]])

(defmethod input :query
  [_]
  [[{:function :staxchg.io/query!
     :params [:screen]}]])

(defmethod input :quit
  [_]
  [[{:function :staxchg.io/quit!
     :params [:screen]}]])

(defmethod input :poll-loop
  [_]
  [[{:function :staxchg.io/sleep!
     :params [poll-loop-latency]}
    {:function :staxchg.io/poll-resize!
     :params [:screen]}
    {:function :staxchg.io/poll-key!
     :params [:screen]}]])

(defn output
  ""
  [world]
  (if (state/render? world)
    (presentation/recipes world)
    []))

(def all (comp (partial apply concat)
               (juxt output input)))

(defn request
  ""
  [world]
  {:recipes (all world)
   :context (:io/context world)})

