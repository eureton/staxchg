(ns staxchg.recipe
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.flow.item :as flow.item])
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
    {:function :staxchg.ui/clear!
     :params [(sub-graphics zone)]}))

(defn scroll
  ""
  [flow
   {:as zone
    :keys [top height]}]
  (when (flow/scrolled? flow)
    {:function :staxchg.ui/scroll!
     :params [:screen
              top
              (+ top (dec height))
              (flow :scroll-delta)]}))

(defn clear-scroll-gap
  ""
  [flow zone]
  (when (flow/scrolled? flow)
    {:function :staxchg.ui/clear!
     :params [(sub-graphics (flow/scroll-gap-rect flow zone))]}))

(defn put-payload
  ""
  [flow zone item]
  {:function (case (item :type)
               :markdown :staxchg.ui/put-markdown!
               :string :staxchg.ui/put-string!)
   :params [(fx-graphics (flow/scroll-gap-rect flow zone) item)
            (flow.item/payload item)
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

(defn resolve-function
  ""
  [fn-key]
  (->> fn-key symbol find-var var-get))

(defn bind-symbols
  [recipe]
  (map #(update % :function resolve-function) recipe))

(defn commit-step
  ""
  [{:keys [function params]}]
  (apply function params))

(defn commit
  [[results recipe]]
  (->>
    (cond->> (doall (map commit-step recipe))
      (some? results) (>!! results))
    time
    with-out-str
    clojure.string/trim-newline
    (dev/log "[commit] ")))

(defn log
  ""
  [recipes]
  (dev/log " /^^^ Routing " (count recipes) " recipe(s)")
  (doseq [[i r] (map-indexed vector recipes)]
    (apply dev/log "|----- recipe #" i ": " (interpose ", " (map :function r))))
  (dev/log " \\___ Complete")
  recipes)

(defn route
  [{:keys [from to screen]
    :or {to nil}}]
  (let [prepare (comp bind-symbols
                      (partial inflate screen))]
    (->>
      (<!! from)
      log
      (map prepare)
      (map vector (repeat to))
      (run! commit))))

