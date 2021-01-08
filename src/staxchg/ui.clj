(ns staxchg.ui
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.flow :as flow])
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
  [graphics plot _]
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

(defn put-string
  ""
  [graphics
   string
   {:keys [x y]
    :or {x 0 y 0}}]
  (.putString graphics x y string))

(defn viewport
  ""
  [{:as item
    :keys [y-offset]
    :viewport/keys [left top width height]}]
  (let [scroll-filler? (flow/scrolled? [item])
        top (if scroll-filler? (- (+ top height) 1) top)
        height (if scroll-filler? 1 height)]
    {:left left
     :top top
     :width width
     :height height}))

(defn sub-graphics
  ""
  [screen
   {:as args
    :keys [y-offset foreground-color background-color modifiers]
    :viewport/keys [left top width height]}]
  (let [{:keys [left top width height]} (viewport args)]
    (->
      screen
      .newTextGraphics
      (.newTextGraphics
        (TerminalPosition. left top)
        (TerminalSize. width height))
      (.enableModifiers (into-array SGR modifiers))
      (.setForegroundColor foreground-color)
      (.setBackgroundColor background-color))))

(defn render-flow
  [screen flow]
  (when-some [h (first flow)]
    (if (flow/scrolled? flow)
      (.scrollLines screen (h :viewport/top) (+ (h :viewport/top) (dec (h :viewport/height))) (h :scrolled-by))
      (.fill (sub-graphics screen h) \space)))
  (doseq [{:as item
           :viewport/keys [width height]} flow]
    (when (and (pos? width) (pos? height))
      (let [vp (viewport item)
            bbox {:top (- (vp :top) (item :viewport/top)) :height (vp :height)}
            graphics (sub-graphics screen item)
            options (select-keys item [:x :y])
            f (case (item :type) :markdown put-markdown :string put-string)]
        (f graphics (flow/payload item bbox) options)))))

(def questions-pane-flow-recipe
  [presentation/questions-pane-separator-flow
   presentation/question-list-flow
   presentation/questions-pane-body-flow
   presentation/question-meta-flow ])

(def answers-pane-flow-recipe
  [presentation/answers-pane-body-flow
   presentation/answer-meta-flow
   presentation/answer-acceptance-flow
   presentation/answers-pane-frame-flow])

(defn render [screen world]
  (->>
    (case (world :active-pane)
      :questions-pane questions-pane-flow-recipe
      :answers-pane answers-pane-flow-recipe)
    (map #(% world))
    (run! (partial render-flow screen)))
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

