(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state :only [selected-line-offset]])
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.SGR)
  (:gen-class))

(defn slice
  "Slices the input string into pieces of the given length and returns them in a
  sequence. No padding is applied to the last piece."
  [string width]
  (->>
    string
    (partition width width (repeat \space))
    (map string/join)
    (map string/trim)))

(defn plot
  "Returns a sequence of pairs -one for each character of the input string-
  consisting of:
    * the character
    * the [x y] coordinates of the character"
  [string
   {:keys [left top width height]
    :or {height -1}}]
  (let [lines (string/split-lines string)
        truncate #(take (if (= -1 height) (count %) height) %)]
    (map
      vector
      (seq (string/join lines))
      (->>
        lines
        (map #(slice % width))
        flatten
        truncate
        (map count)
        (map-indexed (fn [index length]
                       (->>
                         length
                         range
                         (map #(vector (+ % left) (+ index top))))))
        (reduce concat)))))

(defn pack [string width]
  (if (->> string count (>= width))
    [string]
    (reduce
      (fn [aggregator word]
        (let [previous (last aggregator)]
          (if (<= (+ (count previous) (count word) 1) width)
            (conj (pop aggregator) (string/join \space (remove string/blank? [previous word])))
            (conj aggregator word))))
      [""]
      (string/split string #" "))))

(defn reflow
  ""
  [string
   {:keys [width height]
    :or {height -1}}]
  (let [truncate #(take (if (= -1 height) (count %) height) %)]
    (->>
      string
      string/split-lines
      (map #(pack % width))
      flatten
      truncate
      (string/join "\r\n"))))

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
  (let [reflowed (reflow string {:width width})
        plot (plot reflowed {:left left :top (- top line-offset) :width width})
        clipped? (fn [[_ [_ y] _]] (or (< y top) (> y (+ top height))))
        markdown-info (->> plot (map first) string/join markdown/parse)
        categories (->> plot count range (map (partial markdown/categories markdown-info)))
        annotated-string (remove clipped? (map conj plot categories))]
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
                          :code-block #(.withForegroundColor % TextColor$ANSI/GREEN))))
                          )))

(defn line-count
  [string width]
  (let [reflowed (reflow string {:width width})
        plot (plot reflowed {:left 0 :top 0 :width width})]
    (inc (- (->> plot last second second) (->> plot first second second)))))

(defn render-question-list
  [screen {:as world
           :keys [questions selected-question-index question-list-size question-list-offset]}]
  (let [left 1
        top 0
        width (- (world :width) (* left 2))
        selected-question (get-in world [:questions (world :selected-question-index)])
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
      (format (str "%-" width "s") (selected-question "title"))
      [SGR/REVERSE])))

(defn render-selected-question [screen world]
  (let [left 1
        top (inc (world :question-list-size))
        width (- (world :width) (* left 2))
        height (- (world :height) top 1)
        selected-question (get-in world [:questions (world :selected-question-index)])
        graphics (.newTextGraphics
                   (.newTextGraphics screen)
                   (TerminalPosition. left top)
                   (TerminalSize. width height))]
    (put-markdown
      graphics
      (selected-question "body_markdown")
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
    (get-in world [:questions (world :selected-question-index) "title"])
    [SGR/REVERSE]))

(defn render-answers [screen world]
  (let [left 1
        top 2
        width (- (world :width) (* left 2))
        height (- (world :height) top)
        answers (get-in world [:questions (world :selected-question-index) "answers"])
        answer-count (count answers)
        line-offset (state/selected-line-offset world)
        graphics (.newTextGraphics
             (.newTextGraphics screen)
             (TerminalPosition. left top)
             (TerminalSize. width (+ height line-offset)))]
    (loop [index 0 y (- line-offset)]
      (when (< index answer-count)
        (let [answer (get answers index)
              text (answer "body_markdown")
              separator-height (if (pos? index) 1 0)]
          (when (pos? index) (.drawLine graphics 0 y width y \=))
          (put-markdown graphics text {:top (+ y separator-height)})
          (recur (inc index) (+ y separator-height (line-count text width))))))))

(defn render-answers-pane [screen world]
  (render-active-question screen world)
  (.drawLine (.newTextGraphics screen) 0 1 (world :width) 1 \-)
  (render-answers screen world))

(defn render [screen world]
  (.clear screen)
  (case (world :active-pane)
    :questions-pane (render-questions-pane screen world)
    :answers-pane (render-answers-pane screen world))
  (.refresh screen))

