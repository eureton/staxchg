(ns staxchg.dev
  (:gen-class))

(def pathname "./dev.log")

(defn log
  [& items]
  (with-open [writer (clojure.java.io/writer pathname :append true)]
    (.write writer (str (apply str items) "\n"))))

