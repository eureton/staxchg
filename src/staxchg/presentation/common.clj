(ns staxchg.presentation.common
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(def horz-bar Symbols/SINGLE_LINE_HORIZONTAL)

(def frame-color TextColor$ANSI/YELLOW)

(def acceptance-text " ACCEPTED ")

(def question-list-left 0)

(def question-list-size 2)

(def questions-body-top (inc question-list-size))

(def questions-body-left 1)

(def answer-body-left 1)

(def answers-header-height 1)

(def answers-separator-height 1)

(def answer-body-top (+ answers-header-height answers-separator-height))

(def answers-header-left 1)

(def comments-left-margin 16)

