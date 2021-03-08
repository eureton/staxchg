(ns staxchg.api
  (:require [clojure.string :as string])
  (:require [cheshire.core])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.util :as util])
  (:gen-class))

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

(defn questions-query-params
  ""
  [term]
  (let [conf (util/properties-hash (util/config-pathname))
        page 1
        page-size 4
        attrs "!*0OqKTs5Z3Ruz_pHoqBLWuuZMbuzO9VDaB-9T1wV("
        site (conf "SITE")
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

(defn answers-query-params
  ""
  [page]
  (let [conf (util/properties-hash (util/config-pathname))]
    {:client_id (conf "CLIENT_ID")
     :key (conf "API_KEY")
     :access_token (conf "ACCESS_TOKEN")
     :page page
     :pagesize answers-page-size
     :order "desc"
     :sort "votes"
     :site (conf "SITE")
     :filter "!*cCE1WB4Zi)NAFAJ)1MAjs8QNFjLAJ0x9tOP9"}))

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

