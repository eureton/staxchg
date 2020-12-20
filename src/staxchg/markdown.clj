(ns staxchg.markdown
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
  (let [matcher (.matcher pattern string)]
    (loop [coordinates []]
      (if (.find matcher)
        (recur (conj coordinates [(.start matcher 1) (.end matcher 1)]))
        coordinates))))

(defn parse [string]
  (reduce
    (fn [aggregator [category pattern]]
      (assoc aggregator category (ranges string pattern)))
    {}
    [[:bold #"(\*\*[^*]+\*\*)"]
     [:italic #"[^*](\*[^*]+\*)[^*]"]
     [:monospace #"[^`](`[^`]+`)[^`]"]
     [:code-block #"(```[^`]+```)"]]))

(defn adjust-info
  ([info index] (adjust-info info index dec))
  ([info index f]
   (let [adjust-following #(if (>= % index) (f %) %)]
     (reduce
       (fn [aggregator category]
         (assoc
           aggregator
           category
           (mapv #(map adjust-following %) (info category))))
       {}
       (keys info)))))

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

(defn strip [string]
  (let [strip-lengths {:italic 1 :bold 2 :monospace 1 :code-block 3}
        unrolled-info (unroll-info (parse string))]
    (loop [s string
           i []
           u unrolled-info]
      (if (empty? u)
        {:input string
         :stripped s
         :markdown-info (roll-info i)}
        (let [[start end category] (first u)
              length (strip-lengths category)
              length-x2 (* 2 length)]
          (recur
            (str
              (subs s 0 start)
              (subs s (+ start length) (- end length))
              (subs s end))
            (conj i [start (- end length-x2) category])
            (map
              (fn [[x y c]] (list (- x length-x2) (- y length-x2) c))
              (rest u))))))))

