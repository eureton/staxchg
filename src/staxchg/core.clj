(ns staxchg.core
  (:require [smachine.core :as smachine])
  (:require [staxchg.io :as io])
  (:require [staxchg.state :as state])
  (:require [staxchg.state.recipe :as state.recipe])
  (:gen-class))

(defn -main
  "Application entry point."
  [& args]
  (smachine/run {:init (state/make)
                 :req-fn state.recipe/request
                 :trans-fn state/update-world
                 :term-fn state/quit?}))

