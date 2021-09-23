(ns staxchg.core
  (:require [clojure.core.async :as async :refer [thread]])
  (:require [smachine.core :as smachine])
  (:require [cookbook.core :as cookbook])
  (:require [staxchg.api :as api])
  (:require [staxchg.io :as io])
  (:require [staxchg.state :as state])
  (:require [staxchg.state.recipe :as state.recipe])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn run-request-loop
  "Wires up the given core.async channels with cookbook."
  [in-channel out-channel]
  (loop []
    (cookbook/route {:from in-channel :to out-channel :log-fn dev/log})
    (recur)))

(defn -main
  "Application entry point."
  [& args]
  (thread (run-request-loop io/request-channel io/response-channel))
  (smachine/run {:s-init (state/make)
                 :req-ch io/request-channel
                 :resp-ch io/response-channel
                 :req-fn state.recipe/request
                 :trans-fn state/update-world
                 :term-fn nil?}))

