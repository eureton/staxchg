(ns staxchg.flow
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.dev :as dev])
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def zero {:items []})

(def y-rhythm 1)

(defn plot-markdown
  [{:as zone :keys [width]}
   {:as item :keys [x y raw]}]
  (when (= (item :type) :markdown)
    (let [{:keys [plotted markdown-info]} (markdown/plot
                                            raw
                                            {:x x :y y :width width})
          categories (->>
                       plotted
                       count
                       range
                       (map (partial markdown/categories markdown-info)))]
      (map conj plotted categories))))

(defn make
  ""
  [{:as args
    :keys [type x y raw foreground-color background-color modifiers scroll-delta scroll-offset]
    :or {type :string raw ""
         x 0 y 0 scroll-offset 0
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/DEFAULT
         modifiers []}}]
  {:scroll-delta scroll-delta
   :scroll-offset scroll-offset
   :items [{:type type
            :x x
            :y y
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
  [{:as item :keys [raw plot]}]
  (case (item :type)
    :string raw
    :markdown plot))

(defn payload-line-count
  ""
  [{:as zone
    :keys [width]}
   {:as item
    :keys [raw]}]
  (case (item :type)
    :markdown (markdown/line-count raw width)
    :string 1))

(defn scroll-y
  ""
  [flow delta]
  (assoc flow :scroll-offset (- delta)))

(defn line-count
  ""
  [flow zone]
  (reduce + (map (partial payload-line-count zone) (flow :items))))

(defn add
  ""
  ([]
   zero)
  ([flow]
   flow)
  ([flow1 flow2]
   (assoc
     flow1
     :items
     (apply concat (map :items [flow1 flow2]))))
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

(defn layout-y
  ""
  [{:as flow :keys [scroll-offset] :or {scroll-offset 0}}
   zone]
  (let [line-counts (map (partial payload-line-count zone) (flow :items))
        arith-prog-reducer (fn [acc x] (conj acc (+ x (last acc))))
        ys (reduce arith-prog-reducer [0] line-counts)]
    (->>
      flow
      :items
      (map #(assoc % :plot (plot-markdown zone %)))
      (map #(update %2 :y (partial + %1 (- scroll-offset))) ys)
      (assoc flow :items))))

(defn translate-to-screen
  ""
  [flow
   {:as zone :keys [left top]}]
  (let [item-mapper #(->
                       %
                       (update :x (partial + left))
                       (update :y (partial + top)))]
    (->>
      flow
      :items
      (map item-mapper)
      (assoc flow :items))))

(defn clip-to-screen-string-item
  ""
  [{:as item :keys [x y raw]}
   rect]
  (assoc item :raw (if (within? x y rect) raw "")))

(defn clip-to-screen-markdown-item
  ""
  [item rect]
  (assoc item :plot (filter
                      (fn [[_ [x y] _]] (within? (+ x (item :x)) (+ y (item :y)) rect))
                      (item :plot))))

(defn clip-to-screen-item
  ""
  [item rect]
  ((case (item :type)
     :string clip-to-screen-string-item
     :markdown clip-to-screen-markdown-item) item rect))

(defn clip-to-screen
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(clip-to-screen-item % rect))
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

(defn translate-to-viewport-string-item
  ""
  [item rect]
  (update item :y #(- % (rect :top))))

(defn translate-to-viewport-markdown-item
  ""
  [item rect]
  (let [translator (fn [[c [x y] cs]] [c [x (- (+ y (item :y)) (rect :top))] cs])]
    (update item :plot #(map translator %))))

(defn translate-to-viewport-item
  ""
  [item rect]
  ((case (item :type)
     :string translate-to-viewport-string-item
     :markdown translate-to-viewport-markdown-item) item rect))

(defn translate-to-viewport
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(translate-to-viewport-item % rect))
    (assoc flow :items)))

(defn scroll-gap-rect
  ""
  [{:as flow
    :keys [scroll-delta]}
   {:as zone
    :keys [left top width height clear?]}]
  (let [bottom (+ top height)
        gap-filler? (and (not clear?) (scrolled? flow))]
    {:left left
     :top (if (and gap-filler? (pos? scroll-delta))
            (- bottom scroll-delta)
            top)
     :width width
     :height (if gap-filler? (Math/abs scroll-delta) height)}))

(defn clip
  ""
  [flow zone]
  (let [viewport (scroll-gap-rect flow zone)]
    (->
      flow
      (layout-y zone)
      (translate-to-screen zone)
      (clip-to-screen viewport)
      (cull)
      (translate-to-viewport viewport))))

