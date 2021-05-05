(ns staxchg.plot
  (:require [clojure.string :as string])
  (:require [staxchg.string])
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
                (derive :block-quot :block)
                (derive :html-comment-block :block)
                (derive :h :block)
                (derive :blitem :list-item)
                (derive :olitem :list-item)
                (derive :txt :inline)
                (derive :sbr :inline)
                (derive :code :inline)
                (derive :strong :inline)
                (derive :em :inline)
                (derive :link :inline)
                (derive :url :inline)
                (derive :html-inline :inline)
                (derive :link-ref :inline)
                (derive :html-entity :inline)
                atom))

(defn straight
  ""
  ([x y n character]
   (straight x y (repeat n character)))
  ([x y string]
   (->>
     (count string)
     range
     (map (partial + x))
     (map vector (repeat y))
     (map reverse)
     (map vector (seq string)))))

(defn decorate
  [plot & traits]
  (map
    #(apply update % 2 update :traits (comp set conj) traits)
    plot))

(defn ast-dispatch-fn [node & _] (node :tag))

(defmulti next-at ast-dispatch-fn :hierarchy ontology)

(defmethod next-at :inline
  [_
   plot
   {:keys [left top] :or {left 0 top 0}}]
  (let [[x y] (->> plot last second (map + [(- left) (- top)]))]
    {:x (inc x)
     :y y}))

(defmethod next-at :hbr
  [_ _ {:keys [y]}]
  {:x 0
   :y (inc y)})

(defmethod next-at :tbr
  [_ plot {:keys [top] :or {top 0}}]
  {:x 0
   :y (-> plot last second second (- top) inc)})

(defmethod next-at :block
  [_
   plot
   {:keys [top level]
    :or {top 0 level 0}}]
  (let [last-y (-> plot last second second (- top))]
    {:x 0
     :y (+ last-y (if (zero? level) 2 1))}))

(defmethod next-at :list-item
  [_ plot {:keys [top] :or {top 0}}]
  {:x 0
   :y (-> plot last second second (- top) inc)})

(defmethod next-at :default
  [_ _ _]
  {:x -1 :y -1})

(defmulti list-item-decor (fn [tag _] tag) :hierarchy ontology)

(defmethod list-item-decor :blitem
  [_ {:keys [x y]}]
  [[\+     [     x  y] {:traits #{:bullet}}]
   [\space [(inc x) y]                     ]])

(defmethod list-item-decor :olitem
  [_ {:keys [index level x y list-size]}]
  (let [alphabetical-numeral #(-> % dec (+ (int \a)) char)
        alpha? (odd? level)
        figure-count (-> list-size Math/log10 Math/floor int inc)]
    (->>
      index
      ((if alpha? alphabetical-numeral str))
      (format (str "%" (if alpha? 1 figure-count) "s. "))
      (straight x y))))

(defmulti ast ast-dispatch-fn :hierarchy ontology)

(defmethod ast :txt
  [node
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (let [truncate #(take (or height (count %)) %)
        line-plotter (fn [i line] (straight
                                    (+ left (if (zero? i) x 0))
                                    (+ top y i)
                                    line))]
    (as->
      (node :content) v
      (staxchg.string/reflow v {:x x :width width})
      (string/split-lines v)
      (truncate v)
      (map-indexed line-plotter v)
      (reduce concat v))))

(defmethod ast :indented-code-block
  [node
   {:as options
    :keys [x y]
    :or {x 0 y 0}}]
  (->
    node
    (assoc :tag :txt)
    (update :content string/replace #"(?im)^(?:    |\t)" "")
    (ast options)
    (decorate :code)))

(defmethod ast :tbr
  [node
   {:keys [y width]
    :or {y 0}}]
  (decorate
    (straight 0 y width \-)
    :horz))

(defmethod ast :list-item
  [node
   {:as options
    :keys [x y level width]
    :or {x 0 y 0 level 0}}]
  (let [indent-length (* level 2)
        indent (straight x y indent-length \space)
        decor (list-item-decor
                (ast-dispatch-fn node)
                {:index (-> node :index)
                 :level level
                 :x (+ x indent-length)
                 :y y
                 :list-size (-> node :list-size)})
        inner-x-offset (+ indent-length (count decor))
        inner-options (assoc
                        options
                        :left (+ x inner-x-offset)
                        :width (- width inner-x-offset)
                        :level (inc level))
        inner (ast (assoc node :tag :default) inner-options)]
    (concat indent decor inner)))

(defmethod ast :block-quot
  [node
   {:as options
    :keys [x y width]
    :or {x 0 y 0}}]
  (let [decor (straight x y "> ")
        inner-x-offset (count decor)
        inner-options (assoc
                        options
                        :left (+ x inner-x-offset)
                        :width (- width inner-x-offset))
        inner (ast (assoc node :tag :default) inner-options)]
    (concat decor inner)))

(defmethod ast :sbr
  [_
   {:keys [x y left top]
    :or {x 0 y 0 left 0 top 0}}]
  (straight (+ left x) (+ top y) 2 \space))

(defmethod ast :hbr
  [_ _]
  [])

(defmethod ast :default
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
            head-plot (ast head panned-options)
            next-at (next-at head head-plot panned-options)]
        (recur (rest contents) (concat result head-plot) next-at)))))

(defmethod ast :h
  [node options]
  (-> node (assoc :tag :default) (ast options) (decorate :h)))

(defmethod ast :em
  [node options]
  (-> node (assoc :tag :default) (ast options) (decorate :em)))

(defmethod ast :strong
  [node options]
  (-> node (assoc :tag :default) (ast options) (decorate :strong)))

(defmethod ast :fenced-code-block
  [node options]
  (-> node (assoc :tag :default) (ast options) (decorate :code)))

(defmethod ast :html-inline
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :standout)))

(defmethod ast :link-ref
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :standout)))

(defmethod ast :ref
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :standout)))

(defmethod ast :url
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :standout)))

(defmethod ast :html-block
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :standout)))

(defmethod ast :html-comment-block
  [node options]
  (-> node (assoc :tag :txt) (ast options) (decorate :comment)))

(defmethod ast :html-entity
  [node options]
  (-> node (assoc :tag :txt) (ast options)))

(defmethod ast :code
  [node options]
  (-> node (assoc :tag :default) (ast options) (decorate :code)))

(defn cluster-rf
  ""
  [agg [index plot-item]]
  (let [previous (dec index)]
    (-> agg
        (update previous (comp vec conj) plot-item)
        (clojure.set/rename-keys {previous index}))))

(defn strip-traits
  ""
  [plot & traits]
  (map #(update-in % [2 :traits] (partial apply disj) traits) plot))

(defn cluster-by-trait
  ""
  ([plot
    trait
    {:as options
     :keys [remove-trait?]
     :or {remove-trait? false}}]
   (->> plot
        (map-indexed vector)
        (filter (fn [[_ [_ _ {:keys [traits]}]]] (contains? traits trait)))
        (reduce cluster-rf {})
        (map (fn [[k v]] {:plot (cond-> v
                                  remove-trait? (strip-traits trait))
                          :from (- k (dec (count v)))
                          :to k}))))
  ([plot trait]
   (cluster-by-trait plot trait {})))

(defn map-sub
  ""
  [plot from to f & args]
  (let [at-start #(split-at from %)
        at-end #(split-at to %)]
    (apply concat ((juxt (comp first at-start)
                         (comp #(apply f % args) second at-start first at-end)
                         (comp second at-end)) plot))))

