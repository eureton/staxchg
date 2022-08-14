(ns staxchg.io
  (:require [clojure.core.async :refer [chan >!! <!! thread close!]])
  (:require [clojure.java.shell])
  (:require [clojure.java.io :as io])
  (:require [staxchg.io.config :as config])
  (:require [staxchg.dev :as dev])
  (:require [clj-http.lite.client :as http])
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.TextCharacter)
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
  (:import com.googlecode.lanterna.input.KeyType)
  (:import com.googlecode.lanterna.screen.Screen$RefreshType)
  (:import com.googlecode.lanterna.screen.TerminalScreen)
  ; TODO: restore this after it stops breaking native image builds
  ; (:import com.googlecode.lanterna.terminal.DefaultTerminalFactory)
  (:import com.googlecode.lanterna.terminal.ansi.UnixTerminal)
  (:import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal$CtrlCBehaviour)
  (:gen-class))

(defn decorate-with-current
  "Decorates TextCharacter object with attributes read from TextGraphics object:
     1. foreground color
     2. background color
     3. modifiers

   If character has any of the above set to a non-default value, the latter is
   not modified."
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
  "Creates an instance of TextGUI and configures it with the \"staxchg\" theme."
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
  "Renders a spinning widget while body executes. The widget takes up the entire
   terminal. All input is ignored while the widget spins."
  [screen & body]
  `(let [gui# (themed-gui ~screen)
         dialog# (WaitingDialog/createDialog "" "Fetching...")
         channel# (chan)]
     (.setHints dialog# #{Window$Hint/CENTERED Window$Hint/MODAL})
     (.showDialog dialog# gui# false)
     (thread
       (assert (= "hello" (<!! channel#)))
       (>!! channel# (try
                       (do ~@body)
                       (finally (.close dialog#)))))
     (>!! channel# "hello")
     (.waitUntilClosed dialog#)
     (try
       (<!! channel#)
       (finally (close! channel#)))))

(defn show-message!
  "Displays a modal dialog with the given title and text. The dialog comes with
   a single OK button."
  [screen
   {:keys [title text] :or {title ""}}
   return]
  (let [gui (themed-gui screen)]
    (->
      (MessageDialogBuilder.)
      (.setTitle title)
      (.setText text)
      (.setExtraWindowHints [Window$Hint/MODAL Window$Hint/CENTERED])
      (.addButton MessageDialogButton/OK)
      (.build)
      (.showDialog gui))
    return))

(defn put-plot!
  "Renders plot, one character at a time using the given TextGraphics object.
   Decorates characters with the attributes of graphics, unless they have
   already been decorated. See staxchg.io/decorate-with-current for more."
  [graphics plot _]
  (doseq [[character [x y] t] plot]
    (.setCharacter graphics x y (decorate-with-current character graphics))))

(defn put-string!
  "Renders string using the given TextGraphics object. Starts at (x, y)."
  [graphics
   string
   {:keys [x y]
    :or {x 0 y 0}}]
  (.putString graphics x y string))

(defn scroll!
  "Scrolls the terminal content by distance rows, starting at top (inclusive)
   and ending at bottom (exclusive). Positive distance values scroll upwards,
   negative values, scroll downwards."
  [screen top bottom distance]
  (.scrollLines screen top bottom distance))

(defn clear!
  "Fills the terminal subrect which graphics has been configured for with
   undecorated space characters."
  [graphics _ _ _ _]
  (.fill graphics \space))

(defn refresh!
  "Commits the changes recorded so far in screen to the terminal."
  [screen]
  (.refresh screen)) ; TODO provide refresh type according to outgoing recipes

(defn sleep!
  "Pauses the main thread for interval milliseconds."
  [interval]
  (Thread/sleep interval))

(defn poll-resize!
  "Checks whether the terminal has been resized by the user. If so, provides the
   new dimensions of the terminal."
  [screen]
  {:function :poll-resize!
   :values [(.doResizeIfNecessary screen)]})

(defn drain-keystroke-queue!
  "Pops and discards all keystroke event off the lanterna event queue."
  [screen]
  (while (some-> (.pollInput screen)
                 .getKeyType
                 (not= KeyType/EOF))
    nil))

(defn poll-key!
  "First polls the lanterna event queue for keystroke events, then drains it.
   Consequently, if multiple keystrokes occur between successive calls to
   poll-key!, the application acts on the first and ignores the rest.
   The purpose of this policy is to throttle event processing on long-press."
  [screen]
  (let [keystroke (.pollInput screen)]
    (drain-keystroke-queue! screen)
    {:function :poll-key!
     :values [keystroke]}))

(defn query!
  "Renders a modal dialog which prompts the user to submit a query. The dialog
   comes with OK and Cancel buttons."
  [screen legend]
  (let [gui (themed-gui screen)
        dialog-width (-> screen .getTerminalSize .getColumns (* 0.8) int)
        dialog (->
                 (TextInputDialogBuilder.)
                 (.setTitle "")
                 (.setDescription legend)
                 (.setTextBoxSize (TerminalSize. dialog-width 1))
                 (.setExtraWindowHints #{Window$Hint/CENTERED})
                 (.build))]
    {:function :query!
     :values [(.showDialog dialog gui)]}))

(defn try-request!
  "Dispatches an HTTP request to url with the given method and parameters. If
   all goes well, returns the results of clj-http.lite.client/request.
   Otherwise, returns error-response."
  [url method query-params error-response]
  (try
    (http/request {:url url
                   :cookie-policy :standard
                   :method method
                   :query-params query-params})
    (catch Exception _ error-response)))

(defn blocking-fetch!
  "Dispatches an HTTP request and blocks user input while waiting for the
   result. See staxchg.io/try-request! for details about the parameters."
  [url query-params error-response screen]
  (->>
    (try-request! url "get" query-params error-response)
    (block-till-done! screen)))

(defn fetch-questions!
  "Blocks and waits for questions to be fetched. See staxchg.io/try-request! for
   details about the parameters."
  [screen url query-params error-response]
  {:function :fetch-questions!
   :values [(blocking-fetch! url query-params error-response screen)]})

(defn fetch-answers!
  "Blocks and waits for answers to be fetched. See staxchg.io/try-request! for
   details about the parameters."
  [screen url query-params error-response question-id]
  {:function :fetch-answers!
   :values [(blocking-fetch! url query-params error-response screen)
            question-id]})

(defn run-skylighting!
  "Forks a shell process attempting to run the skylighting tool. The command is
   configured to format the results in HTML and use the given syntax on the
   given code. Returns the shell output of clojure.java.shell/sh as is."
  [code syntax question-id answer-id]
  (let [sh-out (try
                 (clojure.java.shell/sh
                   "skylighting"
                   "--format" "html"
                   "--syntax" syntax
                   :in code)
                 (catch java.io.IOException _ nil))]
    {:function :highlight-code!
     :values [sh-out question-id answer-id]}))

(defn run-highlight.js!
  "Forks a shell process attempting to run the runhljs script. Returns the shell
   output of clojure.java.shell/sh as is."
  [code syntaxes question-id answer-id]
  (let [sh-out (try
                 (apply clojure.java.shell/sh
                        "runhljs"
                        (concat syntaxes [:in code]))
                 (catch java.io.IOException _ nil))]
    {:function :highlight-code!
     :values [sh-out question-id answer-id]}))

(defn quit!
  "Performs application shutdown tasks."
  [screen]
  (.stopScreen screen))

(defn register-theme!
  "Registers the theme whose description is filename with lanterna, under the
   name theme-name. Registering in this way must be done before any attempt to
   style a lanterna GUI with a theme."
  [theme-name filename]
  (LanternaThemes/registerTheme
    theme-name
    (PropertyTheme. (->> filename io/resource config/read-properties) false)))

(defn acquire-screen!
  "Checks the terminal which the application is run in for suitability. If
   successful, returns a lanterna Screen object."
  []
  (let [terminal (UnixTerminal.
                   System/in
                   System/out
                   (java.nio.charset.Charset/defaultCharset)
                   UnixLikeTerminal$CtrlCBehaviour/CTRL_C_KILLS_APPLICATION)]
        ; TODO: restore this after it stops breaking native image builds
        ; terminal (.createTerminal (DefaultTerminalFactory.))
    {:function :acquire-screen!
     :values [(TerminalScreen. terminal)]}))

(defn enable-screen!
  "Puts the terminal which the application is run in into private mode. See the
   lanterna documentation for more details."
  [screen]
  (.startScreen screen)
  {:function :enable-screen!
   :values []})

(defn read-config!
  "Reads the configuration file."
  []
  {:function :read-config!
   :values [(config/fetch)]})

