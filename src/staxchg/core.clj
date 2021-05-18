(ns staxchg.core
  (:require [clojure.core.async :as async :refer [thread]])
  (:require [smachine.core :as smachine])
  (:require [staxchg.api :as api])
  (:require [staxchg.io :as io])
  (:require [staxchg.state :as state])
  (:require [staxchg.state.recipe :as state.recipe])
  (:require [staxchg.request :as request])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn run-request-loop
  ""
  [in-channel out-channel]
  (loop []
    (request/route {:from in-channel :to out-channel :log-fn dev/log})
    (recur)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (thread (run-request-loop io/request-channel io/response-channel))
  (smachine/run {:s-init (state/make)
                 :req-ch io/request-channel
                 :resp-ch io/response-channel
                 :req-fn state.recipe/request
                 :trans-fn state/update-world
                 :term-fn nil?}))

