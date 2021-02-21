(ns staxchg.core
  (:require [staxchg.api :as api])
  (:require [staxchg.ui :as ui])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn initialize
  ""
  []
  (ui/register-theme! "staxchg" "resources/lanterna-theme.properties"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (initialize)
  (ui/run-input-loop (-> dev/response-body
                         api/parse-response
                         (get "items")
                         ((partial mapv api/scrub-question)))))

