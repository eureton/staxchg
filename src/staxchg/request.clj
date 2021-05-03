(ns staxchg.request
  (:require [staxchg.recipe :as recipe])
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:gen-class))

(defn inflate
  ""
  [{:keys [context recipes]}]
  (map recipe/inflate recipes (repeat context)))

(defn route
  [{:keys [from to log-fn]}]
  (let [request (<!! from)
        results (->> request
                     inflate
                     (map (comp recipe/commit recipe/bind-symbols))
                     flatten
                     doall)]
    (when (some? log-fn) (log-fn request))
    (cond->> results
      (some? to) (>!! to))))

