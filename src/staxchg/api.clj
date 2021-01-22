(ns staxchg.api
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.util :as util])
  (:require [cheshire.core])
  (:gen-class))

(defn url
  ""
  []
  (let [base "https://api.stackexchange.com"
        version "2.2"
        endpoint "search/advanced"]
    (str base "/" version "/" endpoint)))

(defn query-tags
  ""
  [term]
  (->>
    term
    (re-seq #"(?:^|\s)\[([a-z_-]+)\](?:$|\s)")
    (map second)))

(defn query-user
  ""
  [term]
  (->>
    term
    (re-find #"\buser:(\d+)")
    (second)))

(defn query-accepted
  ""
  [term]
  (->>
    term
    (re-find #"\bisaccepted:(yes|no)\b")
    (second)))

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
        [tags user accepted] ((juxt query-tags query-user query-accepted) term)
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
      (not-empty tags) (assoc :tagged (clojure.string/join \; tags))
      (some? user) (assoc :user user)
      (some? accepted) (assoc :accepted accepted))))

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

