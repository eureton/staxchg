(ns staxchg.markdown
  (:gen-class))

(defn within? [ranges index]
  (some
    (fn [[from to]] (and (<= from index) (< index to)))
    ranges))

(defn categories [markdown-info index]
  (reduce
    (fn [aggregator [category ranges]]
      (if (within? ranges index)
        (conj aggregator category)
        aggregator))
    #{}
    markdown-info))

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
    [[:bold #"[^*](\*\*[^*]+\*\*)[^*]"]
     [:italic #"[^*](\*[^*]+\*)[^*]"]
     [:monospace #"[^`](`[^`]+`)[^`]"]
     [:code-block #"[^`](```[^`]+```)[^`]"]]))

