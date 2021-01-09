(ns staxchg.flow
  (:require [staxchg.markdown :as markdown])
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def zero {:items []})

(def y-rhythm 1)

(defn plot-markdown
  [string
   {:as args
    :keys [x y left width]
    :or {x 0 y 0}}]
  (let [{:keys [plotted markdown-info]} (markdown/plot
                                          string
                                          {:x x :y y :left left :top 0 :width width})
        categories (->>
                     plotted
                     count
                     range
                     (map (partial markdown/categories markdown-info)))]
    (map conj plotted categories)))

(defn clip-plot
  [plot scroll-offset {:keys [top height]}]
  (let [clipped? (fn [[_ [_ y] _]] (or (< y top) (>= y (+ top height))))]
    (->>
      plot
      (map (fn [[c [x y] cs]] [c [x (- y scroll-offset)] cs]))
      (remove clipped?)
      (map (fn [[c [x y] cs]] [c [x (- y top)] cs])))))

(defn make
  ""
  [{:as args
    :keys [type x y raw foreground-color background-color modifiers scroll-delta scroll-offset]
    :viewport/keys [left top width height]
    :or {type :string raw ""
         x 0 y 0 left 0 top 0 width 0 height 0 scroll-offset 0
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/DEFAULT
         modifiers []}}]
  {:scroll-delta scroll-delta
   :scroll-offset scroll-offset
   :items [{:type type
            :x x
            :y y
            :viewport/left left
            :viewport/top top
            :viewport/width width
            :viewport/height height
            :raw raw
            :foreground-color foreground-color
            :background-color background-color
            :modifiers modifiers}]})

(def y-separator (make {}))

(defn scrolled?
  [{:keys [items scroll-delta]}]
  (boolean
    (and
      (not-empty items)
      (some? scroll-delta)
      ((complement zero?) scroll-delta))))

(defn payload
  ""
  [{:keys [scroll-offset]}
   bounding-left
   {:as item
    :keys [x y raw]
    :viewport/keys [left width height]}
   bbox]
  (case (item :type)
    :string raw
    :markdown (->
                raw
                (plot-markdown {:x x :y y :left (- left bounding-left) :width width})
                (clip-plot scroll-offset bbox))))

(defn blank?
  ""
  [{:keys [raw]}]
  (clojure.string/blank? raw))

(defn bounding-rect
  ""
  [flow]
  (let [items (remove blank? (flow :items))
        left (->> items (map :viewport/left) (apply min))
        right (apply max (map + (map :viewport/left items) (map :viewport/width items)))
        top (->> items (map :viewport/top) (apply min))
        bottom (apply max (map + (map :viewport/top items) (map :viewport/height items)))]
    {:left left
     :top top
     :width (- right left)
     :height (- bottom top)}))

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
  [flow offset]
  (->>
    flow
    :items
    (map #(update % :y (partial + offset)))
    (assoc flow :items)))

(defn y-scroll
  ""
  [flow delta]
  (assoc flow :scroll-offset (- delta)))

(defn line-count
  ""
  [flow]
  (reduce + (map payload-line-count (flow :items))))

(defn add
  ""
  ([]
   zero)
  ([flow]
   flow)
  ([flow1 flow2]
   (let [flow2-after (y-offset flow2 (line-count flow1))]
     (assoc
       flow1
       :items
       (apply concat (map :items [flow1 flow2-after])))))
  ([flow1 flow2 & more]
   (reduce add (add flow1 flow2) more)))

