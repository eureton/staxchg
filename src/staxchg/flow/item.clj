(ns staxchg.flow.item
  (:require [clojure.string :as string]
            [staxchg.hilite :as hilite]
            [staxchg.markdown :as markdown]
            [staxchg.plot :as plot])
  (:import [com.googlecode.lanterna TextColor$ANSI])
  (:gen-class))

(defn within?
  "True if the cell at (x, y) is within rect, false otherwise."
  [x y {:as rect :keys [left top width height]}]
  (let [right (+ left width)
        bottom (+ top height)]
    (and
      (>= x left)
      (< x right)
      (>= y top)
      (< y bottom))))

(defn highlight-code
  "Adds syntax traits to the items within plot, according to highlights."
  [{:keys [plot highlights]}]
  (reduce (fn [acc {:keys [from to] code-plot :plot}]
            (concat (take from acc)
                    (reduce #(try (-> (hilite/annotate %1 %2)
                                      (plot/strip-traits :code))
                                  (catch Exception _ %1))
                            code-plot
                            highlights)
                    (drop to acc)))
          plot
          (plot/cluster-by-trait plot :code)))

(defn plot-markdown
  "Plots the markdown string in item within the confines of zone. nil if item
   holds no markdown data."
  [zone
   {:as item
    :keys [x y sub-zone raw]}]
  (when (= (item :type) :markdown)
    (let [options {:x x
                   :y y
                   :left (if sub-zone (sub-zone :left) 0)
                   :width (if sub-zone (sub-zone :width) (zone :width))}]
      (markdown/plot raw options))))

(def dispatch-fn-1
  "Dispatch function for multimethods which expect a flow item."
  :type)

(def dispatch-fn-2
  "Dispatch function for multimethods which expect a flow item plus one more
   argument."
  (fn [item _] (item :type)))

(defmulti payload
  "Data which is suitable for sending to an io function, i.e. can be rendered
   as is - without further processing."
  dispatch-fn-1)

(defmethod payload :string
  [{:keys [raw]}]
  raw)

(defmethod payload :characters
  [{:keys [raw x y]}]
  (map-indexed (fn [index [character extras]]
                 [character [(+ x index) y] extras])
               raw))

(defmethod payload :markdown
  [{:keys [plot]}]
  plot)

(defmulti line-count
  "The number of lines this flow item will span within its designated zone."
  dispatch-fn-2)

(defmethod line-count :default
  [_ _]
  1)

(defmethod line-count :markdown
  [{:as item
    :keys [sub-zone raw]}
   zone]
  (let [width ((or sub-zone zone) :width)]
    (markdown/line-count raw width)))

(defmulti clip
  "Removes data which lies outside rect."
  dispatch-fn-2)

(defmethod clip :default
  [{:as item :keys [x y raw]}
   rect]
  (assoc item :raw (if (within? x y rect) raw "")))

(defmethod clip :markdown
  [item rect]
  (let [clipper (fn [[_ [x y] _]]
                  (within? (+ x (item :x)) (+ y (item :y)) rect))]
    (update item :plot (partial filter clipper))))

(defmulti invisible?
  "True if the entire flow item has been clipped, false otherwise."
  dispatch-fn-1)

(defmethod invisible? :default
  [{:keys [raw]}]
  (empty? raw))

(defmethod invisible? :markdown
  [{:keys [plot]}]
  (empty? plot))

(defmulti translate
  "Translates data to use the top-left corner of rect as origin."
  dispatch-fn-2)

(defmethod translate :default
  [item
   {:keys [left top]}]
  (-> item
      (update :x - left)
      (update :y - top)))

(defmethod translate :markdown
  [item
   {:keys [top]}]
  (let [translator (fn [[c [x y] cs]]
                     [c [x (- (+ y (item :y)) top)] cs])]
    (update item :plot #(map translator %))))

