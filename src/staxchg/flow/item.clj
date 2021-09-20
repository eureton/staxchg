(ns staxchg.flow.item
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.hilite :as hilite])
  (:require [staxchg.plot :as plot])
  (:require [staxchg.dev :as dev])
  (:require [clojure.string :as string])
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(defn within?
  ""
  [x y {:as rect :keys [left top width height]}]
  (let [right (+ left width)
        bottom (+ top height)]
    (and
      (>= x left)
      (< x right)
      (>= y top)
      (< y bottom))))

(defn highlight-code
  ""
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
  [zone
   {:as item
    :keys [x y sub-zone raw]}]
  (when (= (item :type) :markdown)
    (let [options {:x x
                   :y y
                   :left (if sub-zone (sub-zone :left) 0)
                   :width (if sub-zone (sub-zone :width) (zone :width))}]
      (markdown/plot raw options))))

(def dispatch-fn-1 :type)

(def dispatch-fn-2 (fn [item _] (item :type)))

(defmulti payload dispatch-fn-1)

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

(defmulti line-count dispatch-fn-2)

(defmethod line-count :default
  [_ _]
  1)

(defmethod line-count :markdown
  [{:as item
    :keys [sub-zone raw]}
   zone]
  (let [width ((or sub-zone zone) :width)]
    (markdown/line-count raw width)))

(defmulti clip dispatch-fn-2)

(defmethod clip :default
  [{:as item :keys [x y raw]}
   rect]
  (assoc item :raw (if (within? x y rect) raw "")))

(defmethod clip :markdown
  [item rect]
  (let [clipper (fn [[_ [x y] _]]
                  (within? (+ x (item :x)) (+ y (item :y)) rect))]
    (update item :plot (partial filter clipper))))

(defmulti invisible? dispatch-fn-1)

(defmethod invisible? :default
  [{:keys [raw]}]
  (empty? raw))

(defmethod invisible? :markdown
  [{:keys [plot]}]
  (empty? plot))

(defmulti translate dispatch-fn-2)

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

