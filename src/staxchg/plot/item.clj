(ns staxchg.plot.item
  (:require [clojure.set])
  (:gen-class))

(defn decorate
  "Applies traits to item."
  [item traits]
  (update-in item [2 :traits] clojure.set/union (set traits)))

(defn strip
  "Removes traits from item."
  [item traits]
  (update-in item [2 :traits] clojure.set/difference (set traits)))

