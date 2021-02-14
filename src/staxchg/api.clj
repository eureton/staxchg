(ns staxchg.api
  (:require [clojure.string :as string])
  (:require [cheshire.core])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.util :as util])
  (:gen-class))

(defn url
  ""
  []
  (let [base "https://api.stackexchange.com"
        version "2.2"
        endpoint "search/advanced"]
    (str base "/" version "/" endpoint)))

(def query-params-patterns [{:regex #"\[([a-z_-]+)\]"          :multi? true }
                            {:regex #"\buser:(\d+)"            :multi? false}
                            {:regex #"\bisaccepted:(yes|no)\b" :multi? false}
                            {:regex #"\bscore:(\d+)\b"         :multi? false}
                            {:regex #"\"(.*?)\""               :multi? false}])

(defn query-params-match
  ""
  [term
   {:as pattern :keys [regex multi?]}]
  (if multi?
    (->>
      term
      (re-seq regex)
      (map second))
    (->>
      term
      (re-find regex)
      (second))))

(defn query-freeform
  ""
  [term]
  (let [replace-with-blank #(string/replace %1 %2 " ")]
    (->
      (reduce replace-with-blank term (map :regex query-params-patterns))
      (string/replace #"\s+" " ")
      (string/trim))))

(defn query-params
  ""
  [term]
  (let [conf (util/properties-hash (util/config-pathname))
        page 1
        page-size 4
        attrs "!-lBu6t1YOC-dY0oUYCPHplAv.M5WWQgF.uhV0ZXZ19O1BjQLcqChyu"
        site "stackoverflow"
        order "desc"
        sort-attr "relevance"
        [tags user accepted
         score title] (map
                        (partial query-params-match term)
                        query-params-patterns)
        q (query-freeform term)
        base {:client_id (conf "CLIENT_ID")
              :key (conf "API_KEY")
              :access_token (conf "ACCESS_TOKEN")
              :page page
              :pagesize page-size
              :order order
              :sort sort-attr
              :site site
              :filter attrs}]
    (cond-> base
      (not-empty tags) (assoc :tagged (string/join \; tags))
      (some? user) (assoc :user user)
      (some? accepted) (assoc :accepted accepted)
      (some? score) (assoc :sort "votes" :min score)
      (some? title) (assoc :title title)
      (not (string/blank? q)) (assoc :q q))))

(defn unescape-html [string]
  (org.jsoup.parser.Parser/unescapeEntities string true))

(defn scrub-body
  ""
  [post]
  (update post "body_markdown" unescape-html))

(defn scrub-answer
  ""
  [{:as answer
    :strs [body_markdown comments]}]
  (assoc
    answer
    "body_markdown" (unescape-html body_markdown)
    "comments" (mapv scrub-body (vec comments))))

(defn scrub-question
  ""
  [{:as question
    :strs [title body_markdown answers comments]}]
  (assoc
    question
    "title" (unescape-html title)
    "body_markdown" (unescape-html body_markdown)
    "answers" (mapv scrub-answer (vec answers))
    "comments" (mapv scrub-body (vec comments))))

(defn parse-response
  ""
  [response]
  (->>
    response
    :body
    cheshire.core/parse-string))

