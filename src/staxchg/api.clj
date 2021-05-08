(ns staxchg.api
  (:require [clojure.string :as string])
  (:require [cheshire.core])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.util :as util])
  (:gen-class))

(def client-id "19510")

(def api-key "pUdRXaEu0*w82Brq7xzlyw((")

(def default-site "stackoverflow")

(def questions-page-size 4)

(def answers-page-size 5)

(def error-wrapper-object {"items" []
                           "has_more" false
                           "error" true})

(def error-response {:body (cheshire.core/encode error-wrapper-object)})

(defn questions-url
  ""
  []
  (let [base "https://api.stackexchange.com"
        version "2.2"
        endpoint "search/advanced"]
    (str base "/" version "/" endpoint)))

(defn answers-url
  ""
  [question-id]
  (let [base "https://api.stackexchange.com"
        version "2.2"
        endpoint "questions"]
    (str base "/" version "/" endpoint "/" question-id "/answers")))

(def query-params-patterns [{:regex #"\[([a-z.#_+-]+)\]"       :multi? true }
                            {:regex #"\buser:(\d+)\b"          :multi? false}
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

(defn auth-query-params
  ""
  []
  (let [token ((util/config-hash) "ACCESS_TOKEN")]
    (cond-> {:client_id client-id
             :key api-key}
      token (assoc :access_token token))))

(defn questions-query-params
  ""
  [term]
  (let [[tags user accepted
         score title] (map
                        (partial query-params-match term)
                        query-params-patterns)
        q (query-freeform term)
        base {:page 1
              :pagesize questions-page-size
              :order "desc"
              :sort "relevance"
              :site ((util/config-hash) "SITE" default-site)
              :filter "!*0Ld)hQoB5KcGorrGBWAL9j(DXh.(bWg*(h)Jfo1h"}]
    (cond-> (merge (auth-query-params) base)
      (not-empty tags) (assoc :tagged (string/join \; tags))
      (some? user) (assoc :user user)
      (some? accepted) (assoc :accepted accepted)
      (some? score) (assoc :sort "votes" :min score)
      (some? title) (assoc :title title)
      (not (string/blank? q)) (assoc :q q))))

(defn answers-query-params
  ""
  [page]
  (merge (auth-query-params) {:page page
                              :pagesize answers-page-size
                              :order "desc"
                              :sort "votes"
                              :site ((util/config-hash) "SITE" default-site)
                              :filter "!WWsokPk3Vh*T_kIP2MV(bQNcR1w-GRejyamhb31"}))

(defn unescape-html [string]
  (org.jsoup.parser.Parser/unescapeEntities string true))

(defn scrub
  ""
  [item]
  (cond-> item
    (contains? item "title") (update "title" unescape-html)
    (contains? item "body_markdown") (update "body_markdown" unescape-html)
    (contains? item "answers") (update "answers" (partial mapv scrub))
    (contains? item "comments") (update "comments" (partial mapv scrub))))

(defn parse-response
  ""
  [response]
  (->
    response
    :body
    cheshire.core/parse-string
    (update "items" (partial mapv scrub))))

