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
            :plot (when (= type :markdown)
                    (plot-markdown raw {:x x :y y :left left :top 0 :width width}))
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

(defn blank?
  ""
  [{:keys [raw]}]
  (clojure.string/blank? raw))

(defn force-clear
  ""
  [flow rect]
  (assoc flow :force-clear rect))

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

(defn payload
  ""
  [{:as item :keys [raw plot]}]
  (case (item :type)
    :string raw
    :markdown plot))

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

(defn apply-scroll
  ""
  [{:as flow :keys [scroll-offset] :or {scroll-offset 0}}]
  (->>
    flow
    :items
    (map #(update % :y (partial + (- scroll-offset))))
    (assoc flow :items)))

(defn translate-into-screen
  ""
  [flow]
  (let [{bounding-left :left
         bounding-top :top} (bounding-rect flow)
        item-mapper #(let [left (% :viewport/left)
                           top (% :viewport/top)]
                       (->
                         %
                         (update :x (partial + left (- left bounding-left)))
                         (update :y (partial + top (- top bounding-top)))))]
    (->>
      flow
      :items
      (map item-mapper)
      (assoc flow :items))))

(defn clip-string-item
  ""
  [{:as item :keys [x y raw]}
   rect]
  (assoc item :raw (if (within? x y rect) raw "")))

(defn clip-markdown-item
  ""
  [item rect]
  (assoc item :plot (filter
                      (fn [[_ [x y] _]] (within? (+ x (item :x)) (+ y (item :y)) rect))
                      (item :plot))))

(defn clip-item
  ""
  [item rect]
  ((case (item :type)
     :string clip-string-item
     :markdown clip-markdown-item) item rect))

(defn clip
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(clip-item % rect))
    (assoc flow :items)))

(defn invisible?
  ""
  [item]
  (case (item :type)
    :string (empty? (item :raw))
    :markdown (empty? (item :plot))))

(defn cull
  ""
  [flow]
  (->>
    flow
    :items
    (remove invisible?)
    (assoc flow :items)))

(defn pan-string-item
  ""
  [item rect]
  (update item :y #(- % (rect :top))))

(defn pan-markdown-item
  ""
  [item rect]
  (let [translator (fn [[c [x y] cs]] [c [x (- (+ y (item :y)) (rect :top))] cs])]
    (update item :plot #(map translator %))))

(defn pan-item
  ""
  [item rect]
  ((case (item :type)
     :string pan-string-item
     :markdown pan-markdown-item) item rect))

(defn pan
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(pan-item % rect))
    (assoc flow :items)))

(defn scroll-gap-rect
  ""
  [{:as flow
    :keys [scroll-delta]}]
  (let [{:keys [left top width height]} (bounding-rect flow)
        bottom (+ top height)
        gap-filler? (scrolled? flow)]
    {:left left
     :top (if (and gap-filler? (pos? scroll-delta))
            (- bottom scroll-delta)
            top)
     :width width
     :height (if gap-filler? (Math/abs scroll-delta) height)}))

(defn visible-subset
  ""
  [{:as flow :keys [scroll-offset]}]
  (let [rect (scroll-gap-rect flow)]
    (->
      flow
      apply-scroll
      translate-into-screen
      (clip rect)
      cull
      (pan rect)
      )))

