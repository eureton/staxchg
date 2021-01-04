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

(defn ranges [string pattern multiline?]
  (let [regexp (java.util.regex.Pattern/compile
                 pattern
                 (cond-> 0
                   multiline? (bit-or java.util.regex.Pattern/DOTALL)))
        matcher (.matcher regexp string)]
    (loop [coordinates []]
      (if (.find matcher)
        (recur (conj coordinates [(.start matcher 1) (.end matcher 1)]))
        coordinates))))

(defn parse [string]
  (reduce
    (fn [aggregator [category pattern {:keys [multiline?]}]]
      (assoc aggregator category (ranges string pattern multiline?)))
    {}
    [[:bold       "(\\*\\*((?!\\*\\*).)+\\*\\*)"              {:multiline? true }]
     [:italic     "(?:^|[^*])(\\*[^*^\\r^\\n]+\\*)(?:[^*]|$)" {:multiline? false}]
     [:monospace  "(?:^|[^`])(`[^`]*`)(?:[^`]|$)"             {:multiline? false}]
     [:code-block "(```((?!```).)+```)"                       {:multiline? true }]]))

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
    (partition 3)))

(defn roll-info
  ""
  [info]
  (reduce
    (fn [aggregator [start end character]]
      (update aggregator character conj [start end]))
    {:bold [] :italic [] :monospace [] :code-block []}
    info))

(defn adjust-info
  ""
  [info index range-f]
  (->>
    info
    unroll-info
    (map
      (fn [[start end category]]
        (let [range-after (range-f index [start end])]
          (vector (first range-after) (second range-after) category))))
    roll-info))

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
                               (adjust-info (- end length) bounds-adjuster)
                               (adjust-info start bounds-adjuster))]
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
        (let [previous (peek aggregator)
              popped (if-not (empty? aggregator) (pop aggregator) aggregator)]
          (if (<= (+ (count previous) (count word) 1) width)
            (conj popped (string/join \space (remove nil? [previous word])))
            (conj aggregator word))))
      []
      (string/split string #"(?!\s*$) "))))

(defn reflow
  ""
  [string
   info
   {:keys [width height]}]
  (let [truncate #(take (or height (count %)) %)
        bounds-adjuster (fn [i [s e]]
                          (vector
                            (if (> s i) (inc s) s)
                            (if (> e i) (inc e) e)))]
    (as-> string v
      (string/split-lines v)
      (map
        (fn [line]
          (->>
            (pack line width)
            (map #(hash-map :s % :c  (count %)))
            (#(reduce  (fn  [agg h]  (conj agg  (assoc h :art  (not= h  (last %)))))  [] %))))
        v)
      (flatten v)
      (truncate v)
      (reduce
        (fn [agg h]
          (assoc
            agg
            :length (+ (agg :length) (h :c) (if (h :art) 1 2))
            :breaks (conj (agg :breaks) (if (h :art) (+ (h :c) (agg :length)) nil))
            :reflowed (conj (agg :reflowed) (h :s))))
        {:reflowed [] :breaks [] :length 0}
        v)
       (assoc
         v
         :reflowed (string/join "\r\n" (v :reflowed))
         :markdown-info (reduce
                          (fn [agg i] (adjust-info agg i bounds-adjuster))
                          info
                          (reverse (remove nil? (v :breaks)))))
       (dissoc v :length :breaks))))

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
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
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
                                   (map #(vector (+ % left (if (zero? index) x 0)) (+ index top y))))))
                  (reduce concat)))
     :markdown-info (->>
                      lengths
                      (reduce (fn [agg x] (conj agg (if (empty? agg) x (+ (last agg) x 2)))) [])
                      reverse
                      (reduce (fn [agg x] (adjust-info agg x adjust-range)) markdown-info))}))

(defn line-count
  [string width]
  (let [{:keys [plotted]} (plot string {:width width})]
    (inc (-
          (->> plotted last second second)
          (->> plotted first second second)))))

