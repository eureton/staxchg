(ns staxchg.markdown
  (:require [clojure.string :as string])
  (:gen-class))

(defn within? [ranges index]
  (some
    (fn [[from to]] (and (<= from index) (< index to)))
    ranges))

(defn categories [info index]
  (reduce
    (fn [aggregator [category ranges]]
      (if (within? ranges index)
        (conj aggregator category)
        aggregator))
    #{}
    info))

(defn decorate [recipient categories & clauses]
  (let [effect-map (apply
                     zipmap
                     (map
                       (fn [f]
                         (map
                           second
                           (filter #(->> % first f) (map-indexed vector clauses))))
                       [even? odd?]))]
    (reduce
      (fn [aggregator category] ((effect-map category) aggregator))
      recipient
      categories)))

(defn ranges [string pattern]
  (let [regexp (java.util.regex.Pattern/compile pattern java.util.regex.Pattern/DOTALL)
        matcher (.matcher regexp string)]
    (loop [coordinates []]
      (if (.find matcher)
        (recur (conj coordinates [(.start matcher 1) (.end matcher 1)]))
        coordinates))))

(defn parse [string]
  (reduce
    (fn [aggregator [category pattern]]
      (assoc aggregator category (ranges string pattern)))
    {}
    [[:bold "(\\*\\*((?!\\*\\*).)+\\*\\*)"]
     [:italic "[^*](\\*[^*]+\\*)[^*]"]
     [:monospace "[^`](`[^`]+`)[^`]"]
     [:code-block "(```((?!```).)+```)"]]))

(defn adjust-info2
  ""
  [info index range-f]
  (reduce
    (fn [aggregator category]
      (assoc
        aggregator
        category
        (mapv #(range-f index %) (info category))))
    {}
    (keys info)))

(defn adjust-info
  ""
  [info index f]
  (let [adjust-range (fn [[start end]]
                       (vector
                         (if (>= start index) (f start) start)
                         (if (> end index) (f end) end)))]
    (reduce
      (fn [aggregator category]
        (assoc
          aggregator
          category
          (mapv adjust-range (info category))))
      {}
      (keys info))))

(defn unroll-info
  ""
  [info]
  (->>
    info
    (map
      (fn [[category ranges]]
        (map
          (fn [[start end]] (vector start end category))
          ranges)))
    flatten
    (partition 3)
    (sort #(- (first %1) (first %2)))))

(defn roll-info
  ""
  [info]
  (reduce
    (fn [aggregator [start end character]]
      (update aggregator character conj [start end]))
    {:bold [] :italic [] :monospace [] :code-block []}
    info))

(defn strip
  ""
  [string info]
  (let [strip-lengths {:italic 1 :bold 2 :monospace 1 :code-block 3}]
    (loop [s string
           i info
           u (unroll-info info)]
      (if (empty? u)
        {:input string
         :stripped s
         :markdown-info i}
        (let [[start end category] (first u)
              length (strip-lengths category)
              bounds-adjuster (fn [i [s e]]
                                (vector
                                  (if (> s i) (- s length) s)
                                  (if (> e i) (- e length) e)))
              info-adjuster #(->
                               %
                               (adjust-info2 (- end length) bounds-adjuster)
                               (adjust-info2 start bounds-adjuster))]
          (recur
            (str
              (subs s 0 start)
              (subs s (+ start length) (- end length))
              (subs s end))
            (info-adjuster i)
            (->> u rest roll-info info-adjuster unroll-info)))))))

(defn pack [string width]
  (if (->> string count (>= width))
    [string]
    (reduce
      (fn [aggregator word]
        (let [previous (last aggregator)]
          (if (<= (+ (count previous) (count word) 1) width)
            (conj (pop aggregator) (string/join \space (remove string/blank? [previous word])))
            (conj aggregator word))))
      [""]
      (string/split string #" "))))

(defn reflow
  ""
  [string
   info
   {:keys [width height]}]
  (let [truncate #(take (or height (count %)) %)]
    (->>
      string
      string/split-lines
      (map (fn [line]
             (->>
               (pack line width)
               (map #(hash-map :s % :c  (count %)))
               (#(reduce  (fn  [agg h]  (conj agg  (assoc h :art  (not= h  (last %)))))  [] %)))))
      flatten
      truncate
      (reduce
        (fn [agg h]
          (assoc
            agg
            :length (+ (agg :length) (h :c) (if (h :art) 1 2))
            :breaks (conj (agg :breaks) (if (h :art) (+ (h :c) (agg :length)) nil))
            :reflowed (conj (agg :reflowed) (h :s))))
        {:reflowed [] :breaks [] :length 0})
       (#(assoc
           %
           :reflowed (string/join "\r\n" (% :reflowed))
           :markdown-info (reduce (fn [agg i] (adjust-info agg i inc)) info (remove nil? (% :breaks)))))
       )))

(defn slice
  "Slices the input string into pieces of the given length and returns them in a
  sequence. No padding is applied to the last piece."
  [string width]
  (->>
    string
    (partition width width (repeat \space))
    (map string/join)))

(defn plot
  "Returns a sequence of pairs -one for each character of the input string-
  consisting of:
    * the character
    * the [x y] coordinates of the character"
  [string
   {:keys [left top width height]
    :or {left 0 top 0}}]
  (let [{:keys [stripped markdown-info]} (strip string (parse string))
        {:keys [reflowed markdown-info]} (reflow
                                           stripped
                                           markdown-info
                                           {:width width})
        lines (string/split-lines reflowed)
        truncate #(take (or height (count %)) %)
        lengths (->>
                  lines
                  ;(map #(slice % width))
                  flatten
                  truncate
                  (map count))
        adjust-range (fn [index [start end]]
                       (vector
                         (if (> start index) (- start 2) start)
                         (if (> end index) (- end 2) end)))]
    {:plotted (map
                vector
                (seq (string/join lines))
                (->>
                  lengths
                  (map-indexed (fn [index length]
                                 (->>
                                   length
                                   range
                                   (map #(vector (+ % left) (+ index top))))))
                  (reduce concat)))
     :markdown-info (->>
                      lengths
                      (reduce (fn [agg x] (conj agg (if (empty? agg) x (+ (last agg) x 2)))) [])
                      reverse
                      (reduce (fn [agg x] (adjust-info2 agg x adjust-range)) markdown-info))}))

(defn line-count
  [string width]
  (let [{:keys [plotted markdown-info]} (plot string {:width width})]
    (inc (- (->> plotted last second second) (->> plotted first second second)))))

