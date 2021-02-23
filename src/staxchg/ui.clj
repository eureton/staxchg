(ns staxchg.ui
  (:require [clojure.string :as string])
  (:require [clojure.core.async :as async :refer [>!! <!!]])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.recipe :as recipe])
  (:require [staxchg.api :as api])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.util :as util])
  (:require [clj-http.client :as http])
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:import com.googlecode.lanterna.bundle.LanternaThemes)
  (:import com.googlecode.lanterna.graphics.PropertyTheme)
  (:import com.googlecode.lanterna.gui2.DefaultWindowManager)
  (:import com.googlecode.lanterna.gui2.MultiWindowTextGUI)
  (:import com.googlecode.lanterna.gui2.SameTextGUIThread$Factory)
  (:import com.googlecode.lanterna.gui2.Window$Hint)
  (:import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder)
  (:import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton)
  (:import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder)
  (:import com.googlecode.lanterna.gui2.dialogs.WaitingDialog)
  (:import com.googlecode.lanterna.screen.Screen$RefreshType)
  (:import com.googlecode.lanterna.screen.TerminalScreen)
  (:import com.googlecode.lanterna.terminal.DefaultTerminalFactory)
  (:gen-class))

(defn decorate-with-current
  ""
  [character graphics]
  (let [fg-color (.getForegroundColor character)
        bg-color (.getBackgroundColor character)
        default? #(= TextColor$ANSI/DEFAULT %)
        modifiers (.getModifiers character)
        current-modifiers (-> graphics .getActiveModifiers vec)]
    (cond-> character
      (default? fg-color) (.withForegroundColor (.getForegroundColor graphics))
      (default? bg-color) (.withBackgroundColor (.getBackgroundColor graphics))
      (and (empty? modifiers)
           (not-empty current-modifiers)) (.withModifiers current-modifiers))))

(defn themed-gui
  ""
  [screen]
  (let [theme (LanternaThemes/getRegisteredTheme "staxchg")
        size (.getTerminalSize screen)
        gui (MultiWindowTextGUI.
              (SameTextGUIThread$Factory.)
              screen
              (DefaultWindowManager. size))]
    (.setTheme gui theme)
    gui))

(defmacro block-till-done!
  [screen & body]
  `(let [gui# (themed-gui ~screen)
         dialog# (WaitingDialog/createDialog "" "Fetching...")
         channel# (async/chan)]
     (.setHints dialog# #{Window$Hint/CENTERED Window$Hint/MODAL})
     (.showDialog dialog# gui# false)
     (async/thread
       (assert (= "hello" (<!! channel#)))
       (>!! channel# (try
                       (do ~@body)
                       (finally (.close dialog#)))))
     (>!! channel# "hello")
     (.waitUntilClosed dialog#)
     (try
       (<!! channel#)
       (finally (async/close! channel#)))))

(defmacro show-message!
  ""
  [screen
   {:keys [title text] :or {title ""}}
   & body]
  `(let [gui# (themed-gui ~screen)]
     (->
       (MessageDialogBuilder.)
       (.setTitle ~title)
       (.setText ~text)
       (.setExtraWindowHints [Window$Hint/MODAL Window$Hint/CENTERED])
       (.addButton MessageDialogButton/OK)
       (.build)
       (.showDialog gui#))
     (do ~@body)))

(defn put-markdown!
  [graphics plot _]
  (dev/log
    "[put-markdown] "
    (->> (map second plot) (take 10) (apply str))
    " |>" (string/join (->> plot (map first) (map #(.getCharacter %)))) "<|")
  (doseq [[character [x y]] plot]
    (.setCharacter graphics x y (decorate-with-current character graphics))))

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
  (.fill graphics \space))

(defn read-key!
  ""
  [screen]
  (dev/log "[read-key]")
  (let [keystroke (.readInput screen)
        keycode (.getCharacter keystroke)
        ctrl? (.isCtrlDown keystroke)]
    (dev/log "[read-key] code: '" keycode "', ctrl? " ctrl?)
    {:function :read-key!
     :params [keycode ctrl?]}))

(defn query!
  ""
  [screen]
  (dev/log "[query]")
  (let [gui (themed-gui screen)
        dialog-width (-> screen .getTerminalSize .getColumns (* 0.8) int)
        dialog (->
                 (TextInputDialogBuilder.)
                 (.setTitle "")
                 (.setDescription presentation/search-legend)
                 (.setTextBoxSize (TerminalSize. dialog-width 1))
                 (.setExtraWindowHints #{Window$Hint/CENTERED})
                 (.build))]
    (let [term (.showDialog dialog gui)]
      (dev/log "[query] " (if (some? term) (str "term: '" term "'") "<canceled>"))
      {:function :query!
       :params [term]})))

(defn try-request!
  ""
  [url method query-params]
  (try
    (http/request {:url url
                   :cookie-policy :standard
                   :method method
                   :query-params query-params})
    (catch Exception _ api/error-response)))

(defn blocking-fetch!
  ""
  [url query-params screen]
  (->>
    (try-request! url "get" query-params)
    (block-till-done! screen)))

(defn fetch-questions!
  [screen url query-params]
  (dev/log "[fetch-questions] url: " url ", query-params: " query-params)
  {:function :fetch-questions!
   :params [(blocking-fetch! url query-params screen)]})

(defn fetch-answers!
  [screen url query-params question-id]
  (dev/log "[fetch-answers] url: " url ", query-params: " query-params)
  {:function :fetch-answers!
   :params [(blocking-fetch! url query-params screen) question-id]})

(defn read-input
  ""
  [screen channel]
  (loop []
    (recipe/route
      screen
      channel
      (<!! channel))
    (recur)))

(defn write-output
  ""
  [screen channel]
  (loop []
    (recipe/route screen nil
      (<!! channel)
      (.refresh screen)) ; TODO provide refresh type according to outgoing recipes
    (recur)))

(defn register-theme!
  ""
  [theme-name pathname]
  (LanternaThemes/registerTheme
    theme-name
    (PropertyTheme. (util/read-properties pathname) false)))

(defn run-input-loop
  ""
  [questions]
  (let [terminal (.createTerminal (DefaultTerminalFactory.))
        screen (TerminalScreen. terminal)
        size (.getTerminalSize screen)
        input-channel (async/chan)
        output-channel (async/chan)]
    (.startScreen screen)
    (async/thread
      (read-input screen input-channel))
    (async/thread
      (write-output screen output-channel))
    (let [init-world (state/initialize-world questions (.getColumns size) (.getRows size))]
      (->> init-world presentation/recipes (>!! output-channel))
      (loop [world-before init-world]
        (->> world-before state/input-recipes (>!! input-channel))
        (let [input (<!! input-channel)
              world-after (state/update-world world-before input)]
          (when-not (state/generated-output? world-before world-after)
            (->> world-after presentation/recipes (>!! output-channel)))
          (when-not (world-after :quit?)
            (recur world-after)))))
    (.stopScreen screen)))

