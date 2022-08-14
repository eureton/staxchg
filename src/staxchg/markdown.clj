(ns staxchg.markdown
  (:require [clojure.core.cache.wrapped :as cache])
  (:require [clojure.string :as string])
  (:require [clojure.tools.logging :as log])
  (:require [flatland.useful.fn :as ufn])
  (:require [squirrel.tree :as tree])
  (:require [squirrel.node :as node])
  (:require [cljmd.ast])
  (:require [staxchg.plot :as plot])
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

(def plot-cache
  "Instance of clojure.core.cache.wrapped for caching hard-won plots."
  (cache/lru-cache-factory {} :threshold 8))

(def cache-eligibility-limit
  "Minimum number of characters for eligibility to be cached."
  300)

(defn cache-key
  "Collapses all plotting parameter which distinguish one plot from another
   into a single value."
  [string
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (hash [string x y left top width height]))

(defn ast
  "Transforms CommonMark contained in string to an AST suitable for use by the
   application."
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
  "Implementation detail. Do not call directly."
  [string options]
  (let [digest (cache-key string options)
        preview (subs string 0 (min (count string) 32))]
    (log/debug "[through-cache-plot] \"" preview "\" " options
               " in cache? " (cache/has? plot-cache digest))
    (cache/lookup-or-miss
      plot-cache
      digest
      (fn [_]
        (log/debug "[through-cache-plot] cache miss for \"" preview "\""
                   ", calculating...")
        (no-cache-plot string options)))))

(defn plot
  "Plots string with the given options. See staxchg.plot/ast for more details on
   the latter. Caches generated plots. Checks the cache before attempting to
   generate a plot."
  [string options]
  ((if (>= (count string) cache-eligibility-limit)
     through-cache-plot
     no-cache-plot) string options))

(defn line-count
  "Number of full lines the Markdown contained in string spans. Assumes lines
   are width columns wide."
  [string width]
  (->>
    (plot string {:width width})
    ((juxt last first))
    (map second)
    (map second)
    (reduce -)
    inc))

(defmulti code-info-rf
  "Reducer for staxchg.markdown/code-info"
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
  "Vector containing one hash for each code block Markdown element within
   string. Each hash contains the contents of the code block under :string. In
   the case of fenced code blocks, the info string -when available- is added
   under :info."
  [string]
  (tree/reduce code-info-rf [] (ast string) :depth-first))

