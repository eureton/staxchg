(ns staxchg.markdown
  (:require [clojure.string :as string])
  (:import (com.vladsch.flexmark.parser Parser))
  (:gen-class))

(defn tag
  ""
  [node]
  (case (->> node type str)
    "class com.vladsch.flexmark.util.ast.Document" :doc
    "class com.vladsch.flexmark.ast.AutoLink" :auto-link
    "class com.vladsch.flexmark.ast.BlockQuote" :block-quot
    "class com.vladsch.flexmark.ast.BulletList" :blist
    "class com.vladsch.flexmark.ast.BulletListItem" :blitem
    "class com.vladsch.flexmark.ast.Code" :code
    "class com.vladsch.flexmark.ast.Delimit" :delim
    "class com.vladsch.flexmark.ast.Emphasis" :em
    "class com.vladsch.flexmark.ast.FencedCodeBlock" :fenced-code-block
    "class com.vladsch.flexmark.ast.HardLineBreak" :hbr
    "class com.vladsch.flexmark.ast.Heading" :h
    "class com.vladsch.flexmark.ast.HtmlBlock" :html-block
    "class com.vladsch.flexmark.ast.HtmlCommentBlock" :html-comment-block
    "class com.vladsch.flexmark.ast.HtmlEntity" :html-entity
    "class com.vladsch.flexmark.ast.HtmlInline" :html-inline
    "class com.vladsch.flexmark.ast.HtmlInlineComment" :html-inline-comment
    "class com.vladsch.flexmark.ast.HtmlInnerBlockComment" :html-inner-block-comment
    "class com.vladsch.flexmark.ast.Image" :img
    "class com.vladsch.flexmark.ast.ImageRef" :img-ref
    "class com.vladsch.flexmark.ast.IndentedCodeBlock" :indented-code-block
    "class com.vladsch.flexmark.ast.Link" :link
    "class com.vladsch.flexmark.ast.LinkRef" :link-ref
    "class com.vladsch.flexmark.ast.MailLink" :mail-link
    "class com.vladsch.flexmark.ast.OrderedList" :olist
    "class com.vladsch.flexmark.ast.OrderedListItem" :olitem
    "class com.vladsch.flexmark.ast.Paragraph" :p
    "class com.vladsch.flexmark.ast.Reference" :ref
    "class com.vladsch.flexmark.ast.ThematicBreak" :tbr
    "class com.vladsch.flexmark.ast.SoftLineBreak" :sbr
    "class com.vladsch.flexmark.ast.StrongEmphasis" :strong
    "class com.vladsch.flexmark.ast.Text" :txt))

(defn unpack
  ""
  [node]
  (if (.hasChildren node)
    (loop [iterator (.getChildIterator node)
           result [(tag node)]]
      (if (.hasNext iterator)
        (recur iterator (conj result (unpack (.next iterator))))
        result))
    [(tag node) (-> node .getChars .unescape)]))

(defn parse
  ""
  [string]
  (let [parser (-> (Parser/builder) .build)
        root (.parse parser string)]
    (unpack root)))

(def sample (->> ["- item one"
                  "- item two"
                  "- item three"
                  "    - item three (a)"
                  "    - item three (b)"
                  "where to?\r\n"
                  "# Heading BIG and **STRONG** #\r\n"
                  "Display *emphasized* stuff here!\r\n"
                  "``` clojure"
                  "(defn dbl [x]"
                  "  (* x 2))"
                  "```\r\n"
                  "    (defn sqr [x]"
                  "      (* x x))\r\n"
                  "Goodbye and `happy coding`!"]
                 (string/join "\r\n")))

(def md-ast (parse sample))

(def ontology (->
                (make-hierarchy)
                (derive :blist :block)
                (derive :fenced-code-block :block)
                (derive :indented-code-block :block)
                atom))

(defn decorate [recipient categories & clauses]
  (let [effect-map (apply
                     zipmap
                     (map
                       (fn [f]
                         (map
                           second
                           (filter #(->> % first f) (map-indexed vector clauses))))
                       [even? odd?]))]
    (reduce
      (fn [aggregator category] ((effect-map category) aggregator))
      recipient
      (filter (partial contains? effect-map) categories))))

(defn pack [string width]
  (if (->> string count (>= width))
    [string]
    (reduce
      (fn [aggregator word]
        (let [previous (peek aggregator)
              popped (if-not (empty? aggregator) (pop aggregator) aggregator)]
          (if (<= (+ (count previous) (count word) 1) width)
            (conj popped (string/join \space (remove nil? [previous word])))
            (conj aggregator word))))
      []
      (string/split string #"(?!\s*$) "))))

(defn decorate-plot
  [plot & traits]
  (map
    #(apply update % 2 update :traits (comp set conj) traits)
    plot))

(defmulti plot-ast (fn [node _] (first node)))

(defmulti next-at (fn [node _ _] (first node)) :hierarchy ontology)

(defn reflow
  ""
  [string
   {:keys [width height]}]
  (let [truncate #(take (or height (count %)) %)]
    (as-> string v
      (string/split-lines v)
      (map
        (fn [line]
          (->>
            (pack line width)
            (map #(hash-map :s % :c  (count %)))
            (#(reduce  (fn  [agg h]  (conj agg  (assoc h :art  (not= h  (last %)))))  [] %))))
        v)
      (flatten v)
      (truncate v)
      (reduce
        (fn [agg h]
          (assoc
            agg
            :length (+ (agg :length) (h :c) (if (h :art) 1 2))
            :breaks (conj (agg :breaks) (if (h :art) (+ (h :c) (agg :length)) nil))
            :reflowed (conj (agg :reflowed) (h :s))))
        {:reflowed [] :breaks [] :length 0}
        v)
      (assoc v
         :reflowed (string/join "\r\n" (v :reflowed)))
       (dissoc v :length :breaks)
       (:reflowed v)
       )))

(defmethod next-at :txt
  [_
   plot
   {:keys [width]}]
  (let [[x y] (second (last plot))
        overrun? (>= (inc x) width)]
    {:x (if overrun? 0 (inc x))
     :y (if overrun? (inc y) y)}))

(defmethod next-at :p
  [_ _ {:keys [y] :or {y 0}}]
  {:x 0
   :y (inc y)})

(defmethod next-at :sbr
  [_ _ {:keys [x y]}]
  {:x (+ x 2)
   :y y})

(defmethod next-at :hbr
  [_ _ {:keys [y]}]
  {:x 0
   :y (inc y)})

(defmethod next-at :block
  [_
   plot
   {:keys [top level]
    :or {top 0 level 0}}]
  (let [last-y (-> plot last second second (- top))]
    {:x 0
     :y (+ last-y (if (zero? level) 2 1))}))

(defmethod next-at :blitem
  [_ plot _]
  (let [[_ y] (second (last plot))]
    {:x 0
     :y (inc y)}))

(defmethod next-at :default
  [_ _ _]
  {:x -1 :y -1})

(defmethod plot-ast :txt
  [node
   {:keys [x y left top width height]
    :or {x 0 y 0 left 0 top 0}}]
  (let [string (second node)
        reflowed (reflow string {:width width})
        lines (string/split-lines reflowed)
        truncate #(take (or height (count %)) %)
        lengths (->> lines flatten truncate (map count))]
    (map
      vector
      (seq (string/join lines))
      (->>
        lengths
        (map-indexed (fn [index length]
                       (->>
                         length
                         range
                         (map #(vector (+ % left (if (zero? index) x 0)) (+ index top y))))))
        (reduce concat)))))

(defmethod plot-ast :indented-code-block
  [node
   {:as options
    :keys [x y]
    :or {x 0 y 0}}]
  (let [indent-length 4
        indent (->>
                 indent-length
                 range
                 (map (partial + x))
                 (map vector (repeat y))
                 (map reverse)
                 (map vector (repeat \space)))
        inner-options (assoc options :x (+ x indent-length))
        inner (plot-ast (assoc node 0 :txt) inner-options)]
    (->
      (concat indent inner)
      (decorate-plot :code))))

(defmethod plot-ast :blitem
  [node
   {:as options
    :keys [x y level]
    :or {x 0 y 0 level 0}}]
  (let [indent-length (* level 2)
        indent (->>
                 indent-length
                 range
                 (map (partial + x))
                 (map vector (repeat y))
                 (map reverse)
                 (map vector (repeat \space)))
        decor [[\+     [(+ x indent-length  ) y] {:traits #{:bullet}}]
               [\space [(+ x indent-length 1) y]                     ]]
        inner-options (assoc
                        options
                        :x (+ x indent-length (count decor))
                        :level (inc level))
        inner (plot-ast (assoc node 0 :default) inner-options)]
    (concat indent decor inner)))

(defmethod plot-ast :sbr
  [_
   {:keys [x y]
    :or {x 0 y 0}}]
  [[\space [x y]] [\space [(inc x) y]]])

(defmethod plot-ast :hbr
  [_ _]
  [])

(defmethod plot-ast :default
  [node
   {:as options
    :keys [x y]
    :or {x 0 y 0}}]
  (loop [contents (rest node)
         result []
         origin {:x x :y y}]
    (if (empty? contents)
      result
      (let [head (first contents)
            panned-options (merge options origin)
            head-plot (plot-ast head panned-options)
            next-at (next-at head head-plot panned-options)]
        (recur (rest contents) (concat result head-plot) next-at)))))

(defmethod plot-ast :h
  [node options]
  (-> node (assoc 0 :default) (plot-ast options) (decorate-plot :h)))

(defmethod plot-ast :em
  [node options]
  (-> node (assoc 0 :default) (plot-ast options) (decorate-plot :em)))

(defmethod plot-ast :strong
  [node options]
  (-> node (assoc 0 :default) (plot-ast options) (decorate-plot :strong)))

(defmethod plot-ast :fenced-code-block
  [node options]
  (-> node (assoc 0 :default) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :html-inline
  [node options]
  (-> node (assoc 0 :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :ref
  [node options]
  (-> node (assoc 0 :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :html-block
  [node options]
  (-> node (assoc 0 :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :tbr
  [node options]
  (-> node (assoc 0 :txt) (plot-ast options) (decorate-plot :code)))

(defmethod plot-ast :code
  [node options]
  (-> node (assoc 0 :default) (plot-ast options) (decorate-plot :code)))

(defn plot
  "Returns a sequence of pairs -one for each character of the input string-
   consisting of:
     * the character
     * the [x y] coordinates of the character"
  [string options]
  (plot-ast (parse string) options))

(defn line-count
  [string width]
  (->>
    (plot string {:width width})
    ((juxt last first))
    (map second)
    (map second)
    (reduce -)
    inc))
