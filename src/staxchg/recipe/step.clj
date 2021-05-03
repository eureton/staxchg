(ns staxchg.recipe.step
  (:gen-class))

(defn inflate-param
  ""
  [context param]
  (cond
    (keyword? param) (param context)
    (fn? param) (param (context :screen))
    :else param))

(defn inflate
  ""
  [step context]
  (update step :params (partial map inflate-param (repeat context))))

(defn resolve-function
  ""
  [fn-key]
  (->> fn-key symbol find-var var-get))

(defn bind-symbol
  [step]
  (update step :function resolve-function))

(defn commit
  ""
  [{:keys [function params]}]
  (apply function params))

