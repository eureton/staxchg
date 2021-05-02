(ns staxchg.recipe.step
  (:require [clojure.string :as string])
  (:gen-class))

(defmulti dump :function)

(defmethod dump :staxchg.io/put-markdown!
  [{[_ plot _] :params}]
  ["[put-markdown] " (->> (map second plot) (take 10) (apply str))
   " |>" (string/join (->> plot (map first) (map #(.getCharacter %)))) "<|"])

(defmethod dump :staxchg.io/put-string!
  [{[_ string {:keys [x y]}] :params}]
  ["[put-string] " [x y] " |>"  string "<|"])

(defmethod dump :staxchg.io/scroll!
  [{[_ top bottom distance] :params}]
  ["[scroll] at [" top " " bottom "] by " distance])

(defmethod dump :staxchg.io/clear!
  [{[graphics] :params}]
  (let [size (.getSize graphics)]
    ["[clear] rect [" (.getColumns size) "x" (.getRows size) "]"]))

(defmethod dump :staxchg.io/refresh!
  [_]
  ["[refresh]"])

(defmethod dump :staxchg.io/read-key!
  [_]
  ["[read-key]"])

(defmethod dump :staxchg.io/query!
  [_]
  ["[query]"])

(defmethod dump :staxchg.io/fetch-questions!
  [{[_ url query-params] :params}]
  ["[fetch-questions] url: " url ", query-params: " query-params])

(defmethod dump :staxchg.io/fetch-answers!
  [{[_ url query-params question-id] :params}]
  ["[fetch-answers] url: " url ", "
   "query-params: " query-params ", "
   "question-id: " question-id])

(defmethod dump :staxchg.io/highlight-code!
  [{[code syntax question-id] :params}]
  ["[highlight-code] BEGIN syntax: " syntax ", "
   "question-id: " question-id "\r\n" code
   "[highlight-code] END"])

(defmethod dump :staxchg.io/quit!
  [_]
  ["[quit]"])  

(defmethod dump :staxchg.io/register-theme!
  [{[theme-name filename] :params}]
  ["[register-theme] name: " theme-name ", filename: " filename])

(defmethod dump :staxchg.io/acquire-screen!
  [_]
  ["[acquire-screen]"])

(defmethod dump :staxchg.io/enable-screen!
  [_]
  ["[enable-screen]"])

(defmethod dump :default
  [_]
  [])

