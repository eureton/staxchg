(ns staxchg.ui
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:import com.googlecode.lanterna.screen.Screen$RefreshType)
  (:import com.googlecode.lanterna.screen.TerminalScreen)
  (:import com.googlecode.lanterna.terminal.DefaultTerminalFactory)
  (:gen-class))

(defn decorate
  ""
  [character foreground-color background-color modifiers]
  (as->
    character v
    (TextCharacter. v)
    (.withForegroundColor v foreground-color)
    (.withBackgroundColor v background-color)
    (reduce #(.withModifier %1 %2) v modifiers)))

(defn put-markdown
  [graphics
   string
   {:as args
    :keys [left top width height modifiers foreground-color background-color]
    :or {left 0
         top 0
         width (->> graphics .getSize .getColumns)
         height (->> graphics .getSize .getRows)
         modifiers []
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/BLACK}}]
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
          (markdown/decorate
            (decorate character foreground-color background-color modifiers)
            categories
            :bold #(.withModifier % SGR/BOLD)
            :italic #(.withModifier % SGR/REVERSE)
            :monospace #(.withForegroundColor % TextColor$ANSI/GREEN)
            :code-block #(.withForegroundColor % TextColor$ANSI/GREEN)))))))

(defn put-string
  ""
  [graphics
   string
   {:keys [x y modifiers foreground-color background-color]
    :or {x 0
         y 0
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/DEFAULT}}]
  (let [graphics (->
                   graphics
                   (.setForegroundColor foreground-color)
                   (.setBackgroundColor background-color)
                   (.enableModifiers (into-array SGR modifiers)))]
    (.putString graphics x y string)))

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

(defn render-questions-pane-separator
  ""
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
    (.drawLine graphics 0 separator-y width separator-y \-)
    (.putString
      graphics
      (max 0 (- width (count page-hint) 1))
      separator-y
      page-hint)))

(defn render-plot
  [screen plot]
  (doseq [{:as args
           :keys [type x y payload foreground-color]
           :viewport/keys [left top width height]} plot]
    (when (and (pos? width) (pos? height))
      (let [graphics (->
                       screen
                       .newTextGraphics
                       (.newTextGraphics
                         (TerminalPosition. left top)
                         (TerminalSize. width height))
                       (.setForegroundColor foreground-color))
            options (select-keys args [:x :y :foreground-color :modifiers])]
        (condp = type
          :markdown (put-markdown graphics payload {:left x :top y :foreground-color foreground-color})
          :string   (put-string graphics payload options))))))

(defn render-questions-pane
  [screen world]
  (render-question-list screen world)
  (render-questions-pane-separator screen world)
  (render-plot screen (presentation/question-pane-body-plot
                        (state/selected-question world)
                        (state/selected-line-offset world)
                        world))
  (render-plot screen (presentation/question-meta-plot
                        (state/selected-question world)
                        world)))

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
                    (presentation/format-answer-meta answer))
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
  (.refresh screen Screen$RefreshType/COMPLETE))

(defn run-input-loop
  ""
  [questions]
  (let [terminal (.createTerminal (DefaultTerminalFactory.))
        screen (TerminalScreen. terminal)
        size (.getTerminalSize screen)]
    (.startScreen screen)
    (loop [world-before (state/initialize-world questions (.getColumns size) (.getRows size))]
      (let [keycode (.getCharacter (.readInput screen))
            world-after (state/update-world world-before keycode)]
        (when-not (= world-before world-after) (render screen world-after))
        (when-not (= keycode \q) (recur world-after))))
    (.stopScreen screen)))
