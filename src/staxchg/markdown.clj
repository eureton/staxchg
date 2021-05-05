(ns staxchg.markdown
  (:require [clojure.core.cache.wrapped :as cache])
  (:require [clojure.set])
  (:require [staxchg.flexmark :as flexmark])
  (:require [staxchg.ast :as ast])
  (:require [staxchg.plot :as plot])
  (:require [staxchg.dev :as dev])
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
  (let [url {:tag :url :content (str " " (node :url))}]
    (update node :children conj url)))

(defmethod annotate :link-ref
  [node _]
  (->
    node
    (assoc :content (str "[" (node :ref) "]"))
    (dissoc :children)))

(defmethod annotate :html-comment-block
  [node _]
  (update node :content #(->> %
                              (re-find #"<!-- (.*) -->")
                              second)))

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

(def plot-cache (cache/lru-cache-factory {} :threshold 8))

(def cache-eligibility-limit 3000)

(defn cache-key
  ""
  [string
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (hash [string x y left top width height]))

(defn no-cache-plot
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

(defn through-cache-plot
  ""
  [string options]
  (let [digest (cache-key string options)
        preview (subs string 0 (min (count string) 32))]
    (dev/log "[through-cache-plot] \"" preview "\" " options
             " in cache? " (cache/has? plot-cache digest))
    (cache/lookup-or-miss
      plot-cache
      digest
      (fn [_]
        (dev/log "[through-cache-plot] cache miss for \"" preview "\""
                 ", calculating...")
        (no-cache-plot string options)))))

(defn plot
  ""
  [string options]
  ((if (>= (count string) cache-eligibility-limit)
     through-cache-plot
     no-cache-plot) string options))

(defn line-count
  [string width]
  (->>
    (plot string {:width width})
    ((juxt last first))
    (map second)
    (map second)
    (reduce -)
    inc))

(defmulti code-content :tag)

(defmethod code-content :indented-code-block
  [node]
  (->> (:content node)
       clojure.string/split-lines
       (map-indexed #(subs %2 (if (zero? %1) 0 4)))
       clojure.string/join))

(defmethod code-content :fenced-code-block
  [node]
  (->> (:children node)
       (filter (comp #{:txt} :tag))
       first
       :content))

(defmethod code-content :default
  [_]
  nil)

(defn code-snippets
  ""
  [string]
  (let [tree (flexmark/parse string)
        scrape-text #(->> % (filter (comp #{:txt} :tag)) first :content)
        f (fn [acc {:keys [tag content children info] :as node}]
            (let [text (code-content node)]
              (cond-> acc
                (= :indented-code-block tag) (conj {:string text})
                (= :fenced-code-block tag)   (conj {:string text :lang info}))))]
    (ast/reduce-df f [] tree)))

