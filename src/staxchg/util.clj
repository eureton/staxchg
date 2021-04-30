(ns staxchg.util
  (:require [clojure.java.io :as io])
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:gen-class))

(defn read-properties
  ""
  [source]
  (let [properties (java.util.Properties.)]
    (try
      (with-open [stream (io/reader source)]
        (.load properties stream))
      (catch java.io.FileNotFoundException _ nil))
    properties))

(defn read-resource-properties
  ""
  [filename]
  (->> filename io/resource read-properties))

(defn properties-hash
  ""
  [pathname]
  (reduce
    (fn [m [k v]] (assoc m k v))
    {}
    (read-properties pathname)))

(defn config-pathname
  ""
  []
  (str (System/getenv "HOME") "/.staxchg.conf"))

(defn config-hash
  ""
  ([]
   (properties-hash (config-pathname)))
  ([config-key]
   (config-hash config-key nil))
  ([config-key not-found]
   (get (config-hash) config-key not-found)))

(defmacro timed-eval
  [body]
  `(let [channel# (async/chan 1)
         timing# (with-out-str (>!! channel# (time (~@body))))]
     {:value (<!! channel#)
      :timing timing#}))

