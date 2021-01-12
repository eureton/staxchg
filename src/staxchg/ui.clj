(ns staxchg.ui
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.recipe :as recipe])
  (:require [staxchg.dev :as dev])
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

(defn put-markdown!
  [graphics plot _]
  (dev/log
    "[put-markdown] "
    (->> (map second plot) (take 10) (apply str))
    " |>" (string/join (map first plot)) "<|")
  (doseq [[character [x y] categories] plot]
    (when-not (TerminalTextUtils/isControlCharacter character)
      (.setCharacter
        graphics
        x
        y
        (markdown/decorate
          (TextCharacter. character)
          categories
          :bold #(.withModifier % SGR/BOLD)
          :italic #(.withModifier % SGR/REVERSE)
          :monospace #(.withForegroundColor % TextColor$ANSI/GREEN)
          :code-block #(.withForegroundColor % TextColor$ANSI/GREEN))))))

(defn put-string!
  ""
  [graphics
   string
   {:keys [x y]
    :or {x 0 y 0}}]
  (dev/log "[put-string] " [x y] " |>"  string "<|")
  (.putString graphics x y string))

(defn scroll!
  ""
  [screen top bottom distance]
  (dev/log "[scroll] [" top " " bottom "] @ " distance)
  (.scrollLines screen top bottom distance))

(defn clear!
  ""
  [graphics]
  (dev/log "[clear] [" (-> graphics .getSize .getColumns) " " (-> graphics .getSize .getRows) "]")
  (.fillRectangle
    graphics
    (TerminalPosition. 0 0)
    (.getSize graphics)
    (->
      \space
      TextCharacter.
      (.withBackgroundColor TextColor$ANSI/RED)))
  ;(.fill graphics \space)
  )

(defn render-recipe
  [recipe]
  (doseq [{:keys [function params]} recipe]
    (let [f (case function
              :scroll! scroll!
              :clear! clear!
              :put-markdown! put-markdown!
              :put-string! put-string!)]
      (apply f params))))

(def questions-pane-flows
  [presentation/questions-pane-separator-flow
   presentation/question-list-flow
   presentation/questions-pane-body-flow
   presentation/question-meta-flow ])

(def answers-pane-flows
  [presentation/answers-pane-body-flow
   presentation/answer-meta-flow
   presentation/answer-acceptance-flow
   presentation/answers-pane-frame-flow])

(defn render [screen world]
  (->>
    (case (world :active-pane)
      :questions-pane questions-pane-flows
      :answers-pane answers-pane-flows)
    (map #(% world))
    (map recipe/make)
    (map (partial recipe/inflate screen))
    (run! render-recipe))
  (.refresh screen))

(defn run-input-loop
  ""
  [questions]
  (let [terminal (.createTerminal (DefaultTerminalFactory.))
        screen (TerminalScreen. terminal)
        size (.getTerminalSize screen)]
    (.startScreen screen)
    (loop [world-before (state/initialize-world questions (.getColumns size) (.getRows size))]
      (let [keystroke (.readInput screen)
            keycode (.getCharacter keystroke)
            world-after (state/update-world
                          world-before
                          keycode
                          (.isCtrlDown keystroke))]
        (when-not (= world-before world-after) (render screen world-after))
        (when-not (= keycode \q) (recur world-after))))
    (.stopScreen screen)))

