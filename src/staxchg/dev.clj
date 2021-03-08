(ns staxchg.dev
  (:gen-class))

(def pathname "./dev.log")

(defn truncate
  [x]
  (if-let [_ (string? x)]
    (let [length (count x)
          truncation-limit 128
          context 32]
      (if (> length truncation-limit)
        (str (subs x 0 context) " [...] " (subs x (- length context) length))
        x))
    x))

(defn log
  [& items]
  (with-open [writer (clojure.java.io/writer pathname :append true)]
    (.write writer (str (apply str (map truncate items)) "\n"))))

; sample response for testing purposes
(def response-body
{
  "items" [] ,
  "has_more" true,
  "quota_max" 10000,
  "quota_remaining" 9966
})

