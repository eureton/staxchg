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
  (let [mapper #(-> % .attributes (.get "class") keyword class-trait-map)]
    (->> node
         ((juxt inner-hilite-nodes identity))
         (apply conj)
         (map mapper)
         set)))

(defn info
  ""
  [html]
  (->> html
       jsoup-document
       outer-hilite-nodes
       (map (juxt #(.wholeText %) #(.outerHtml %) classes))
       (sort-by (comp - count second))
       (map (fn [[text html classes]] {html [classes text]}))
       (reduce merge)
       (reduce-kv (fn [acc k [classes code]]
                    (conj acc {:code code
                               :classes classes
                               :html k}))
                  [])))

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

(defn parse
  ""
  [sh-out]
  (let [iron #(string/replace % #"[\r\n]" "")]
    (when (util/shell-output-ok? sh-out)
      (->> sh-out
           :out
           normalize
           root-jsoup-elem
           ((juxt identity #(iron (.html %)) #(iron (.wholeText %))))
           (zipmap [:raw :html :text])))))

(defn match?
  "Returns true if the text to which the plot corresponds is entirely contained
   in the code which the highlight is derived from, false otherwise."
  [highlight plot]
  (string/includes? (:text highlight)
                    (string/join (map first plot))))

(defn index-of
  "Returns the index at which the text to which the plot corresponds is found in
   in the code which the highlight is derived from.
   If it is not found, returns nil."
  [highlight plot]
  (string/index-of (:text highlight)
                   (string/join (map first plot))))

(defn annotate
  ""
  [plot html]
  (if (nil? html)
    plot
    (let [info (info html)]
      (loop [html html
             plot plot
             result []]
        (if (some empty? [plot html])
          result
          (let [out-of-tag-html (->> html
                                     (re-find #"^(.*?)(?:<span class=\"|$)")
                                     second)
                out-of-tag-esc-html-size (count out-of-tag-html)
                out-of-tag-unesc-html-size (count (util/unescape-html out-of-tag-html))
                html-starts-with? #(string/starts-with? html %)]
            (if (zero? out-of-tag-esc-html-size)
              (let [tag (first (filter (comp html-starts-with? :html) info))
                    token-size (count (:code tag))
                    annotator #(update-in % [2 :traits] union (:classes tag))]
                (recur
                  (subs html (count (:html tag)))
                  (drop token-size plot)
                  (concat result (->> plot (take token-size) (map annotator)))))
              (recur
                (subs html out-of-tag-esc-html-size)
                (drop out-of-tag-unesc-html-size plot)
                (concat result (take out-of-tag-unesc-html-size plot))))))))))

