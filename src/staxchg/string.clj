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

(def truncated?
  "True if a word has been truncated, false otherwise."
  (comp #{\…} last))

(defn pack
  "Splits string into a vector of strings, none of which is longer than width.
   Splitting is done by the following rules:
     * only whitespace characters are considered as points for splitting
     * the split replaces a single whitespace character
     * if no split yields parts of appropriate length, string is truncated

   When provided, x denotes an indentation applicable to the first line only."
  ([width string]
   (if (->> string count (>= width))
     [string]
     (reduce
       (fn [aggregator word]
         (let [previous (peek aggregator)
               popped (if-not (empty? aggregator) (pop aggregator) aggregator)]
           (if (<= (+ (count previous) (count word) 1) width)
             (conj popped (string/join \space (remove nil? [previous word])))
             (conj aggregator (truncate word width)))))
       []
       (string/split string #"(?<!^\s*)\s(?!\s|$)"))))
  ([x width string]
   (loop [string string
          result []
          index 0]
     (let [top? (zero? index)
           packed (-> (cond-> width
                        top? (- x))
                      (pack string))
           packed (if (and top? (truncated? (first packed)))
                    ["" (string/triml string)]
                    packed)]
       (if (= (count packed) 1)
         (vec (concat result packed))
         (recur (->> packed rest (remove string/blank?) (string/join \space))
                (conj result (first packed))
                (inc index)))))))

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

