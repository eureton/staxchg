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

(defn truncated?
  "True if a word has been truncated, false otherwise."
  [word]
  (some #{\…} word))

(defn pack
  ""
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
           packed (pack (- width (if top? x 0)) string)
           packed (if (and top? (truncated? (first packed)))
                    ["" (string/triml string)]
                    packed)]
       (if (= (count packed) 1)
         (concat result packed)
         (recur
           (string/join \space (remove string/blank? (rest packed)))
           (conj result (first packed))
           (inc index)))))))

(defn reflow
  ""
  [string
   {:keys [x width]
    :or {x 0}}]
  (->>
    string
    string/split-lines
    (map (partial pack x width))
    flatten
    (string/join "\r\n")))

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

