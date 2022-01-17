(ns staxchg.dev
  (:require [clojure.string :as string])
  (:require [clojure.java.io :as io])
  (:require [staxchg.plot :as plot])
  (:gen-class))

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

(def recipe-step-hierarchy
  "Hierarchy meant for use in staxchg.dev/log-recipe-step"
  (-> (make-hierarchy)
      (derive :staxchg.io/run-highlight.js! :staxchg.io/highlight-code!)
      (derive :staxchg.io/run-skylighting! :staxchg.io/highlight-code!)
      atom))

(defmulti log-recipe-step
  "String representation of the steps of domain-specific recipes."
  first
  :hierarchy recipe-step-hierarchy)

(defmethod log-recipe-step :staxchg.io/put-plot!
  [[_ _ plot _]]
  (->> [(str "[put-plot] from: " (->> plot first second) ", "
                         "to: " (->> plot last second) " BEGIN")
        (->> plot
             (map #(update % 0 (memfn getCharacter)))
             plot/text
             decorate)
        "[put-plot] END"]
       (string/join "\r\n")))

(defmethod log-recipe-step :staxchg.io/put-string!
  [[_ _ string {:keys [x y]}]]
  (str "[put-string] " [x y] " |>"  string "<|"))

(defmethod log-recipe-step :staxchg.io/scroll!
  [[_ _ top bottom distance]]
  (str "[scroll] at (" top "..." bottom ") by " distance))

(defmethod log-recipe-step :staxchg.io/clear!
  [[_ _ left top width height]]
  (str "[clear] rect [" width "x" height "] at (" left ", " top ")"))

(defmethod log-recipe-step :staxchg.io/fetch-questions!
  [[_ _ url query-params]]
  (str "[fetch-questions] url: " url ", query-params: " query-params))

(defmethod log-recipe-step :staxchg.io/fetch-answers!
  [[_ _ url query-params question-id]]
  (str "[fetch-answers] url: " url ", "
       "query-params: " query-params ", "
       "question-id: " question-id))

(defmethod log-recipe-step :staxchg.io/highlight-code!
  [[_ code syntaxes question-id answer-id]]
  (->> [(cond-> (str "[highlight-code] BEGIN syntax: ")
          true (str (cond->> syntaxes (coll? syntaxes) (string/join " ")) ", ")
          answer-id (str "answer-id: " answer-id ", ")
          true (str "question-id: " question-id))
        (decorate code)
        "[highlight-code] END"]
       (string/join "\r\n")))

(defmethod log-recipe-step :staxchg.io/register-theme!
  [[_ theme-name filename]]
  (str "[register-theme] name: " theme-name ", filename: " filename))

(defmethod log-recipe-step :default [_])

(defn log-recipe
  "String representation of recipe."
  [recipe]
  (->> recipe
       (map log-recipe-step)
       (remove nil?)
       (string/join "\r\n")))

(defn log-item-df
  "Dispatch function for staxchg.dev/log-item"
  [item]
  (let [has-keys? #(every? (partial contains? item) %)
        hash-with-keys? #(and (map? item)
                              (has-keys? %&))]
    (cond (hash-with-keys? :io/context :questions) :world
          (hash-with-keys? :context :recipes) :cookbook
          (hash-with-keys? :raw :html :traits :text) :hilite)))

(defmulti log-item
  "Abstraction layer for logging domain structures.
   Returns a string suitable for writing to a file."
  log-item-df)

(defmethod log-item :cookbook
  [{:keys [recipes timing]}]
  (->> [[(str " /^^^ " (count recipes) " recipe(s)")]
        (map #(str "|----- " (string/join ", " (map first %1)) " ---- " %2)
             recipes
             (map (comp second #(re-find #"(\d*.\d* msecs)" %)) timing))
        [" \\___ Complete"]
        (mapv log-recipe recipes)]
       (reduce concat)
       (string/join "\r\n")))

(defmethod log-item :hilite
  [{:keys [raw]}]
  (->> ["HTML BEGIN"
        (-> raw .html (string/replace #"\r" "") decorate)
        "HTML END"]
       (string/join "\r\n")))

(defmethod log-item :world
  [{:as world
    :keys [active-pane switched-question? switched-answer? width height
           snippets search-term fetch-answers no-questions no-answers
           query? questions fetch-failed quit? previous]
    pane? :switched-pane?
    {:keys [screen]} :io/context}]
  (let [question? (and (= active-pane :questions) switched-question?)
        answer? (and (= active-pane :answers) switched-answer?)]
    (->> (cond-> []
           (nil? screen) (conj "[world] uninitialized screen")
           (nil? width) (conj "[world] screen dimensions unknown")
           no-questions (conj "[world] no questions")
           no-answers (conj "[world] no answers")
           fetch-failed (conj "[world] fetch failed")
           (or query? (empty? questions)) (conj "[world] prompt query")
           snippets (conj "[world] code snippets await highlighting")
           search-term (conj "[world] search term submitted")
           fetch-answers (conj "[world] answers requested")
           pane? (conj "[world] switched pane")
           question? (conj "[world] switched question")
           answer? (conj "[world] switched answer")
           quit? (conj "[world] quit"))
         (string/join "\r\n"))))

(defmethod log-item :default
  [item]
  item)

(defn log
  "Writes string representations of items to the log, if the LOGFILE environment
   variable has been set."
  [& items]
  (when-let [pathname (System/getenv "LOGFILE")]
    (with-open [writer (io/writer pathname :append true)]
      (.write writer (str (apply str (map log-item items)) "\n")))))

