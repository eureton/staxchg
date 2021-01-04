(ns staxchg.flow
  (:require [staxchg.markdown :as markdown])
  (:gen-class))

(def zero [])

(def y-rhythm 1)

(defn make
  ""
  [{:keys [type x y payload foreground-color modifiers]
    :viewport/keys [left top width height]
    :or {type :string
         x 0
         y 0
         left 0
         top 0
         width 0
         height 0
         payload ""
         modifiers []}}]
  [{:type type
    :x x
    :y y
    :viewport/left left
    :viewport/top top
    :viewport/width width
    :viewport/height height
    :payload payload
    :foreground-color foreground-color
    :modifiers modifiers}])

(def y-separator (make {}))

(defn payload-line-count
  ""
  [{:keys [type payload] :viewport/keys [width]}]
  (condp = type
    :markdown (markdown/line-count payload width)
    :string 1))

(defn y-offset
  ""
  [flow delta]
  (mapv
    #(update % :y (partial + delta))
    flow))

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

