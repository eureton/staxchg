(ns staxchg.string
  (:require [clojure.string :as string])
  (:gen-class))

(defn truncate
  ""
  [word width]
  (let [length (count word)]
    (if (<= length width)
      word
      (str (subs word 0 (dec width)) \…))))

(defn truncated?
  ""
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
       (string/split string #"(?!\s*$) "))))
  ([x width string]
   (loop [string string
          result []
          index 0]
     (let [top? (zero? index)
           packed (pack (- width (if top? x 0)) string)
           packed (if (and top? (truncated? (first packed)))
                    ["" string]
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

