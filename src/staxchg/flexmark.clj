(ns staxchg.flexmark
  (:require [staxchg.ast :as ast])
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
  [node zerof addf leaff]
  (if (.hasChildren node)
    (loop [iterator (.getChildIterator node)
           result (zerof (tag node))]
      (if (.hasNext iterator)
        (recur
          iterator
          (addf result (unpack (.next iterator) zerof addf leaff)))
        result))
    (leaff (tag node) (-> node .getChars .unescape))))

(defn parse
  ""
  [string]
  (let [parser (-> (Parser/builder) .build)
        root (.parse parser string)]
    (unpack root ast/zero ast/add ast/leaf)))

