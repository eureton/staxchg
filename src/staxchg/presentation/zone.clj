(ns staxchg.presentation.zone
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn answers-footer-left-width
  ""
  [width]
  (quot width 3))

(defn questions-header
  ""
  [{:keys [width switched-pane?]}]
  {:left question-list-left
   :top 0
   :width (- width (* question-list-left 2))
   :height question-list-size
   :clear? switched-pane?})

(defn questions-separator
  ""
  [{:keys [width]}]
  {:left 0
   :top question-list-size
   :width width
   :height 1})

(defn questions-body
  ""
  [{:keys [width height switched-pane? switched-question?]}]
  {:left questions-body-left
   :top questions-body-top
   :width (- width (* questions-body-left 2))
   :height (- height questions-body-top 1)
   :clear? (or switched-pane? switched-question?)})

(defn questions-footer
  ""
  [{:keys [width height switched-pane? switched-question?]}]
  {:left 0
   :top (dec height)
   :width width
   :height 1
   :clear? (or switched-pane? switched-question?)})

(defn answers-body
  ""
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left answer-body-left
   :top answer-body-top
   :width (- width (* answer-body-left 2))
   :height (- height answer-body-top 1)
   :clear? (or switched-pane? switched-answer?)})

(defn answers-footer-left
  ""
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left 0
   :top (dec height)
   :width (answers-footer-left-width width)
   :height 1
   :clear? (or switched-pane? switched-answer?)})

(defn answers-footer-right
  ""
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left (answers-footer-left-width width)
   :top (dec height)
   :width (- width (answers-footer-left-width width))
   :height 1
   :clear? (or switched-pane? switched-answer?)})

(defn answers-header
  ""
  [{:keys [width]}]
  {:left answers-header-left
   :top 0
   :width (- width (* answers-header-left 2))
   :height answers-header-height})

(defn answers-separator
  ""
  [{:keys [width switched-pane?]}]
  {:left 0
   :top answers-header-height
   :width width
   :height (inc answers-separator-height)
   :clear? switched-pane?})

(defn full-screen
  ""
  [{:keys [width height switched-pane?]}]
  {:left 0
   :top 0
   :width width
   :height height
   :clear? switched-pane?})

