(ns staxchg.state.recipe
  (:require [staxchg.api :as api])
  (:gen-class))

(defn highlight-code-step
  ""
  [{:keys [string lang question-id]
    :or {lang "lisp"}}]
  {:function :staxchg.ui/highlight-code!
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
  [(mapv highlight-code-step snippets)])

(defmethod input-recipes :search-term
  [{:keys [search-term]}]
  [[{:function :staxchg.ui/fetch-questions!
     :params [:screen
              (api/questions-url)
              (api/questions-query-params search-term)]}]])

(defmethod input-recipes :fetch-answers
  [{:keys [fetch-answers]}]
  [[{:function :staxchg.ui/fetch-answers!
     :params [:screen
              (api/answers-url (fetch-answers :question-id))
              (api/answers-query-params (fetch-answers :page))
              (fetch-answers :question-id)]}]])

(defmethod input-recipes :no-questions
  [_]
  [[{:function :staxchg.ui/show-message!
     :params [:screen
              {:text "No matches found"}
              {:function :no-questions! :values []}]}]])

(defmethod input-recipes :no-answers
  [_]
  [[{:function :staxchg.ui/show-message!
     :params [:screen
              {:text "Question has no answers"}
              {:function :no-answers! :values []}]}]])

(defmethod input-recipes :fetch-failed
  [_]
  [[{:function :staxchg.ui/show-message!
     :params [:screen
              {:title "Error" :text "Could not fetch data"}
              {:function :fetch-failed! :values []}]}]])

(defmethod input-recipes :query
  [_]
  [[{:function :staxchg.ui/query!
     :params [:screen]}]])

(defmethod input-recipes :read-key
  [{:keys [snippets]}]
  [[{:function :staxchg.ui/read-key!
     :params [:screen]}]])

