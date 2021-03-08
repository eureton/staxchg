(ns staxchg.util
  (:gen-class))

(defn read-properties
  ""
  [pathname]
  (let [properties (java.util.Properties.)]
    (with-open [stream (clojure.java.io/reader pathname)]
      (.load properties stream))
    properties))

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
   (get (config-hash) config-key)))

