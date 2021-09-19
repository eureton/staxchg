(ns staxchg.recipe
  (:require [staxchg.flow :as flow])
  (:require [staxchg.flow.item :as flow.item])
  (:require [staxchg.dev :as dev])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:gen-class))

(defn sub-graphics
  "Precursor to a lanterna TextGraphics object, which corresponds to the
   given terminal area.
   Returns a cookbook.step inflation tuple and expects a lanterna Screen."
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
   Returns a cookbook.step inflation tuple and expects a lanterna Screen."
  [dimensions
   {:keys [foreground-color background-color modifiers]}]
  [#(-> ((first (sub-graphics dimensions)) %)
        (.enableModifiers (into-array SGR modifiers))
        (.setForegroundColor foreground-color)
        (.setBackgroundColor background-color))
   :screen])

(defn clear-whole
  ""
  [flow
   {:as zone
    :keys [left top width height]}]
  (when (zone :clear?)
    {:function :staxchg.io/clear!
     :params [(sub-graphics zone) left top width height]}))

(defn scroll
  ""
  [flow
   {:as zone
    :keys [top height]}]
  (when (flow/scrolled? flow)
    {:function :staxchg.io/scroll!
     :params [:screen
              top
              (+ top (dec height))
              (flow :scroll-delta)]}))

(defn clear-scroll-gap
  ""
  [flow
   {:as zone
    :keys [left top width height]}]
  (when (flow/scrolled? flow)
    {:function :staxchg.io/clear!
     :params [(sub-graphics (flow/scroll-gap-rect flow zone))
              left
              top
              width
              height]}))

(defn put-payload
  ""
  [flow zone item]
  {:function (case (item :type)
               :markdown :staxchg.io/put-plot!
               :characters :staxchg.io/put-plot!
               :string :staxchg.io/put-string!)
   :params [(fx-graphics (flow/scroll-gap-rect flow zone) item)
            (flow.item/payload item)
            (select-keys item [:x :y])]})

(defn make
  ""
  [{:keys [flow zone]}]
  (let [visible-flow (flow/adjust flow zone)
        item-processor (partial put-payload visible-flow zone)]
    (->>
      (concat
        ((juxt clear-whole scroll clear-scroll-gap) flow zone)
        (map item-processor (visible-flow :items)))
      (remove nil?))))

