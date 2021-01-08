(ns staxchg.flow
  (:require [staxchg.markdown :as markdown])
  (:gen-class))

(def zero [])

(def y-rhythm 1)

(defn plot-markdown
  [string
   {:as args
    :keys [x y width]
    :or {x 0 y 0}}]
  (let [{:keys [plotted markdown-info]} (markdown/plot
                                          string
                                          {:x x :y y :left 0 :top 0 :width width})
        categories (->>
                     plotted
                     count
                     range
                     (map (partial markdown/categories markdown-info)))]
    (map conj plotted categories)))

(defn clip-plot
  [plot y-offset {:keys [top height]}]
  (let [clipped? (fn [[_ [_ y] _]] (or (< y top) (>= y (+ top height))))]
    (->>
      plot
      (map (fn [[c [x y] cs]] [c [x (- y y-offset)] cs]))
      (remove clipped?)
      (map (fn [[c [x y] cs]] [c [x (- y top)] cs]))
      )))

(defn make
  ""
  [{:keys [type x y y-offset raw foreground-color background-color modifiers scrolled-by]
    :viewport/keys [left top width height]
    :or {type :string
         x 0
         y 0
         scrolled-by nil
         ;y-offset 0
         left 0
         top 0
         width 0
         height 0
         raw ""
         modifiers []}}]
  [{:type type
    :x x
    :y y
    ;:y-offset y-offset
    :scrolled-by scrolled-by
    :viewport/left left
    :viewport/top top
    :viewport/width width
    :viewport/height height
    :raw raw
    :foreground-color foreground-color
    :background-color background-color
    :modifiers modifiers}])

(def y-separator (make {}))

(defn scrolled?
  [flow]
  (if-some [head (first flow)]
    ((every-pred some? (complement zero?)) (head :y-offset))
    false))

(defn payload
  ""
  [{:as item
    :keys [x y y-offset raw]
    :viewport/keys [width height]
    :or {y-offset 0}}
   bbox]
  (case (item :type)
    :string raw
    :markdown (->
                raw
                (plot-markdown {:x x :y y :width width})
                (clip-plot y-offset bbox))))

(defn payload-line-count
  ""
  [{:as item
    :keys [raw]
    :viewport/keys [width]}]
  (case (item :type)
    :markdown (markdown/line-count raw width)
    :string 1))

(defn y-offset
  ""
  [flow delta]
  (mapv
    #(update % :y (partial + delta))
    flow))

(defn y-scroll
  ""
  [flow delta]
  (mapv #(assoc % :y-offset (- delta)) flow))

(defn line-count
  ""
  [flow]
  (reduce + (map payload-line-count flow)))

(defn add
  ""
  ([]
   zero)
  ([p]
   p)
  ([p1 p2]
   (concat p1 (y-offset p2 (line-count p1))))
  ([p1 p2 & more]
   (reduce add (add p1 p2) more)))

