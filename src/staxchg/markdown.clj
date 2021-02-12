(ns staxchg.markdown
  (:require [clojure.string :as string])
  (:require [clojure.set])
  (:require [staxchg.flexmark :as flexmark])
  (:require [staxchg.ast :as ast])
  (:gen-class))

(def ontology (->
                (make-hierarchy)
                (derive :p :block)
                (derive :blist :block)
                (derive :olist :block)
                (derive :fenced-code-block :block)
                (derive :indented-code-block :block)
                (derive :html-block :block)
                (derive :ref :block)
                (derive :blitem :list-item)
                (derive :olitem :list-item)
                (derive :txt :inline)
                (derive :code :inline)
                (derive :strong :inline)
                (derive :html-inline :inline)
                atom))

(defmulti annotate (fn [node _] (node :tag)) :hierarchy ontology)

(defmethod annotate :olist
  [node _]
  (update node :children #(map-indexed
                            (fn [i item]
                              (assoc item :index (inc i) :list-size (count %)))
                            %)))

(defmethod annotate :default
  [node _]
  node)

(defn decorate [recipient traits & clauses]
  (let [effect-map (apply
                     zipmap
                     (map
                       (fn [f]
                         (map
                           second
                           (filter #(->> % first f) (map-indexed vector clauses))))
                       [even? odd?]))]
    (reduce
      (fn [aggregator trait] ((effect-map trait) aggregator))
      recipient
      (clojure.set/intersection (set (keys effect-map)) traits))))

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

(defn decorate-plot
  [plot & traits]
  (map
    #(apply update % 2 update :traits (comp set conj) traits)
    plot))

(defn dispatch-fn [node & _] (node :tag))

(defmulti plot-ast dispatch-fn :hierarchy ontology)

(defmulti next-at dispatch-fn :hierarchy ontology)

(defn reflow
  ""
  [string
   {:keys [x width height]
    :or {x 0}}]
  (let [truncate #(take (or height (count %)) %)]
    (as-> string v
      (string/split-lines v)
      (map
        (fn [line]
          (->>
            (pack line (- width x))
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
      (assoc v
         :reflowed (string/join "\r\n" (v :reflowed)))
       (dissoc v :length :breaks)
       (:reflowed v)
       )))

(defmethod next-at :inline
  [_
   plot
   {:keys [width]}]
  (let [[x y] (second (last plot))
        overrun? (>= (inc x) width)]
    {:x (if overrun? 0 (inc x))
     :y (if overrun? (inc y) y)}))

(defmethod next-at :sbr
  [_ _ {:keys [x y]}]
  {:x (+ x 2)
   :y y})

(defmethod next-at :hbr
  [_ _ {:keys [y]}]
  {:x 0
   :y (inc y)})

(defmethod next-at :tbr
  [_ plot _]
  (let [[_ y] (second (last plot))]
    {:x 0
     :y (inc y)}))

(defmethod next-at :block
  [_
   plot
   {:keys [top level]
    :or {top 0 level 0}}]
  (let [last-y (-> plot last second second (- top))]
    {:x 0
     :y (+ last-y (if (zero? level) 2 1))}))

(defmethod next-at :list-item
  [_ plot _]
  {:x 0
   :y (-> plot last second second inc)})

(defmethod next-at :default
  [_ _ _]
  {:x -1 :y -1})

(defn plot-horizontally
  ""
  ([x y n character]
   (plot-horizontally x y (repeat n character)))
  ([x y string]
   (->>
     (count string)
     range
     (map (partial + x))
     (map vector (repeat y))
     (map reverse)
     (map vector (seq string)))))

(defmethod plot-ast :txt
  [node
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (let [string (node :content)
        reflowed (reflow string {:x x :width width})
        lines (string/split-lines reflowed)
        truncate #(take (or height (count %)) %)
        lengths (->> lines flatten truncate (map count))]
    (map
      vector
      (seq (string/join lines))
      (->>
        lengths
        (map-indexed (fn [index length]
                       (->>
                         length
                         range
                         (map #(vector (+ % left (if (zero? index) x 0)) (+ index top y))))))
        (reduce concat)))))

(defmethod plot-ast :indented-code-block
  [node
   {:as options
    :keys [x y]
    :or {x 0 y 0}}]
  (->
    node
    (assoc :tag :txt)
    (update :content string/replace #"(?im)^(?:    |\t)" "")
    (plot-ast options)
    (decorate-plot :code)))

(defmethod plot-ast :tbr
  [node
   {:keys [y width]
    :or {y 0}}]
  (decorate-plot
    (plot-horizontally 0 y width \-)
    :horz))

(defmulti plot-list-item-decor (fn [tag _] tag) :hierarchy ontology)

(defmethod plot-list-item-decor :blitem
  [_ {:keys [x y]}]
  [[\+     [     x  y] {:traits #{:bullet}}]
   [\space [(inc x) y]                     ]])

(defmethod plot-list-item-decor :olitem
  [_ {:keys [index level x y list-size]}]
  (let [alphabetical-numeral #(-> % dec (+ (int \a)) char)
        alpha? (odd? level)
        figure-count (-> list-size Math/log10 Math/floor int inc)]
    (->>
      index
      ((if alpha? alphabetical-numeral str))
      (format (str "%" (if alpha? 1 figure-count) "s. "))
      (plot-horizontally x y))))

(defmethod plot-ast :list-item
  [node
   {:as options
    :keys [x y level]
    :or {x 0 y 0 level 0}}]
  (let [indent-length (* level 2)
        indent (plot-horizontally x y (string/join (repeat indent-length \space)))
        decor (plot-list-item-decor
                (dispatch-fn node)
                {:index (-> node :index)
                 :level level
                 :x (+ x indent-length)
                 :y y
                 :list-size (-> node :list-size)})
        inner-options (assoc
                        options
                        :x (+ x indent-length (count decor))
                        :level (inc level))
        inner (plot-ast (assoc node :tag :default) inner-options)]
    (concat indent decor inner)))

(defmethod plot-ast :sbr
  [_
   {:keys [x y]
    :or {x 0 y 0}}]
  [[\space [x y]] [\space [(inc x) y]]])

(defmethod plot-ast :hbr
  [_ _]
  [])

(defmethod plot-ast :default
  [node
   {:as options
    :keys [x y]
    :or {x 0 y 0}}]
  (loop [contents (node :children)
         result []
         origin {:x x :y y}]
    (if (empty? contents)
      result
      (let [head (first contents)
            panned-options (merge options origin)
            head-plot (plot-ast head panned-options)
            next-at (next-at head head-plot panned-options)]
        (recur (rest contents) (concat result head-plot) next-at)))))

(defmethod plot-ast :h
  [node options]
  (-> node (assoc :tag :default) (plot-ast options) (decorate-plot :h)))

(defmethod plot-ast :em
  [node options]
  (-> node (assoc :tag :default) (plot-ast options) (decorate-plot :em)))

(defmethod plot-ast :strong
  [node options]
  (-> node (assoc :tag :default) (plot-ast options) (decorate-plot :strong)))

(defmethod plot-ast :fenced-code-block
  [node options]
  (-> node (assoc :tag :default) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :html-inline
  [node options]
  (-> node (assoc :tag :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :ref
  [node options]
  (-> node (assoc :tag :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :html-block
  [node options]
  (-> node (assoc :tag :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :code
  [node options]
  (-> node (assoc :tag :default) (plot-ast options) (decorate-plot :code)))

(defn plot
  "Returns a sequence of pairs -one for each character of the input string-
   consisting of:
     * the character
     * the [x y] coordinates of the character"
  [string options]
  (->
    string
    flexmark/parse
    (ast/depth-first-walk annotate)
    (plot-ast options)))

(defn line-count
  [string width]
  (->>
    (plot string {:width width})
    ((juxt last first))
    (map second)
    (map second)
    (reduce -)
    inc))
