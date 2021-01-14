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

(defn blank?
  ""
  [{:keys [raw]}]
  (clojure.string/blank? raw))

(defn force-clear
  ""
  [flow rect]
  (assoc flow :force-clear rect))

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

(defn apply-scroll
  ""
  [{:as flow :keys [scroll-offset] :or {scroll-offset 0}}
   zone]
  (let [line-counts (map (partial payload-line-count zone) (flow :items))
        ys (reduce (fn [acc x] (conj acc (+ x (or (last acc) 0)))) [0] line-counts)]
    (->>
      flow
      :items
      (map #(assoc % :plot (plot-markdown zone %)))
      (map #(update %2 :y (partial + %1 (- scroll-offset))) ys)
      (assoc flow :items))))

(defn translate-into-screen
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
    :keys [scroll-delta]}
   {:as zone
    :keys [left top width height]}]
  (let [bottom (+ top height)
        gap-filler? (scrolled? flow)]
    {:left left
     :top (if (and gap-filler? (pos? scroll-delta))
            (- bottom scroll-delta)
            top)
     :width width
     :height (if gap-filler? (Math/abs scroll-delta) height)}))

(defn visible-subset
  ""
  [flow zone]
  (let [viewport (scroll-gap-rect flow zone)]
    (dev/log "SGr: " zone)
    (->
      flow
      (apply-scroll zone)
      (translate-into-screen zone)
      (clip viewport)
      cull
      (pan viewport)
      )))

