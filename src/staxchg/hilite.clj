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

(defn classes
  ""
  [node]
  (-> node
      .attributes
      (.get "class")
      keyword
      class-trait-map
      vector
      set))

(defn info
  ""
  [html]
  (->>
    (.select (Jsoup/parse html) "span[class]")
    (map (juxt #(.wholeText %) classes #(.outerHtml %)))
    (map (fn [[text classes html]] {html [classes text]}))
    (reduce merge)
    (reduce-kv (fn [acc k [classes code]]
                 (conj acc {:code code
                            :classes classes
                            :html k}))
               [])))

(defn configure-jsoup-doc
  "Configures the output settings of the org.jsoup.nodes.Document object to work
   well with the app."
  [doc]
  (.outputSettings doc (.prettyPrint (Document$OutputSettings.) false)))

(defn jsoup-elem
  "Expects skylighting shell output in HTML format.
   Returns the JSoup element for the <code class=\"sourceCode\"> tag."
  [sh-out]
  (-> sh-out
      :out
      Jsoup/parse
      configure-jsoup-doc
      (.select "code.sourceCode")
      .first))

(defn normalize-df
  "Dispatch function for hilite/normalize. Returns a keyword."
  [html]
  (let [doc (-> html Jsoup/parse configure-jsoup-doc)]
    (cond
      (-> doc (.select "code.sourceCode") .first some?) :skylighting
      (-> doc (.select "div.highlight") .first some?) :pygments
      :else :highlight.js)))

(defmulti normalize
  "Returns html parsable by hilite/jsoup-elem, regardless of utility of origin."
  normalize-df)

(defmethod normalize :skylighting
  [html]
  html)

(defmethod normalize :pygments
  [html]
  (str "<code class=\"sourceCode\">"
       (-> html
           Jsoup/parse
           configure-jsoup-doc
           (.select "div.highlight pre")
           .first
           .html)
       "</code>"))

(defmethod normalize :highlight.js
  [html]
  (str "<code class=\"sourceCode\">" html "</code>"))

(defn parse
  ""
  [sh-out]
  (let [iron #(string/replace % #"[\r\n]" "")]
    (when (util/shell-output-ok? sh-out)
      (->> (update sh-out :out normalize)
           jsoup-elem
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
                    token-size (count (staxchg.util/unescape-html (:code tag)))
                    annotator #(update-in % [2 :traits] union (:classes tag))]
                (recur
                  (subs html (count (:html tag)))
                  (drop token-size plot)
                  (concat result (->> plot (take token-size) (map annotator)))))
              (recur
                (subs html out-of-tag-esc-html-size)
                (drop out-of-tag-unesc-html-size plot)
                (concat result (take out-of-tag-unesc-html-size plot))))))))))

