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

(defn plot-transducer
  ""
  [_ zone]
  (map #(assoc % :plot (item/plot-markdown zone %))))

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

(defn translate-to-zone-transducer
  ""
  [_ {:keys [left top]}]
  (map #(-> %
            (update :x + left)
            (update :y + top))))

(defn hilite-transducer
  ""
  [_ _]
  (map #(assoc % :plot (item/highlight-code %))))

(defn clip-transducer
  ""
  [flow zone]
  (map #(item/clip % (scroll-gap-rect flow zone))))

(defn cull-transducer
  ""
  [_ _]
  (remove item/invisible?))

(defn translate-to-viewport-transducer
  ""
  [flow zone]
  (map #(item/translate % (scroll-gap-rect flow zone))))

(defn adjust-transducer
  ""
  [flow zone]
  (apply comp ((juxt plot-transducer
                     y-layout-transducer
                     translate-to-zone-transducer
                     hilite-transducer
                     clip-transducer
                     cull-transducer
                     translate-to-viewport-transducer) flow zone)))

(defn adjust
  ""
  [flow zone]
  (update flow :items #(eduction (adjust-transducer flow zone) %)))

