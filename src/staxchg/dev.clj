(ns staxchg.dev
  (:require [clojure.string :as string])
  (:require [staxchg.util :as util])
  (:gen-class))

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
  (when-let [pathname (util/config-hash "LOGFILE")]
    (with-open [writer (clojure.java.io/writer pathname :append true)]
      (.write writer (str (apply str (map truncate items)) "\n")))))

(defmulti log-recipe-step :function)

(defmethod log-recipe-step :staxchg.io/put-markdown!
  [{[_ plot _] :params}]
  (log "[put-markdown] " (->> (map second plot) (take 10) (apply str))
       " |>" (string/join (map (comp #(.getCharacter %) first) plot)) "<|"))

(defmethod log-recipe-step :staxchg.io/put-string!
  [{[_ string {:keys [x y]}] :params}]
  (log "[put-string] " [x y] " |>"  string "<|"))

(defmethod log-recipe-step :staxchg.io/scroll!
  [{[_ top bottom distance] :params}]
  (log "[scroll] at [" top " " bottom "] by " distance))

(defmethod log-recipe-step :staxchg.io/clear!
  [{[graphics left top width height] :params}]
  (log "[clear] rect [" width "x" height "] at [" left "x" top "]"))

(defmethod log-recipe-step :staxchg.io/refresh!
  [_]
  (log "[refresh]"))

(defmethod log-recipe-step :staxchg.io/read-key!
  [_]
  (log "[read-key]"))

(defmethod log-recipe-step :staxchg.io/query!
  [_]
  (log "[query]"))

(defmethod log-recipe-step :staxchg.io/fetch-questions!
  [{[_ url query-params] :params}]
  (log "[fetch-questions] url: " url ", query-params: " query-params))

(defmethod log-recipe-step :staxchg.io/fetch-answers!
  [{[_ url query-params question-id] :params}]
  (log "[fetch-answers] url: " url ", "
       "query-params: " query-params ", "
       "question-id: " question-id))

(defmethod log-recipe-step :staxchg.io/highlight-code!
  [{[code syntax question-id answer-id] :params}]
  (log "[highlight-code] BEGIN syntax: " syntax ", "
       "question-id: " question-id ", "
       "answer-id: " answer-id "\r\n"
       "\\")
  (->> code
       string/trim-newline
       string/split-lines
       (map #(str " |  " %))
       (run! log))
  (log "/" "\r\n"
       "[highlight-code] END"))

(defmethod log-recipe-step :staxchg.io/quit!
  [_]
  (log "[quit]"))

(defmethod log-recipe-step :staxchg.io/register-theme!
  [{[theme-name filename] :params}]
  (log "[register-theme] name: " theme-name ", filename: " filename))

(defmethod log-recipe-step :staxchg.io/acquire-screen!
  [_]
  (log "[acquire-screen]"))

(defmethod log-recipe-step :staxchg.io/enable-screen!
  [_]
  (log "[enable-screen]"))

(defmethod log-recipe-step :default [_])

(defn log-request
  ""
  [{:keys [recipes timing]}]
  (log " /^^^ " (count recipes) " recipe(s)")
  (run! log (map
              #(str "|----- " (string/join ", " (map :function %1))
                    " --- " %2)
              recipes
              (map (comp second #(re-find #"(\d*.\d* msecs)" %)) timing)))
  (log " \\___ Complete")
  (run! log-recipe-step (flatten recipes)))

