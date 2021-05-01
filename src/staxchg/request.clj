(ns staxchg.request
  (:require [staxchg.recipe :as recipe])
  (:require [staxchg.state.recipe :as state.recipe])
  (:require [staxchg.dev :as dev])
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:gen-class))

(defn make
  ""
  [world]
  {:recipes (state.recipe/all world)
   :context (:io/context world)})

(defn inflate
  ""
  [{:keys [context recipes]}]
  (map recipe/inflate recipes (repeat context)))

(defn log
  ""
  [{:as request
    :keys [recipes]}]
  (dev/log " /^^^ Routing " (count recipes) " recipe(s)")
  (doseq [[i r] (map-indexed vector recipes)]
    (apply dev/log "|----- recipe #" i ": " (interpose ", " (map :function r))))
  (dev/log " \\___ Complete")
  request)

(defn route
  [{:keys [from to]}]
  (let [results (->> (<!! from)
                     log
                     inflate
                     (map (comp recipe/commit recipe/bind-symbols))
                     flatten
                     doall)]
    (cond->> results
      (some? to) (>!! to))))

