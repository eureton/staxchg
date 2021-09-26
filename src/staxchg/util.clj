(ns staxchg.util
  (:require [clojure.java.io :as io])
  (:require [clojure.core.async :as async :refer [<!! >!!]])
  (:require [flatland.useful.fn :as ufn])
  (:gen-class))

(defn read-properties
  "Open source with java.io.Reader and read its contents into a
   java.util.Properties object."
  [source]
  (let [properties (java.util.Properties.)]
    (try
      (with-open [stream (io/reader source)]
        (.load properties stream))
      (catch java.io.FileNotFoundException _ nil))
    properties))

(defn read-resource-properties
  "Read filename from resources/ as a Java properties file."
  [filename]
  (->> filename io/resource read-properties))

(defn properties-hash
  "Hash representation of the file at pathname."
  [pathname]
  (->> pathname
       read-properties
       (reduce (fn [m [k v]] (assoc m k v)) {})))

(defn config-pathname
  "Absolute pathname of where the configuration file is expected to be."
  []
  (str (System/getenv "HOME") "/.staxchg.conf"))

(defn config-hash
  "Hash representation of the contents of the configuration file."
  ([]
   (properties-hash (config-pathname)))
  ([config-key]
   (config-hash config-key nil))
  ([config-key not-found]
   (get (config-hash) config-key not-found)))

(defn unescape-html [string]
  "Shorthand for the Jsoup method to unescape HTML entities within string."
  (org.jsoup.parser.Parser/unescapeEntities string true))

(def shell-output-ok?
  "True if given the output of a clojure.java.shell/sh call which:
     1. is valid
     2. ended successfully
   False otherwise."
  (every-pred some?
              (comp number? :exit)
              (comp zero? :exit)
              (comp string? :out)))

