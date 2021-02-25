(ns staxchg.flow
  (:require [staxchg.flow.item :as item])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.dev :as dev])
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def zero {:items []})

(def y-rhythm 1)

(defn make
  ""
  [{:as args
    :keys [type x y raw foreground-color background-color modifiers scroll-delta
           scroll-offset sub-zone]
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
            :sub-zone sub-zone
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

(defn scroll-y
  ""
  [flow delta]
  (assoc flow :scroll-offset (- delta)))

(defn line-count
  ""
  [flow zone]
  (reduce + (map (partial item/line-count zone) (flow :items))))

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

(defn layout-y
  ""
  [{:as flow
    :keys [scroll-offset items]
    :or {scroll-offset 0}}
   zone]
  (let [plotted-items (map #(assoc % :plot (item/plot-markdown zone %)) items)
        line-counts (map (partial item/line-count zone) plotted-items)
        arith-prog-reducer (fn [acc x] (conj acc (+ x (last acc))))
        ys (reduce arith-prog-reducer [0] line-counts)]
    (->>
      plotted-items
      (map #(update %2 :y + %1 (- scroll-offset)) ys)
      (assoc flow :items))))

(defn translate-to-screen
  ""
  [flow
   {:as zone :keys [left top]}]
  (let [item-mapper #(->
                       %
                       (update :x + left)
                       (update :y + top))]
    (->>
      flow
      :items
      (map item-mapper)
      (assoc flow :items))))

(defn clip-to-screen
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(item/clip % rect))
    (assoc flow :items)))

(defn cull
  ""
  [flow]
  (->>
    flow
    :items
    (remove item/invisible?)
    (assoc flow :items)))

(defn translate-to-viewport
  ""
  [flow rect]
  (->>
    flow
    :items
    (map #(item/translate % rect))
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

