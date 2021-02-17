(ns staxchg.markdown
  (:require [clojure.set])
  (:require [staxchg.flexmark :as flexmark])
  (:require [staxchg.ast :as ast])
  (:require [staxchg.plot :as plot])
  (:gen-class))

(defmulti annotate
  (fn [node _] (node :tag))
  :hierarchy plot/ontology)

(defmethod annotate :olist
  [node _]
  (update node :children #(map-indexed
                            (fn [i item]
                              (assoc item :index (inc i) :list-size (count %)))
                            %)))

(defmethod annotate :link
  [node _]
  (let [spacing {:tag :txt :content " "}
        url {:tag :url :content (node :url)}]
    (update node :children conj spacing url)))

(defmethod annotate :link-ref
  [node _]
  (->
    node
    (assoc :content (str "[" (node :ref) "]"))
    (dissoc :children)))

(defmethod annotate :default
  [node _]
  node)

(defn decorate [recipient traits & clauses]
  (let [effect-map (apply
                     zipmap
                     (map
                       (fn [f]
                         (map
                           second
                           (filter #(->> % first f) (map-indexed vector clauses))))
                       [even? odd?]))]
    (reduce
      (fn [aggregator trait] ((effect-map trait) aggregator))
      recipient
      (clojure.set/intersection (set (keys effect-map)) traits))))

(defn plot
  "Returns a sequence of pairs -one for each character of the input string-
   consisting of:
     * the character
     * the [x y] coordinates of the character"
  [string options]
  (->
    string
    flexmark/parse
    (ast/depth-first-walk annotate)
    (plot/ast options)))

(defn line-count
  [string width]
  (->>
    (plot string {:width width})
    ((juxt last first))
    (map second)
    (map second)
    (reduce -)
    inc))
