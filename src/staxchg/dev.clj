(ns staxchg.dev
  (:require [clojure.string :as string])
  (:require [staxchg.plot :as plot])
  (:require [staxchg.util :as util])
  (:gen-class))

(defn truncate
  [x]
  (if-let [_ (string? x)]
    (let [length (count x)
          truncation-limit 128
          context 32]
      (if (> length truncation-limit)
        (str (subs x 0 context) " [...] " (subs x (- length context) length))
        x))
    x))

(defn decorate
  "Helps a block of text stand out by:
    1. indenting
    2. prepending each line with a character

   The number of spaces to indent and the decoration character are controlled
   by the options :indent and :decor, respectively."
  ([string {:keys [indent decor]
            :or {indent 1
                 decor \|}}]
   (->> (concat [(str (string/join (repeat (dec indent) \^)) "\\")]
                (->> string
                     string/trim-newline
                     string/split-lines
                     (map #(str (string/join (repeat indent \space)) decor %)))
                [(str (string/join (repeat (dec indent) \_)) \/)])
        (string/join "\r\n")))
  ([string]
   (decorate string {})))

(defmulti log-recipe-step :function)

(defmethod log-recipe-step :staxchg.io/put-markdown!
  [{[_ plot _] :params}]
  (->> [(str "[put-markdown] from: " (->> plot first second) ", "
                              "to: " (->> plot last second) " BEGIN")
        (->> plot
             (map #(update % 0 (memfn getCharacter)))
             plot/text
             decorate)
        "[put-markdown] END"]
       (string/join "\r\n")))

(defmethod log-recipe-step :staxchg.io/put-string!
  [{[_ string {:keys [x y]}] :params}]
  (str "[put-string] " [x y] " |>"  string "<|"))

(defmethod log-recipe-step :staxchg.io/scroll!
  [{[_ top bottom distance] :params}]
  (str "[scroll] at (" top ", " bottom ") by " distance))

(defmethod log-recipe-step :staxchg.io/clear!
  [{[graphics left top width height] :params}]
  (str "[clear] rect [" width "x" height "] at (" left ", " top ")"))

(defmethod log-recipe-step :staxchg.io/fetch-questions!
  [{[_ url query-params] :params}]
  (str "[fetch-questions] url: " url ", query-params: " query-params))

(defmethod log-recipe-step :staxchg.io/fetch-answers!
  [{[_ url query-params question-id] :params}]
  (str "[fetch-answers] url: " url ", "
       "query-params: " query-params ", "
       "question-id: " question-id))

(defmethod log-recipe-step :staxchg.io/highlight-code!
  [{[code syntax question-id answer-id] :params}]
  (->> [(cond-> (str "[highlight-code] BEGIN syntax: " syntax ", ")
          answer-id (str "answer-id: " answer-id ", ")
          true (str "question-id: " question-id))
        (decorate code)
        "[highlight-code] END"]
       (string/join "\r\n")))

(defmethod log-recipe-step :staxchg.io/register-theme!
  [{[theme-name filename] :params}]
  (str "[register-theme] name: " theme-name ", filename: " filename))

(defmethod log-recipe-step :default [_])

(defn log-item-df
  "Dispatch function for dev/log-item."
  [item]
  (let [has-keys? #(every? (partial contains? item) %)
        hash-with-keys? #(and (map? item)
                              (has-keys? %&))]
    (cond
      (hash-with-keys? :context :recipes) :request
      (hash-with-keys? :raw :html :text) :hilite)))

(defmulti log-item
  "Abstraction layer for logging domain structures.
   Returns a string suitable for writing to a file."
  log-item-df)

(defmethod log-item :request
  [{:keys [recipes timing]}]
  (->> [[(str " /^^^ " (count recipes) " recipe(s)")]
        (map #(str "|----- " (string/join ", " (map :function %1)) " ---- " %2)
             recipes
             (map (comp second #(re-find #"(\d*.\d* msecs)" %)) timing))
        [" \\___ Complete"]
        (->> recipes flatten (map log-recipe-step) (remove nil?))]
       (reduce concat)
       (string/join "\r\n")))

(defmethod log-item :hilite
  [{:keys [raw]}]
  (->> ["HTML BEGIN"
        (-> raw .html (string/replace #"\r" "") decorate)
        "HTML END"]
       (string/join "\r\n")))

(defmethod log-item :default
  [item]
  item)

(defn log
  [& items]
  (when-let [pathname (util/config-hash "LOGFILE")]
    (with-open [writer (clojure.java.io/writer pathname :append true)]
      (.write writer (str (apply str (map log-item items)) "\n")))))

