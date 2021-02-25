(ns staxchg.flow.item
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.dev :as dev])
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

(defmulti payload :type)

(defmethod payload :string
  [{:keys [raw]}]
  raw)

(defmethod payload :markdown
  [{:keys [plot]}]
  plot)

(defmulti line-count (fn [_ item] (item :type)))

(defmethod line-count :string
  [_ _]
  1)

(defmethod line-count :markdown
  [zone
   {:as item
    :keys [sub-zone raw]}]
  (let [width ((or sub-zone zone) :width)]
    (markdown/line-count raw width)))

(defmulti clip (fn [item _] (item :type)))

(defmethod clip :string
  [{:as item :keys [x y raw]}
   rect]
  (assoc item :raw (if (within? x y rect) raw "")))

(defmethod clip :markdown
  [item rect]
  (let [clipper (fn [[_ [x y] _]]
                  (within? (+ x (item :x)) (+ y (item :y)) rect))]
    (update item :plot (partial filter clipper))))

(defmulti invisible? :type)

(defmethod invisible? :string
  [{:keys [raw]}]
  (empty? raw))

(defmethod invisible? :markdown
  [{:keys [plot]}]
  (empty? plot))

(defmulti translate (fn [item _] (item :type)))

(defmethod translate :string
  [item rect]
  (-> item
      (update :x - (rect :left))
      (update :y - (rect :top))))

(defmethod translate :markdown
  [item rect]
  (let [translator (fn [[c [x y] cs]]
                     [c [x (- (+ y (item :y)) (rect :top))] cs])]
    (update item :plot #(map translator %))))

