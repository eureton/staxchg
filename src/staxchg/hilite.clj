(ns staxchg.hilite
  (:require [staxchg.dev :as dev])
  (:require [clojure.pprint])
  (:require [clojure.string :as string])
  (:require [clojure.set :refer [union]])
  (:require [cheshire.core])
  (:import (org.jsoup Jsoup))
  (:import (org.jsoup.parser Parser))
  (:gen-class))

(def skylighting-class-trait-map {:kw :hilite-keyword
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
                                  :er :hilite-error})

(defn classes
  ""
  [node]
  (-> node
      .attributes
      (.get "class")
      keyword
      skylighting-class-trait-map
      vector
      set))

(defn info
  ""
  [html]
  (->>
    (.select (Jsoup/parse html) "span[class]")
    (map (juxt #(.wholeText %) classes #(.outerHtml %)))
    (map (fn [[text classes html]] {text [classes html]}))
    (reduce (partial merge-with (fn [[classes1 html1] [classes2 html2]]
                                  [(union classes1 classes2)
                                   (first (sort-by (comp - count) [html1 html2]))])))
    (reduce-kv (fn [acc k v]
                 (conj acc {:code k
                            :classes (v 0)
                            :html (Parser/unescapeEntities (v 1) true)}))
               [])))

(defn parse
  ""
  [sh-out]
  (let [ok? (every-pred some?
                        (comp number? :exit)
                        (comp zero? :exit)
                        (comp string? :out))]
    (when (ok? sh-out))
      (-> sh-out
          :out
          (string/replace #"[\r\n]"  "")
          Jsoup/parse
          (.select "code.sourceCode")
          .html
          (Parser/unescapeEntities true))))

(defn annotate
  ""
  [plot html]
  (if (nil? html)
    plot
    (let [info (info html)]
      (loop [html html
             plot plot
             result []]
        (if (empty? html)
          result
          (let [normal-count (->> html
                                  (re-find #"^(.*?)(?:<span class=\"|$)")
                                  second
                                  count)
                html-starts-with? #(string/starts-with? html %)]
            (if (zero? normal-count)
              (let [tag (first (filter (comp html-starts-with? :html) info))
                    token-size (count (:code tag))
                    annotator #(update-in % [2 :traits] union (:classes tag))]
                (recur
                  (subs html (count (:html tag)))
                  (drop token-size plot)
                  (concat result (->> plot (take token-size) (map annotator)))))
              (recur
                (subs html normal-count)
                (drop normal-count plot)
                (concat result (take normal-count plot))))))))))

