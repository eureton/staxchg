(ns staxchg.plot
  (:require [clojure.string :as string])
  (:require [clojure.set])
  (:require [staxchg.plot.item :as item])
  (:require [staxchg.code :as code])
  (:require [staxchg.string])
  (:gen-class))

(def ontology
  "Hierarchy depiciting abstractions useful for multimethods in this namespace."
  (-> (make-hierarchy)
      (derive :p :block)
      (derive :list :block)
      (derive :ofcblk :block)
      (derive :icblk :block)
      (derive :html-block :block)
      (derive :bq :block)
      (derive :h :block)
      (derive :txt :inline)
      (derive :sbr :inline)
      (derive :cs :inline)
      (derive :strong :inline)
      (derive :em :inline)
      (derive :a :inline)
      (derive :url :inline)
      (derive :html-inline :inline)
      (derive :ofcblk :code-block)
      (derive :icblk :code-block)
      (derive :atxh :h)
      (derive :stxh :h)
      atom))

(defn straight
  "The [string] variant plots the string on a single row, starting at (x, y).
   The [n character] variant does the same for character, n consecutive times."
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
  "Applies traits throughout plot."
  [plot & traits]
  (map #(item/decorate % traits) plot))

(defn ast-dispatch-fn
  "Dispatch function for multimethods in this namespace."
  [node & _]
  (-> node :data :tag))

(defmulti next-at
  "Coordinates at which to plot the next character of node, given:
     * what has been plotted so far
     * the rectangle within which to plot

   Packs results within a hash under keys :x and :y."
  ast-dispatch-fn
  :hierarchy ontology)

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

(defmethod next-at :li
  [_ plot {:keys [top] :or {top 0}}]
  {:x 0
   :y (-> plot last second second (- top) inc)})

(defmethod next-at :default
  [_ _ _]
  {:x -1 :y -1})

(defn list-item-decor
  "Plot suitable for decorating list items. Meant to be prepended to the text."
  [_ {:keys [index level x y list-size]}]
  (if index
    (let [alphabetical-numeral #(-> % dec (+ (int \a)) char)
          alpha? (odd? level)
          figure-count (-> list-size Math/log10 Math/floor int inc)]
      (->>
        index
        ((if alpha? alphabetical-numeral str))
        (format (str "%" (if alpha? 1 figure-count) "s. "))
        (straight x y)))
    [[\+     [     x  y] {:traits #{:bullet}}]
     [\space [(inc x) y]                     ]]))

(defn retag
  "Sets the markdown tag. Useful in forcing the node through a handler which
   doesn't match the original tag, when calling multimethods."
  ([node tag]
   (assoc-in node [:data :tag] tag)))

(defn untag
  "Sets the markdown tag to :default, effectively untagging the node. Useful in
   forcing the node through the default handler of multimethods."
  [node]
  (retag node :default))

(defmulti ast
  "Plots the AST whose root is node, given the following parameters:
        top: y coordinate of the top edge of rect
       left: x coordinate of the left edge of rect
      width: width of rect
     height: height of rect
          x: number of columns to indent the first line of the plot by
          y: number of rows to vertically offset the plot by
   where rect is the rectangle to plot into.

   In addition:
     * top, left, x and y must belong to the same coordinate space
     * the coordinate space must be absolute, i.e. same as that of the canvas

   Packs the characters of node into a sequence. Each character is represented
   by a vector consisting of:
     * the character itself
     * a tuple of coordinates in the same space as the target canvas
     * a hash for collecting meta information, e.g. visual effects

   For example:
     => (ast {:data {:tag :cs :content \"abc\"}} {:left 5 :width 10})
     ([\\a (5 0) {:traits #{:standout}}]
      [\\b (6 0) {:traits #{:standout}}]
      [\\c (7 0) {:traits #{:standout}}])"
  ast-dispatch-fn
  :hierarchy ontology)

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
      (-> node :data :content) v
      (staxchg.string/reflow v {:x x :width width})
      (string/split-lines v)
      (truncate v)
      (map-indexed line-plotter v)
      (reduce concat v))))

(defmethod ast :tbr
  [_
   {:keys [y width]
    :or {y 0}}]
  (decorate
    (straight 0 y width \-)
    :horz))

(defmethod ast :li
  [node
   {:as options
    :keys [x y level width]
    :or {x 0 y 0 level 0}}]
  (let [indent-length (* level 2)
        indent (straight x y indent-length \space)
        decor (list-item-decor
                (ast-dispatch-fn node)
                {:index (-> node :data :index)
                 :level level
                 :x (+ x indent-length)
                 :y y
                 :list-size (-> node :data :list-size)})
        inner-x-offset (+ indent-length (count decor))
        inner-options (assoc
                        options
                        :left (+ x inner-x-offset)
                        :width (- width inner-x-offset)
                        :level (inc level))
        inner (ast (untag node) inner-options)]
    (concat indent decor inner)))

(defmethod ast :bq
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
        inner (ast (untag node) inner-options)]
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
  (-> node untag (ast options) (decorate :h)))

(defmethod ast :em
  [node options]
  (-> node untag (ast options) (decorate :em)))

(defmethod ast :strong
  [node options]
  (-> node untag (ast options) (decorate :strong)))

(defmethod ast :code-block
  [node options]
  (let [syntax (some not-empty [(-> node :data :info) (:syntax options)])]
    (-> node
        (retag :txt)
        (update-in [:data :content] code/expand-tabs syntax)
        (ast options)
        (decorate :code))))

(defmethod ast :html-inline
  [node options]
  (-> node (retag :txt) (ast options) (decorate :standout)))

(defmethod ast :url
  [node options]
  (-> node (retag :txt) (ast options) (decorate :standout)))

(defmethod ast :html-block
  [node options]
  (-> node (retag :txt) (ast options) (decorate :standout)))

(defmethod ast :cs
  [node options]
  (-> node (retag :txt) (ast options) (decorate :standout)))

(defn cluster-rf
  "Reducer for staxchg.plot/cluster-by."
  [agg [index plot-item]]
  (let [previous (dec index)]
    (-> agg
        (update previous (comp vec conj) plot-item)
        (clojure.set/rename-keys {previous index}))))

(defn cluster-by
  "Clusters adjunct plot items by the result of applying f to the item.

   For each such cluster, a hash is returned. This hash contains keys:
    * :from
    * :to
    * :plot
    * :value

   These hold values, respectively:
    * start index (inclusive)
    * end index (exclusive)
    * corresponding plot
    * value of (f item)

   Returns hashes in a sequence, sorted by :from ascending."
  [f plot]
  (->> plot
       (map-indexed vector)
       (group-by (comp f second))
       (reduce-kv (fn [agg k0 v0]
                    (->> (reduce cluster-rf {} v0)
                         (map (fn [[k v]] {:plot v
                                           :from (- k (dec (count v)))
                                           :to (inc k)
                                           :value k0}))
                         (concat agg))) [])
       (sort-by :from)))

(defn strip-traits
  "Removes traits from all items of plot."
  [plot & traits]
  (map #(item/strip % traits) plot))

(defn cluster-by-trait
  "Clusters adjunct plot items which share the given trait.

   For each such cluster, a hash is returned. See cluster-by for details on the
   hash format. Removes the :value key from the hashes."
  [plot trait]
  (->> plot
       (cluster-by #(get-in % [2 :traits]))
       (filter (comp #(contains? % trait) :value))
       (map #(dissoc % :value))))

(defn string
  "String consisting of the characters in the plot. Honors whitespace. Joins
   lines using the given separator."
  [separator plot]
  (let [cluster-to-line (comp string/join
                              #(map first %)
                              :plot)]
    (->> plot
         (cluster-by (comp second second))
         (map cluster-to-line)
         (string/join separator))))

(defn text
  "Shorthand for (string \"\r\n\" plot)."
  [plot]
  (string "\r\n" plot))

