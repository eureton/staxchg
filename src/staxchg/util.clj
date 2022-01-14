(ns staxchg.util
  (:gen-class))

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

