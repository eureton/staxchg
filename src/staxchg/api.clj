(ns staxchg.api
  (:require [clojure.string :as string])
  (:require [cheshire.core])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.util :as util])
  (:gen-class))

(def client-id "19510")

(def api-key "pUdRXaEu0*w82Brq7xzlyw((")

(def default-site "stackoverflow")

(def answers-page-size 5)

(def error-wrapper-object {"items" [{"title" "title 1"
                                     "tags" ["clojure"]
                                     "owner" {"reputation" 9999
                                              "display_name" "Eureton"}
                                     "last_activity_date" 1631608181
                                     "creation_date" 1631567750
                                     "view_count" 83
                                     "answer_count" 2
                                     "score" 1
                                     "question_id" "12356"
                                     "body_markdown" (->> ["paragraph one"
                                                           ""
                                                           "here's a `little bit of code` for you!"
                                                           ""
                                                           "* bullet *1*"
                                                           "* bullet **2**"
                                                           "  * bullet 2a"
                                                           "  * bullet 2b"
                                                           "  * bullet 2c"
                                                           "* bullet 3"
                                                           ""
                                                           "# ATX heading (1)"
                                                           ""
                                                           "paragraph two (thematic break coming up...)"
                                                           ""
                                                           "----"
                                                           ""
                                                           "Setext heading (1)"
                                                           "==="
                                                           ""
                                                           "1. ordered *1*"
                                                           "1. ordered **2**"
                                                           "   1. ordered 2a"
                                                           "   1. ordered 2b"
                                                           "      1. ordered 2bI"
                                                           "      1. ordered 2bII"
                                                           "         1. ordered 2bII-alpha"
                                                           "         1. ordered 2bII-beta"
                                                           "         1. ordered 2bII-gamma"
                                                           "      1. ordered 2bIII"
                                                           "      1. ordered 2bIV"
                                                           "   1. ordered 2c"
                                                           "   1. ordered 2d"
                                                           "1. ordered 3"
                                                           "1. ordered 4"
                                                           "1. ordered 5"
                                                           "1. ordered 6"
                                                           ""
                                                           "### listbreaker heading!"
                                                           ""
                                                           "\t(defn indented"
                                                           "\t  \"docstring\""
                                                           "\t  [x]"
                                                           "\t  (< x x))"
                                                           ""
                                                           "paragraph 3"
                                                           ""
                                                           "``` clojure"
                                                           "(defn fenced"
                                                           "  \"docstring\""
                                                           "  [x]"
                                                           "  (+ x x))"
                                                           "```"
                                                           ""
                                                           "paragraph 4"
                                                           ""
                                                           "[_awesome_ link](http://example.com)"
                                                           ""
                                                           "paragraph 5"
                                                           ]
                                                          (string/join "\n"))}
                                    {"title" "title 2"
                                     "tags" ["java"]
                                     "question_id" "65421"
                                     "body_markdown" (->> ["Here's one:"
                                                           ""
                                                           "``` java"
                                                           "import abc.xyz.qwe;"
                                                           ""
                                                           "public class Abc {"
                                                           "    public static int sqr(int x) {"
                                                           "        return x * x;"
                                                           "    }"
                                                           "}"
                                                           "```"
                                                           ""
                                                           "Indented:"
                                                           ""
                                                           "    /**"
                                                           "     * Signals a waiting take. Called only from put/offer  (which do not"
                                                           "     * otherwise ordinarily lock takeLock.)"
                                                           "     */"
                                                           "    private void signalNotEmpty ()  {"
                                                           "        final ReentrantLock takeLock = this.takeLock;"
                                                           "        takeLock.lock ();"
                                                           "        try  {"
                                                           "            notEmpty.signal ();"
                                                           "        } finally {"
                                                           "            takeLock.unlock ();"
                                                           "        }"
                                                           "    }"
                                                           ""
                                                           "Again! :D"
                                                           ""
                                                           "``` java"
                                                           "import java.util.LinkedList;"
                                                           "import java.util.List;"
                                                           ""
                                                           "public class BBQ<T> {"
                                                           "    private LinkedList<T> tasks;"
                                                           "    private List<T> tasks;"
                                                           "    private Semaphore mutex;"
                                                           "    private Semaphore full;"
                                                           "    private Semaphore zero;"
                                                           "    public BBQ (int numofWorkers) {"
                                                           "        tasks = new ArrayList<T> ();"
                                                           "        mutex = new Semaphore (1, true);"
                                                           "        full = new Semaphore (numofWorkers, true);"
                                                           "        zero = new Semaphore (0, true);"
                                                           "    }"
                                                           "    public boolean add (T item)  {"
                                                           "        boolean ans = false;"
                                                           "        try  {"
                                                           "            zero.acquire ();"
                                                           "            mutex.acquire ();"
                                                           "            ans = tasks.add (item);"
                                                           "        } catch  (InterruptedException e)  {"
                                                           "            e.printStackTrace ();"
                                                           "        }"
                                                           "        finally {"
                                                           "            mutex.release ();"
                                                           "            full.release ();"
                                                           "        }"
                                                           "        return ans;"
                                                           "    }"
                                                           "}"
                                                           "```"
                                                           ]
                                                          (string/join "\n"))}
                                    {"tags" ["clojure" "idioms"]
                                     "owner" {"reputation" 1361
                                              "display_name" "tosh"}
                                     "is_answered" true
                                     "view_count" 83
                                     "answer_count" 2
                                     "score" 1
                                     "last_activity_date" 1631608181
                                     "creation_date" 1631567750
                                     "question_id" 69169186
                                     "body_markdown" "I want to write a function which simply updates a vector in a map with new value, but can take any number of args, but at least one.\r\n\r\nHere is example:\r\n\r\n```clojure\r\n(defn my-update [what item &amp; items]\r\n  (update what :desired-key conj item items))\r\n```\r\n\r\nUnfortunately, this doesn&#39;t work. Despite that `update` do have a signature with multiple values (like `[m k f x y]`), all remaining arguments to `my-update` will be joined into one sequence, which will be passed to `conj` as one argument.\r\n\r\nInstead, wrapping `conj` with `apply` in an anonymous function does work, but looks not so elegant:\r\n\r\n```clojure\r\n(defn my-update [what item &amp; items]\r\n  (update what :desired-key #(apply conj % item items))\r\n```\r\n\r\nWhat is the idiomatic way of writing such a function like `my-update`?"
                                     "title" "How to pass the rest args of a variadic function to another function?"}
                                    {"tags" ["java"]
                                     "last_activity_date" 1428597249
                                     "comments" [{"owner" {"reputation" 19
                                                           "display_name" "Leo Li"}
                                                  "score" 0
                                                  "creation_date" 1428597707
                                                  "body_markdown" "Thanks, I was trying to understand how do two locks guarantee thread-safety, double check the code, found put/take operations are performed on two nodes (head/tail respectively), which explains everything."}]
                                     "owner" {"reputation" 1186
                                              "display_name" "hemant1900"}
                                     "is_answered" true
                                     "view_count" 83
                                     "answer_count" 2
                                     "score" 1
                                     "creation_date" 1631567750
                                     "question_id" 99102186
                                     "body_markdown" "Look:\r\n\r\n        /**\r\n         * Signals a waiting take. Called only from put/offer (which do not\r\n         * otherwise ordinarily lock takeLock.)\r\n         */\r\n        private void signalNotEmpty() {\r\n            final ReentrantLock takeLock = this.takeLock;\r\n            takeLock.lock();\r\n            try {\r\n                notEmpty.signal();\r\n            } finally {\r\n                takeLock.unlock();\r\n            }\r\n        }\r\n    \r\n        /**\r\n         * Signals a waiting put. Called only from take/poll.\r\n         */\r\n        private void signalNotFull() {\r\n            final ReentrantLock putLock = this.putLock;\r\n            putLock.lock();\r\n            try {\r\n                notFull.signal();\r\n            } finally {\r\n                putLock.unlock();\r\n            }\r\n        }\r\n\r\n`put` method signals other threads trying to take/poll from empty queue and `take` method signals other threads trying to put elements into full queue."
                                     "title" "... @ @ ..."}]
                           "has_more" false
                           "error" true})

(comment(def error-wrapper-object {"items" []
                           "has_more" false
                           "error" true}))

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
  [term page-size]
  (let [[tags user accepted
         score title] (map
                        (partial query-params-match term)
                        query-params-patterns)
        q (query-freeform term)
        base {:page 1
              :pagesize page-size
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

(defn scrub
  ""
  [item]
  (cond-> item
    (contains? item "title") (update "title" util/unescape-html)
    (contains? item "body_markdown") (update "body_markdown" util/unescape-html)
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

