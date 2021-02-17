(ns staxchg.ast
  (:require [clojure.pprint :as pprint])
  (:gen-class))

(defn zero
  ""
  [tag attributes]
  (merge {:tag tag :children []} attributes))

(defn add
  ""
  [acc x]
  (update acc :children conj x))

(defn leaf
  ""
  [tag content attributes]
  (merge {:tag tag :content content} attributes))

(defn leaf?
  ""
  [node]
  (-> node :children nil?))

(defn walk-inner
  ""
  [ast f outerf {:keys [trail]}]
  (if (leaf? ast)
    ast
    (loop [nodes (:children ast)
           result (assoc ast :children [])
           trail (conj trail 0)]
      (if (empty? nodes)
        result
        (let [head (first nodes)
              walked-head (outerf head f {:trail trail :leaf? (leaf? head)})]
          (recur
            (rest nodes)
            (update result :children conj walked-head)
            (update trail (dec (count trail)) inc)))))))

(defn depth-first-walk
  ""
  ([ast f]
   (depth-first-walk ast f {:trail [] :leaf? false}))
  ([ast f {:keys [trail leaf?]}]
   (->
     (walk-inner ast f depth-first-walk {:trail trail})
     (f {:trail trail :leaf? leaf?}))))

(defn breadth-first-walk
  ""
  ([ast f]
   (breadth-first-walk ast f {:trail [] :leaf? false}))
  ([ast f {:keys [trail leaf?]}]
   (let [walked (f ast {:trail trail :leaf? leaf?})]
     (walk-inner ast f breadth-first-walk {:trail trail})
     walked)))

(defn walk
  ""
  ([ast f]
   (walk :breadth-first ast f))
  ([strategy ast f]
   ((case strategy
      :depth-first depth-first-walk
      :breadth-first breadth-first-walk) ast f)))

(defn dump
  ""
  ([ast]
   (do (walk :breadth-first ast dump)
       nil))
  ([node {:keys [trail leaf?]}]
   (let [level (count trail)
         prefix (if leaf? "-|" "->")
         indent (clojure.string/join (repeat (* level 2) \space))]
     (->>
       (cond-> [prefix indent (:tag node)]
         leaf? (conj (:content node)))
       (apply println))
     node)))

