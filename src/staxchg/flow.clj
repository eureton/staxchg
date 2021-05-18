(ns staxchg.flow
  (:require [staxchg.flow.item :as item])
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def zero {:items []})

(def y-rhythm 1)

(defn make
  ""
  [{:as args
    :keys [type x y raw foreground-color background-color modifiers scroll-delta
           scroll-offset sub-zone code-highlights]
    :or {type :string raw ""
         x 0 y 0 scroll-offset 0
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/DEFAULT
         modifiers []
         code-highlights []}}]
  {:scroll-delta scroll-delta
   :scroll-offset scroll-offset
   :items [{:type type
            :x x
            :y y
            :sub-zone sub-zone
            :raw raw
            :code-highlights code-highlights
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
  [{:as flow
    :keys [items]}
   zone]
  (reduce
    +
    (map #(item/line-count % zone) items)))

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

(defn y-layout-transducer
  ""
  [{:as flow
    :keys [scroll-offset]
    :or {scroll-offset 0}}
   zone]
  (fn [rf]
    (let [line-count (volatile! 0)]
      (fn
        ([] (rf))
        ([acc] (rf acc))
        ([acc x]
         (let [previous (vswap! line-count identity)]
           (vswap! line-count + (item/line-count x zone))
           (rf acc (update x :y + previous (- scroll-offset)))))))))

(defn adjust
  ""
  [flow zone]
  (let [viewport (scroll-gap-rect flow zone)
        xform (comp (map #(assoc % :plot (item/plot-markdown zone %)))
                    (y-layout-transducer flow zone)
                    (map #(-> %
                              (update :x + (:left zone))
                              (update :y + (:top zone))))
                    (map #(assoc % :plot (item/highlight-code %)))
                    (map #(item/clip % viewport))
                    (remove item/invisible?)
                    (map #(item/translate % viewport)))]
    (update flow :items #(eduction xform %))))

