(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state
             :as state
             :only [selected-line-offset selected-question selected-answer]])
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.SGR)
  (:gen-class))

(defn put-markdown
  [graphics
   string
   {:as args
    :keys [left top width height line-offset]
    :or {left 0
         top 0
         width (->> graphics .getSize .getColumns)
         height (->> graphics .getSize .getRows)
         line-offset 0}}]
  (let [{:keys [stripped markdown-info]} (markdown/strip
                                           string
                                           (markdown/parse string))
        {:keys [reflowed markdown-info]} (markdown/reflow
                                           stripped
                                           markdown-info
                                           {:width width})
        {:keys [plotted markdown-info]} (markdown/plot
                                          reflowed
                                          markdown-info
                                          {:left left
                                           :top (- top line-offset)
                                           :width width})
        clipped? (fn [[_ [_ y] _]] (or (< y top) (> y (+ top height))))
        categories (->>
                     plotted
                     count
                     range
                     (map (partial markdown/categories markdown-info)))
        annotated-string (remove clipped? (map conj plotted categories))]
    (doseq [[character [x y] categories] annotated-string]
      (when-not (TerminalTextUtils/isControlCharacter character)
        (.setCharacter
          graphics
          x
          y
          (markdown/decorate (TextCharacter. character) categories
                          :bold #(.withModifier % SGR/BOLD)
                          :italic #(.withModifier % SGR/REVERSE)
                          :monospace #(.withForegroundColor % TextColor$ANSI/GREEN)
                          :code-block #(.withForegroundColor % TextColor$ANSI/GREEN)))))))

(defn render-question-list
  [screen {:as world
           :keys [questions selected-question-index question-list-size question-list-offset]}]
  (let [left 1
        top 0
        width (- (world :width) (* left 2))
        graphics (.newTextGraphics
                   (.newTextGraphics screen)
                   (TerminalPosition. left top)
                   (TerminalSize. width question-list-size))
        visible-questions (subvec
                            questions
                            question-list-offset
                            (min (count questions) (+ question-list-offset question-list-size)))]
    (doseq [[index title] (map-indexed #(vector %1 (%2 "title")) visible-questions)]
      (.putString graphics left index title))
    (.putString
      graphics
      left
      (- selected-question-index question-list-offset)
      (format (str "%-" width "s") ((state/selected-question world) "title"))
      [SGR/REVERSE])))

(defn render-selected-question [screen world]
  (let [left 1
        top (inc (world :question-list-size))
        width (- (world :width) (* left 2))
        height (- (world :height) top 1)
        graphics (.newTextGraphics
                   (.newTextGraphics screen)
                   (TerminalPosition. left top)
                   (TerminalSize. width height))]
    (put-markdown
      graphics
      ((state/selected-question world) "body_markdown")
      {:line-offset (state/selected-line-offset world)})))

(defn render-questions-pane
  [screen
   {:as world
    :keys [width question-list-size question-list-offset questions]}]
  (let [separator-y question-list-size
        graphics (.newTextGraphics screen)
        page-hint (string/join ["("
                                question-list-offset
                                "-"
                                (+ question-list-offset question-list-size)
                                " of "
                                (count questions)
                                ")"])]
    (render-question-list screen world)
    (.drawLine graphics 0 separator-y width separator-y \-)
    (.putString
      graphics
      (max 0 (- width (count page-hint) 1))
      separator-y
      page-hint)
    (render-selected-question screen world)))

(defn render-active-question [screen world]
  (.putString
    (.newTextGraphics screen)
    1
    0
    ((state/selected-question world) "title")
    [SGR/REVERSE]))

(defn render-selected-answer [screen world]
  (let [left 1
        top 2
        width (- (world :width) (* left 2))
        height (- (world :height) top)
        line-offset (state/selected-line-offset world)
        graphics (.newTextGraphics
             (.newTextGraphics screen)
             (TerminalPosition. left top)
             (TerminalSize. width (+ height line-offset)))]
    (put-markdown
      graphics
      ((state/selected-answer world) "body_markdown")
      {:top (- line-offset)})))

(defn render-answers-pane
  [screen
   {:as world
    :keys [width questions selected-question-index]}]
  (let [graphics (.newTextGraphics screen)
        hint (string/join ["("
                           (inc (state/selected-answer-index world))
                           " of "
                           (count ((questions selected-question-index) "answers"))
                           ")"])]
  (render-active-question screen world)
  (.drawLine (.newTextGraphics screen) 0 1 width 1 \-)
  (.putString graphics (max 0 (- width (count hint) 1)) 1 hint)
  (render-selected-answer screen world)))

(defn render [screen world]
  (.clear screen)
  (case (world :active-pane)
    :questions-pane (render-questions-pane screen world)
    :answers-pane (render-answers-pane screen world))
  (.refresh screen))

