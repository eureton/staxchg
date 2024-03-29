(ns staxchg.recipe
  (:require [flatland.useful.fn :as ufn]
            [staxchg.flow :as flow]
            [staxchg.flow.item :as flow.item])
  (:import [com.googlecode.lanterna SGR TerminalPosition TerminalSize])
  (:gen-class))

(defn sub-graphics
  "Precursor to a lanterna TextGraphics object, which corresponds to the
   given terminal area.
   Returns a cookbook.recipe inflation tuple and expects a lanterna Screen."
  [{:keys [left top width height]}]
  [#(-> %
        .newTextGraphics
        (.newTextGraphics (TerminalPosition. left top)
                          (TerminalSize. width height)))
   :screen])

(defn fx-graphics
  "Precursor to a lanterna TextGraphics object, which corresponds to the
   given terminal area. The object is further configured with the given colors
   and modifiers.
   Returns a cookbook.recipe inflation tuple and expects a lanterna Screen."
  [dimensions
   {:keys [foreground-color background-color modifiers]}]
  [#(-> ((first (sub-graphics dimensions)) %)
        (.enableModifiers (into-array SGR modifiers))
        (.setForegroundColor foreground-color)
        (.setBackgroundColor background-color))
   :screen])

(defn clear-whole
  "Recipe step for clearing every cell in zone."
  [_
   {:as zone
    :keys [left top width height]}]
  (when (zone :clear?)
    [[:staxchg.io/clear! (sub-graphics zone) left top width height]]))

(defn scroll
  "Recipe step for scrolling existing lines within zone."
  [flow
   {:as zone
    :keys [top height]}]
  (when (flow/scrolled? flow)
    [[:staxchg.io/scroll! :screen
                          top
                          (+ top (dec height))
                          (flow :scroll-delta)]]))

(defn clear-scroll-gap
  "Recipe step for clearing that part of zone which is vacated by scrolling its
   existing lines. nil if no scrolling has occurred."
  [flow
   {:as zone
    :keys [left top width height]}]
  (when (flow/scrolled? flow)
    (let [{:keys [left top width height]
           :as subrect} (flow/scroll-gap-rect flow zone)]
      [[:staxchg.io/clear! (sub-graphics subrect) left top width height]])))

(defn put-payload
  "Recipe step for rendering the contents of flow into zone."
  [flow zone]
  (let [flow (flow/adjust flow zone)]
    (map (fn [item]
           [(case (item :type)
              :markdown :staxchg.io/put-plot!
              :characters :staxchg.io/put-plot!
              :string :staxchg.io/put-string!)
            (fx-graphics (flow/scroll-gap-rect flow zone) item)
            (flow.item/payload item)
            (select-keys item [:x :y])])
         (:items flow))))

(def make
  "Recipe for rendering the contents of flow into zone."
  (comp #(remove (some-fn empty? nil?) %)
        (ufn/ap concat)
        (juxt clear-whole scroll clear-scroll-gap put-payload)))

