(ns staxchg.presentation
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
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
    :keys [left top width height]
    :or {left 0
         top 0
         width (->> graphics .getSize .getColumns)
         height (->> graphics .getSize .getRows)}}]
  (let [{:keys [plotted markdown-info]} (markdown/plot
                                          string
                                          {:left left
                                           :top top
                                           :width width})
        clipped? (fn [[_ [_ y] _]] (or (neg? y) (>= y height)))
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

(defn format-date
  ""
  [unixtime]
  (let [datetime (java.time.LocalDateTime/ofEpochSecond unixtime 0 java.time.ZoneOffset/UTC)]
    (format
      "%4d-%02d-%02d %02d:%02d"
      (.getYear datetime)
      (.getValue (.getMonth datetime))
      (.getDayOfMonth datetime)
      (.getHour datetime)
      (.getMinute datetime))))

(defn render-question-list
  [screen {:as world
           :keys [selected-question-index question-list-size question-list-offset]}]
  (let [left 1
        top 0
        width (- (world :width) (* left 2))
        graphics (.newTextGraphics
                   (.newTextGraphics screen)
                   (TerminalPosition. left top)
                   (TerminalSize. width question-list-size))
        visible (state/visible-questions world)]
    (doseq [[index title] (map-indexed #(vector %1 (%2 "title")) visible)]
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
        height (- (world :height) top)
        graphics (.newTextGraphics
                   (.newTextGraphics screen)
                   (TerminalPosition. left top)
                   (TerminalSize. width height))
        question (state/selected-question world)
        meta-y (- height 1)
        meta-text (format
                    (str "%" width "s")
                    (format
                      "(S: %d) | (V: %d) | %s (%s) | %s"
                      (question "score")
                      (question "view_count")
                      (get-in question ["owner" "display_name"])
                      (get-in question ["owner" "reputation"])
                      (format-date (question "last_activity_date"))))
        meta-formatter #(->
                          %
                          TextCharacter.
                          (.withForegroundColor TextColor$ANSI/YELLOW))]
    (put-markdown
      graphics
      (question "body_markdown")
      {:top (- (state/selected-line-offset world))})
    (doseq [[index character] (map-indexed vector meta-text)]
      (.setCharacter
        graphics
        index
        meta-y
        (meta-formatter character)))))

(defn render-questions-pane
  [screen
   {:as world
    :keys [width question-list-size question-list-offset questions]}]
  (let [separator-y question-list-size
        graphics (.newTextGraphics screen)
        page-hint (format
                    "(%d-%d of %d)"
                    question-list-offset
                    (+ question-list-offset question-list-size)
                    (count questions))]
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
        graphics (.newTextGraphics
             (.newTextGraphics screen)
             (TerminalPosition. left top)
             (TerminalSize. width height))
        answer (state/selected-answer world)
        meta-y (- height 1)
        meta-text (format
                    (str "%" width "s")
                    (format
                      "%d | %s (%s) | %s"
                      (answer "score")
                      (get-in answer ["owner" "display_name"])
                      (get-in answer ["owner" "reputation"])
                      (format-date (answer "last_activity_date"))))
        meta-formatter #(->
                          %
                          TextCharacter.
                          (.withForegroundColor TextColor$ANSI/YELLOW))
        acceptance-formatter #(->
                                %
                                TextCharacter.
                                (.withForegroundColor TextColor$ANSI/BLACK)
                                (.withBackgroundColor TextColor$ANSI/YELLOW))]
    (put-markdown
      graphics
      (answer "body_markdown")
      {:top (- (state/selected-line-offset world))})
    (doseq [[index character] (map-indexed vector meta-text)]
      (.setCharacter
        graphics
        index
        meta-y
        (meta-formatter character)))
    (doseq [[index character] (map-indexed vector (when (answer "is_accepted") "ACCEPTED"))]
      (.setCharacter
        graphics
        index
        meta-y
        (acceptance-formatter character)))))

(defn render-answers-pane
  [screen
   {:as world
    :keys [width]}]
  (let [graphics (.newTextGraphics screen)
        index (state/selected-answer-index world)
        answered? (not (nil? index))
        hint (if answered?
               (format
                 "(%d of %d)"
                 (inc index)
                 (count ((state/selected-question world) "answers")))
               "(question has no answers)")]
    (render-active-question screen world)
    (.drawLine (.newTextGraphics screen) 0 1 width 1 \-)
    (.putString graphics (max 0 (- width (count hint) 1)) 1 hint)
    (when answered? (render-selected-answer screen world))))

(defn render [screen world]
  (.clear screen)
  (case (world :active-pane)
    :questions-pane (render-questions-pane screen world)
    :answers-pane (render-answers-pane screen world))
  (.refresh screen))

