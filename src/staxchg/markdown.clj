(ns staxchg.markdown
  (:require [clojure.core.cache.wrapped :as cache])
  (:require [clojure.string :as string])
  (:require [flatland.useful.fn :as ufn])
  (:require [squirrel.tree :as tree])
  (:require [squirrel.node :as node])
  (:require [cljmd.ast])
  (:require [staxchg.plot :as plot])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defmulti normalize
  "Markdown processing step for achieving a domain-specific canonical form.
   It is implemented as a multimethod and is intended to be called on markdown
   AST nodes. It dispatches on markdown tags."
  (comp :tag :data))

(defmethod normalize :list
  [node]
  (let [indexer #(-> %2
                     (assoc-in [:data :index] (inc %1))
                     (assoc-in [:data :list-size] (node/fanout node)))
        mapper #(map-indexed indexer %)
        ordered? (comp #{"ordered"} :type :data)]
    (ufn/fix node ordered? #(update % :children mapper))))

(defmethod normalize :a
  [node]
  (let [url (node/make {:tag :url
                        :content (->> node :data :destination (str " "))})]
    (update node :children conj url)))

(defmethod normalize :default
  [node]
  node)

(def plot-cache (cache/lru-cache-factory {} :threshold 8))

(def cache-eligibility-limit 3000)

(defn cache-key
  ""
  [string
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (hash [string x y left top width height]))

(defn ast
  ""
  [string]
  (->> string
       cljmd.ast/from-string
       (tree/map normalize)))

(defn no-cache-plot
  "Returns a sequence of pairs -one for each character of the input string-
   consisting of:
     * the character
     * the [x y] coordinates of the character"
  [string options]
  (plot/ast (ast string) options))

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

(defmulti code-info-rf
  (fn [_ {:keys [tag]}] tag)
  :hierarchy plot/ontology)

(defmethod code-info-rf :code-block
  [acc {:keys [content info]}]
  (let [entry (cond-> {:string content}
                (not-empty info) (assoc :syntax info))]
    (conj acc entry)))

(defmethod code-info-rf :default
  [acc _]
  acc)

(defn code-info
  ""
  [string]
  (tree/reduce code-info-rf [] (ast string) :depth-first))

