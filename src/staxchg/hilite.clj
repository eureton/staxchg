(ns staxchg.hilite
  (:require [staxchg.util :as util])
  (:require [staxchg.dev :as dev])
  (:require [clojure.pprint])
  (:require [clojure.string :as string])
  (:require [clojure.set :refer [union]])
  (:require [cheshire.core])
  (:import (org.jsoup Jsoup))
  (:import org.jsoup.nodes.Document$OutputSettings)
  (:gen-class))

(def tools
  "Set containing keyword IDs of all supported syntax highlighting tools."
  #{:skylighting :highlight.js :pygments})

(def class-trait-map {; skylighting
                      :kw :hilite-keyword
                      :dt :hilite-data-type
                      :dv :hilite-dec-val
                      :bn :hilite-base-n
                      :fl :hilite-float
                      :cn :hilite-constant
                      :ch :hilite-char
                      :sc :hilite-special-char
                      :st :hilite-string
                      :vs :hilite-verbatim-string
                      :ss :hilite-special-string
                      :im :hilite-import
                      :co :hilite-comment
                      :do :hilite-documentation
                      :an :hilite-annotation
                      :cv :hilite-comment-var
                      :ot :hilite-other
                      :fu :hilite-function
                      :va :hilite-variable
                      :cf :hilite-control-flow
                      :op :hilite-operator
                      :bu :hilite-built-in
                      :ex :hilite-extension
                      :pp :hilite-preprocessor
                      :at :hilite-attribute
                      :re :hilite-region-marker
                      :in :hilite-information
                      :wa :hilite-warning
                      :al :hilite-alert
                      :er :hilite-error
                      ; highlight.js
                      :hljs-keyword :hilite-keyword
                      :hljs-class :hilite-keyword
                      :hljs-type :hilite-data-type
                      :hljs-number :hilite-dec-val
                      :hljs-name :hilite-title
                      :hljs-title :hilite-title
                      :hljs-params :hilite-params
                      :hljs-symbol :hilite-constant
                      :hljs-regexp :hilite-constant
                      :hljs-literal :hilite-constant
                      :hljs-string :hilite-string
                      :hljs-meta-keyword :hilite-import
                      :hljs-comment :hilite-comment
                      :hljs-doctag :hilite-documentation
                      :hljs-function :hilite-function
                      :hljs-variable :hilite-variable
                      :hljs-operator :hilite-operator
                      :hljs-built_in :hilite-built-in
                      :hljs-builtin-name :hilite-built-in
                      :hljs-meta :hilite-preprocessor
                      :hljs-meta-string :hilite-preprocessor
                      :hljs-attr :hilite-attribute
                      :hljs-attribute :hilite-attribute
                      :hljs-sub-attribute :hilite-attribute
                      :hljs-section :hilite-region-marker
                      :hljs-tag :hilite-tag
                      :hljs-selector-tag :hilite-css-selector-tag
                      :hljs-selector-id :hilite-css-selector-id
                      :hljs-selector-class :hilite-css-selector-class
                      :hljs-selector-attr :hilite-css-selector-attr
                      :hljs-selector-pseudo :hilite-css-selector-pseudo})

(defn jsoup-document
  "Returns the org.jsoup.nodes.Document which holds the given HTML, properly
   configured to work well with the app."
  [html]
  (-> html
      Jsoup/parse
      (.outputSettings (.prettyPrint (Document$OutputSettings.) false))))

(defn outer-hilite-nodes
  "Returns Jsoup nodes which denote a syntax highlight. In case of multiple
   nested nodes, only the outermost are returned."
  [doc]
  (.select doc "span[class]:not(span[class] > span[class])"))

(defn inner-hilite-nodes
  "Returns Jsoup nodes within node which denote a syntax highlight."
  [node]
  (loop [node node
         result []]
    (if-some [subnode (-> node
                          .html
                          jsoup-document
                          outer-hilite-nodes
                          first)]
      (recur subnode (conj result subnode))
      result)))

(defn classes
  "Returns a set of all detected syntax highlight classes for both node and its
   descendants."
  [node]
  (let [mapper #(-> % .attributes (.get "class") (string/split #" "))]
    (->> node
         ((juxt inner-hilite-nodes identity))
         (apply conj)
         (mapcat mapper)
         (map (comp class-trait-map keyword))
         (remove nil?)
         set)))

(defn root-jsoup-elem
  "Expects skylighting shell output in HTML format.
   Returns the JSoup element for the root tag."
  [html]
  (-> html
      jsoup-document
      (.select "code.sourceCode")
      .first))

(defn normalize-df
  "Dispatch function for hilite/normalize. Returns a keyword."
  [html]
  (let [doc (jsoup-document html)]
    (cond
      (-> doc (.select "code.sourceCode") .first some?) :skylighting
      (-> doc (.select "div.highlight") .first some?) :pygments
      :else :highlight.js)))

(defn wrap-in-code-tag
  "Wrap the HTML in the standard <code> tag."
  [html]
  (format "<code class=\"sourceCode\">%s</code>" html))

(defmulti normalize
  "Returns html parsable by hilite/root-jsoup-elem, regardless of origin."
  normalize-df)

(defmethod normalize :skylighting
  [html]
  (let [wrapped-lines (-> html root-jsoup-elem (.select "a.sourceLine"))]
    (if (empty? wrapped-lines)
      html
      (->> wrapped-lines
           (map #(.html %))
           (string/join "\n")
           wrap-in-code-tag))))

(defmethod normalize :pygments
  [html]
  (-> html
      jsoup-document
      (.select "div.highlight pre")
      .first
      .html
      wrap-in-code-tag))

(defmethod normalize :highlight.js
  [html]
  (wrap-in-code-tag html))

(defn demarcate
  ""
  [node]
  (let [node-text (.wholeText node)]
    (reduce (fn [acc x]
              (let [text (.wholeText x)
                    start (string/index-of node-text
                                           text
                                           (-> acc peek :end (or 0)))]
                (conj acc {:start start
                           :end (+ start (count text))
                           :classes (classes x)})))
            []
            (outer-hilite-nodes node))))

(defn traits-seq
  ""
  [html]
  (let [root (jsoup-document html)
        tags (demarcate root)]
    (map-indexed (fn [index _]
                   (->> tags
                        (filter #(and (<= (:start %) index)
                                      (< index (:end %))))
                        first
                        :classes
                        set))
                 (string/trim-newline (.wholeText root)))))

(defn parse
  ""
  [sh-out]
  (let [iron #(string/replace % #"[\r\n]" "")
        html #(iron (.html %))]
    (when (util/shell-output-ok? sh-out)
      (->> sh-out
           :out
           normalize
           root-jsoup-elem
           ((juxt identity html (comp traits-seq html) #(iron (.wholeText %))))
           (zipmap [:raw :html :traits :text])))))

(defn annotate
  ""
  [plot {:keys [traits] code-text :text}]
  (let [plot-text (->> plot (map first) string/join)
        code-offset (string/index-of code-text plot-text)
        plot-offset (string/index-of plot-text code-text)]
    (if (every? nil? [code-offset plot-offset])
      plot
      (let [[code-offset plot-offset] (map #(or % 0) [code-offset plot-offset])
            annotated (map #(update-in %1 [2 :traits] clojure.set/union %2)
                           (drop plot-offset plot)
                           (drop code-offset traits))]
        (concat (take plot-offset plot)
                annotated
                (drop (+ plot-offset (count annotated)) plot))))))

