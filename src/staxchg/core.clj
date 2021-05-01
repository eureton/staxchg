(ns staxchg.core
  (:require [staxchg.api :as api])
  (:require [staxchg.io :as io])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (io/run-input-loop (-> dev/response-body
                         (get "items")
                         ((partial mapv api/scrub)))))

