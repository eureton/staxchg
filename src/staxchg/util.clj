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

(defn unescape-html [string]
  (org.jsoup.parser.Parser/unescapeEntities string true))

(def shell-output-ok?
  "Returns true if given the output of a clojure.java.shell/sh call which:
     1. is valid
     2. ended successfully
   False otherwise."
  (every-pred some?
              (comp number? :exit)
              (comp zero? :exit)
              (comp string? :out)))

