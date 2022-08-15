(ns staxchg.core
  (:require [smachine.core :as smachine]
            [staxchg.dev]
            [staxchg.io :as io]
            [staxchg.state :as state]
            [staxchg.state.recipe :as state.recipe])
  (:gen-class))

(defn -main
  "Application entry point."
  [& args]
  (smachine/run {:init (state/make)
                 :req-fn state.recipe/request
                 :trans-fn state/update-world
                 :term-fn state/shutdown?}))

