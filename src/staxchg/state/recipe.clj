(ns staxchg.state.recipe
  (:require [staxchg.api :as api])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:gen-class))

(defn highlight-code-step
  ""
  [{:keys [string lang question-id]
    :or {lang "lisp"}}]
  {:function :staxchg.io/highlight-code!
   :params [string lang question-id]})

(defn input-recipes-df
  ""
  [{:as world
    :keys [query? questions search-term fetch-answers no-questions no-answers
           fetch-failed snippets]}]
  (cond
    snippets :snippets
    search-term :search-term
    fetch-answers :fetch-answers
    no-questions :no-questions
    no-answers :no-answers
    fetch-failed :fetch-failed
    (or query? (empty? questions)) :query
    :else :read-key))

(defmulti input-recipes input-recipes-df)

(defmethod input-recipes :snippets
  [{:keys [snippets]}]
  (list (map highlight-code-step snippets)))

(defmethod input-recipes :search-term
  [{:keys [search-term]}]
  [[{:function :staxchg.io/fetch-questions!
     :params [:screen
              (api/questions-url)
              (api/questions-query-params search-term)]}]])

(defmethod input-recipes :fetch-answers
  [{:keys [fetch-answers]}]
  [[{:function :staxchg.io/fetch-answers!
     :params [:screen
              (api/answers-url (fetch-answers :question-id))
              (api/answers-query-params (fetch-answers :page))
              (fetch-answers :question-id)]}]])

(defmethod input-recipes :no-questions
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "No matches found"}
              {:function :no-questions! :values []}]}]])

(defmethod input-recipes :no-answers
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "Question has no answers"}
              {:function :no-answers! :values []}]}]])

(defmethod input-recipes :fetch-failed
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:title "Error" :text "Could not fetch data"}
              {:function :fetch-failed! :values []}]}]])

(defmethod input-recipes :query
  [_]
  [[{:function :staxchg.io/query!
     :params [:screen]}]])

(defmethod input-recipes :read-key
  [{:keys [snippets]}]
  [[{:function :staxchg.io/read-key!
     :params [:screen]}]])

(defn output
  ""
  [world]
  (if (state/write-output? (:previous world) world)
    (presentation/recipes world)
    []))

(def all (comp (partial apply concat)
               (juxt output input-recipes)))

