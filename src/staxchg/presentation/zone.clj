(ns staxchg.presentation.zone
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.dev :as dev])
  (:gen-class))

(defn answers-footer-left-width
  "Implementation detail - do not call directly."
  [width]
  (quot width 3))

(defn questions-header
  "Zone for the header of the questions pane."
  [{:keys [width question-list-size switched-pane?]}]
  {:left question-list-left
   :top 0
   :width (- width (* question-list-left 2))
   :height question-list-size
   :clear? switched-pane?})

(defn questions-separator
  "Zone for the separator of the questions pane."
  [{:keys [width question-list-size]}]
  {:left 0
   :top question-list-size
   :width width
   :height 1})

(defn questions-body
  "Zone for the body of the questions pane."
  [{:keys [width height question-list-size switched-pane? switched-question?]}]
  (let [questions-body-top (inc question-list-size)]
    {:left questions-body-left
     :top questions-body-top
     :width (- width (* questions-body-left 2))
     :height (- height questions-body-top 1)
     :clear? (or switched-pane? switched-question?)}))

(defn questions-footer
  "Zone for the footer of the questions pane."
  [{:keys [width height switched-pane? switched-question?]}]
  {:left 0
   :top (dec height)
   :width width
   :height 1
   :clear? (or switched-pane? switched-question?)})

(defn answers-body
  "Zone for the body of the answers pane."
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left answer-body-left
   :top answer-body-top
   :width (- width (* answer-body-left 2))
   :height (- height answer-body-top 1)
   :clear? (or switched-pane? switched-answer?)})

(defn answers-footer-left
  "Zone for the left part of the footer of the questions pane."
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left 0
   :top (dec height)
   :width (answers-footer-left-width width)
   :height 1
   :clear? (or switched-pane? switched-answer?)})

(defn answers-footer-right
  "Zone for the right part of the footer of the questions pane."
  [{:keys [width height switched-pane? switched-answer?]}]
  {:left (answers-footer-left-width width)
   :top (dec height)
   :width (- width (answers-footer-left-width width))
   :height 1
   :clear? (or switched-pane? switched-answer?)})

(defn answers-header
  "Zone for the header of the answers pane."
  [{:keys [width]}]
  {:left answers-header-left
   :top 0
   :width (- width (* answers-header-left 2))
   :height answers-header-height})

(defn answers-separator
  "Zone for the separator of the answers pane."
  [{:keys [width switched-pane?]}]
  {:left 0
   :top answers-header-height
   :width width
   :height (inc answers-separator-height)
   :clear? switched-pane?})

