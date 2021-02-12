(ns staxchg.string
  (:require [clojure.string :as string])
  (:gen-class))

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
             (conj aggregator word))))
       []
       (string/split string #"(?!\s*$) "))))
  ([x width string]
   (loop [string string
          result []
          index 0]
     (let [packed (pack (- width (if (zero? index) x 0)) string)]
       (if (= (count packed) 1)
         (concat result packed)
         (recur
           (string/join \space (rest packed))
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

