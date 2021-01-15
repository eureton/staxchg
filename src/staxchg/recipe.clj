(ns staxchg.recipe
  (:require [staxchg.flow :as flow])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.dev :as dev])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:gen-class))

(defn sub-graphics
  ""
  [{:keys [left top width height]}]
  #(->
     %
     .newTextGraphics
     (.newTextGraphics
       (TerminalPosition. left top)
       (TerminalSize. width height))))

(defn fx-graphics
  ""
  [dimensions
   {:keys [foreground-color background-color modifiers]}]
  #(->
     ((sub-graphics dimensions) %)
     (.enableModifiers (into-array SGR modifiers))
     (.setForegroundColor foreground-color)
     (.setBackgroundColor background-color)))

(defn clear-whole
  ""
  [flow zone]
  (when (zone :clear?)
    {:function :clear!
     :params [(sub-graphics zone)]}))

(defn scroll
  ""
  [flow
   {:as zone
    :keys [top height]}]
  (when (flow/scrolled? flow)
    {:function :scroll!
     :params [:screen
              top
              (+ top (dec height))
              (flow :scroll-delta)]}))

(defn clear-scroll-gap
  ""
  [flow zone]
  (when (flow/scrolled? flow)
    {:function :clear!
     :params [(sub-graphics (flow/scroll-gap-rect flow zone))]}))

(defn put-payload
  ""
  [flow zone item]
  {:function (case (item :type)
               :markdown :put-markdown!
               :string :put-string!)
   :params [(fx-graphics (flow/scroll-gap-rect flow zone) item)
            (flow/payload item)
            (select-keys item [:x :y])]})

(defn make
  ""
  [{:keys [flow zone]}]
  (let [visible-flow (flow/clip flow zone)
        item-processor (partial put-payload visible-flow zone)]
    (->>
      (concat
        ((juxt clear-whole scroll clear-scroll-gap) flow zone)
        (map item-processor (visible-flow :items)))
      (remove nil?))))

(defn inflate
  ""
  [screen recipe]
  (let [param-mapper #(cond
                        (= :screen %) screen
                        (fn? %) (% screen)
                        :else %)
        step-mapper #(->>
                       %
                       :params
                       (map param-mapper)
                       (assoc % :params))]
    (map step-mapper recipe)))

