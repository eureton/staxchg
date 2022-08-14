(ns staxchg.dev
  (:require [clojure.string :as string])
  (:require [clojure.java.io :as io])
  (:require [flatland.useful.fn :as ufn])
  (:require [staxchg.plot :as plot])
  (:import [java.io Writer])
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

(defmulti log-recipe
  "String representation of circum recipe."
  first
  :hierarchy recipe-step-hierarchy)

(defmethod log-recipe :staxchg.io/put-plot!
  [[_ _ plot _]]
  (->> [(str "from: " (->> plot first second) ", "
             "to: " (->> plot last second) " BEGIN")
        (->> plot
             (map #(update % 0 (memfn getCharacter)))
             plot/text
             decorate)
        "END"]
       (string/join "\r\n")))

(defmethod log-recipe :staxchg.io/put-string!
  [[_ _ string {:keys [x y]}]]
  (str [x y] " |>"  string "<|"))

(defmethod log-recipe :staxchg.io/scroll!
  [[_ _ top bottom distance]]
  (str "at (" top "..." bottom ") by " distance))

(defmethod log-recipe :staxchg.io/clear!
  [[_ _ left top width height]]
  (str "rect [" width "x" height "] at (" left ", " top ")"))

(defmethod log-recipe :staxchg.io/fetch-questions!
  [[_ _ url _]]
  url)

(defmethod log-recipe :staxchg.io/fetch-answers!
  [[_ _ url _ question-id]]
  (str url ", " question-id))

(defmethod log-recipe :staxchg.io/highlight-code!
  [[_ code syntaxes question-id answer-id]]
  (->> [(cond-> (str "BEGIN syntax: ")
          true (str (cond->> syntaxes (coll? syntaxes) (string/join " ")) ", ")
          answer-id (str "answer-id: " answer-id ", ")
          true (str "question-id: " question-id))
        (decorate code)
        "END"]
       (string/join "\r\n")))

(defmethod log-recipe :staxchg.io/register-theme!
  [[_ theme-name filename]]
  (str theme-name " @ " filename))

(defmethod log-recipe :default [_])

(defmethod print-method :circum
  [x ^Writer writer]
  (let [fsym (-> x :in first)]
    (when-not (contains? #{:staxchg.io/poll-resize!
                           :staxchg.io/poll-key!
                           :staxchg.io/refresh!
                           :staxchg.io/sleep!} fsym)
      (.write writer (format "[%s][%s] %s"
                             (name fsym)
                             (->> x :wrappers/time (re-find #"\d*.\d{2}"))
                             (log-recipe (:in x)))))))

(defmethod print-method :hilite
  [hilite ^Writer writer]
  (->> ["HTML BEGIN"
        (-> (:raw hilite) .html (string/replace #"\r" "") decorate)
        "HTML END"]
       (string/join "\r\n")
       (.write writer)))

(defmethod print-method :world
  [world ^Writer writer]
  (let [{:keys [active-pane switched-question? switched-answer? width height
                snippets search-term fetch-answers no-questions no-answers
                query? questions fetch-failed quit? previous]
         pane? :switched-pane?
         {:keys [screen]} :io/context} world
        question? (and (= active-pane :questions) switched-question?)
        answer? (and (= active-pane :answers) switched-answer?)
        summary (->> (cond-> []
                       (nil? screen) (conj "[world] uninitialized screen")
                       (nil? width) (conj "[world] screen dimensions unknown")
                       no-questions (conj "[world] no questions")
                       no-answers (conj "[world] no answers")
                       fetch-failed (conj "[world] fetch failed")
                       (or query?
                           (empty? questions)) (conj "[world] prompt query")
                       snippets (conj "[world] snippets await highlighting")
                       search-term (conj "[world] search term submitted")
                       fetch-answers (conj "[world] answers requested")
                       pane? (conj "[world] switched pane")
                       question? (conj "[world] switched question")
                       answer? (conj "[world] switched answer")
                       quit? (conj "[world] quit"))
                     (string/join "\r\n"))]
    (if (string/blank? summary)
      (.write writer "[NO CHANGE]")
      (.write writer summary))))
