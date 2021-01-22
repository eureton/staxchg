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

(def tag-re #"(?:^|\s)\[([a-z_-]+)\](?:$|\s)")

(def user-re #"\buser:(\d+)")

(def accepted-re #"\bisaccepted:(yes|no)\b")

(defn query-tags
  ""
  [term]
  (->>
    term
    (re-seq tag-re)
    (map second)))

(defn query-user
  ""
  [term]
  (->>
    term
    (re-find user-re)
    (second)))

(defn query-accepted
  ""
  [term]
  (->>
    term
    (re-find accepted-re)
    (second)))

(defn query-freeform
  ""
  [term]
  (let [replace-with-blank #(string/replace %1 %2 " ")]
    (->
      (reduce replace-with-blank term [tag-re user-re accepted-re])
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
        [tags user accepted q] ((juxt
                                  query-tags
                                  query-user
                                  query-accepted
                                  query-freeform) term)
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
      (some? term) (assoc :intitle term)
      (not-empty tags) (assoc :tagged (string/join \; tags))
      (some? user) (assoc :user user)
      (some? accepted) (assoc :accepted accepted)
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

