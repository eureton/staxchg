(ns staxchg.io.config
  (:require [clojure.java.io :as io])
  (:gen-class))

(def ^:private config-pathname
  "Absolute pathname of configuration file."
  (str (System/getenv "HOME") "/.staxchg.conf"))

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

(defn- properties-hash
  "Hash representation of the Java properties file at pathname."
  [pathname]
  (->> pathname
       read-properties
       (reduce (fn [m [k v]] (assoc m k v)) {})))

(defn fetch
  "Hash representation of the contents of the configuration file. If provided,
   returns the value under config-key instead. If no value is under that key,
   returns not-found instead."
  ([]
   (properties-hash config-pathname))
  ([config-key]
   (fetch config-key nil))
  ([config-key not-found]
   (get (fetch) config-key not-found)))

