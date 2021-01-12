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

(defn clear-questions-pane-body
  ""
  [flow]
  (when-let [rect (flow :force-clear)]
    {:function :clear!
     :params [(sub-graphics rect)]}))

(defn scroll
  ""
  [flow]
  (when (flow/scrolled? flow)
    (let [{:keys [top height]} (flow/bounding-rect flow)]
      {:function :scroll!
       :params [:screen
                top
                (+ top (dec height))
                (flow :scroll-delta)]})))

(defn clear-footprint
  ""
  [flow]
  (when (flow/scrolled? flow)
    {:function :clear!
     :params [(sub-graphics (flow/scroll-gap-rect flow))]}))

(defn put-payload
  ""
  [flow item]
  {:function (case (item :type)
               :markdown :put-markdown!
               :string :put-string!)
   :params [(fx-graphics (flow/scroll-gap-rect flow) item)
            (flow/payload item)
            (select-keys item [:x :y])]})

(defn make
  ""
  [flow]
  (let [visible-flow (flow/visible-subset flow)
        item-processor (partial put-payload visible-flow)]
    (->>
      (concat
        ((juxt clear-questions-pane-body scroll clear-footprint) flow)
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

