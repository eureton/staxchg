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
    :keys [x y left top width height modifiers foreground-color background-color]
    :or {x 0
         y 0
         left 0
         top 0
         width (->> graphics .getSize .getColumns)
         height (->> graphics .getSize .getRows)
         modifiers []
         foreground-color TextColor$ANSI/DEFAULT
         background-color TextColor$ANSI/DEFAULT}}]
  (let [{:keys [plotted markdown-info]} (markdown/plot
                                          string
                                          (select-keys args [:x :y :left :top :width]))
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

(defn render-flow
  [screen flow]
  (doseq [{:as args
           :keys [payload foreground-color]
           :viewport/keys [left top width height]} flow]
    (when (and (pos? width) (pos? height))
      (let [graphics (->
                       screen
                       .newTextGraphics
                       (.newTextGraphics
                         (TerminalPosition. left top)
                         (TerminalSize. width height))
                       (.setForegroundColor foreground-color))
            options (merge
                      (select-keys
                        args
                        [:x :y :foreground-color :background-color :modifiers])
                      {:left 0 :top 0 :width width})
            f (case (args :type) :markdown put-markdown :string put-string)]
        (f graphics payload options)))))

(def questions-pane-flow-recipe
  [presentation/questions-pane-separator-flow
   presentation/question-list-flow
   presentation/questions-pane-body-flow
   presentation/question-meta-flow ])

(def answers-pane-flow-recipe
  [presentation/answer-flow
   presentation/answer-meta-flow
   presentation/answer-acceptance-flow
   presentation/answers-pane-frame-flow])

(defn render [screen world]
  (.clear screen)
  (->>
    (case (world :active-pane)
      :questions-pane questions-pane-flow-recipe
      :answers-pane answers-pane-flow-recipe)
    (map #(% world))
    (run! (partial render-flow screen)))
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
