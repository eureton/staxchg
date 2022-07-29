(ns staxchg.string
  (:require [clojure.string :as string])
  (:gen-class))

(defn truncate
  "If word is longer than width, excess characters plus one are replaced by a
   single ellipsis (…) character."
  [word width]
  (let [length (count word)]
    (if (<= length width)
      word
      (str (subs word 0 (max 0 (dec width))) \…))))

(defn bite
  "Breaks off the beginning of string such that it is:
     * width characters at most
     * either followed by a space or no longer than width
   Should the above be impossible, policy comes into effect:
     * :truncate   fragment is (width - 1) characters followed by an
                   ellipses character (…)
     * :float      fragment is an empty string
   The fragment and the remainder are returned in a vector. The space character
   following the fragment is not included in the remainder."
  [width string policy]
  (when-not (contains? #{:truncate :float} policy)
    (throw (IllegalArgumentException. (str "Unsupported policy: " policy))))
  (loop [string string
         result nil]
    (let [[head tail] (string/split string #" " 2)
          joined (->> [result head] (remove nil?) (string/join " "))
          fits? (<= (count joined) width)]
      (cond (and fits? tail)        (recur tail joined)
            (and fits? (nil? tail)) [joined]
            (not-empty result)      [result string]
            (= :truncate policy)    [(truncate head width) tail]
            (= :float policy)       ["" string]))))

(defn pack
  "Splits string into a vector of strings, none of which is longer than width.
   Splitting is done by the following rules:
     * only whitespace characters are considered as points for splitting
     * the split replaces a single whitespace character
     * if no split yields parts of appropriate length, string is truncated

   When provided, x denotes an indentation applicable to the first line only."
  [x width string]
  (loop [string string
         result []
         index 0]
    (let [top? (zero? index)
          [bit remainder] (bite (cond-> width
                                  top? (- x))
                                string
                                (if top? :float :truncate))
          result (conj result bit)]
      (if (nil? remainder)
        result
        (recur remainder
               result
               (inc index))))))

(defn reflow
  "Introduces line breaks into string so that no line is longer than width. The
   line break sequence is \r\n by default, but may be overridden via :separator.
   Keyword :x denotes the number of spaces to indent the first line by (default
   is zero)."
  [string
   {:keys [x width separator]
    :or {x 0 separator "\r\n"}}]
  (->> string
       string/split-lines
       (map #(pack x width %))
       flatten
       (string/join separator)))

(defn trim-leading-indent
  "Trims the following off each line of the given string:
     * the first tab character
     * the first 4 spaces"
  [string]
  (when (some? string)
    (string/replace string #"(?im)^(?:    |\t)" "")))

(defn append-missing-crlf
  "Appends a CRLF sequence to the given string, unless it already ends in one."
  [string]
  (when (some? string)
    (cond-> string
      (not (string/ends-with? string "\r\n")) (str "\r\n"))))

