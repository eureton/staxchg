(ns staxchg.io
  (:require [clojure.string :as string])
  (:require [clojure.core.async :as async :refer [>!! <!! thread close!]])
  (:require [clojure.java.shell])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.state.recipe :as state.recipe])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.recipe :as recipe])
  (:require [staxchg.request :as request])
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
  ; TODO: restore this after it stops breaking native image builds
  ; (:import com.googlecode.lanterna.terminal.DefaultTerminalFactory)
  (:import com.googlecode.lanterna.terminal.ansi.UnixTerminal)
  (:import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal$CtrlCBehaviour)
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
     (thread
       (assert (= "hello" (<!! channel#)))
       (>!! channel# (try
                       (do ~@body)
                       (finally (.close dialog#)))))
     (>!! channel# "hello")
     (.waitUntilClosed dialog#)
     (try
       (<!! channel#)
       (finally (async/close! channel#)))))

(defn show-message!
  ""
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

(defn refresh!
  ""
  [screen]
  (dev/log "[refresh]")
  (.refresh screen)) ; TODO provide refresh type according to outgoing recipes

(defn read-key!
  ""
  [screen]
  (dev/log "[read-key]")
  (let [keystroke (.readInput screen)
        keycode (.getCharacter keystroke)
        ctrl? (.isCtrlDown keystroke)]
    (dev/log "[read-key] code: '" keycode "', ctrl? " ctrl?)
    {:function :read-key!
     :values [keycode ctrl?]}))

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
       :values [term]})))

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
   :values [(blocking-fetch! url query-params screen)]})

(defn fetch-answers!
  [screen url query-params question-id]
  (dev/log "[fetch-answers] url: " url ", query-params: " query-params)
  {:function :fetch-answers!
   :values [(blocking-fetch! url query-params screen) question-id]})

(defn highlight-code!
  ""
  [code syntax id]
  (let [sh-out (try
                 (clojure.java.shell/sh
                   "skylighting"
                   "--format=html"
                   (str "--syntax=" syntax)
                   :in code)
                 (catch java.io.IOException _ nil))]
    (dev/log "[highlight-code] code: " code ", syntax: " syntax ", id: " id)
    {:function :highlight-code!
     :values [sh-out id]}))

(def request-channel (async/chan))

(def response-channel (async/chan))

(defn quit!
  ""
  [screen]
  (dev/log "[quit]")
  (close! request-channel)
  (close! response-channel)
  (.stopScreen screen))

(defn run-request-loop
  ""
  [in-channel out-channel]
  (loop []
    (request/route {:from in-channel :to out-channel})
    (recur)))

(defn register-theme!
  ""
  [theme-name filename]
  (LanternaThemes/registerTheme
    theme-name
    (PropertyTheme. (util/read-resource-properties filename) false)))

(defn run-input-loop
  ""
  [questions]
  (let [terminal (UnixTerminal.
                   System/in
                   System/out
                   (java.nio.charset.Charset/defaultCharset)
                   UnixLikeTerminal$CtrlCBehaviour/CTRL_C_KILLS_APPLICATION)
        ; TODO: restore this after it stops breaking native image builds
        ; terminal (.createTerminal (DefaultTerminalFactory.))
        screen (TerminalScreen. terminal)
        size (.getTerminalSize screen)]
    (.startScreen screen)
    (thread (run-request-loop request-channel response-channel))
    (let [init-world3 (state/initialize-world questions (.getColumns size) (.getRows size))
          init-world2 (assoc init-world3 :io/context {:screen screen})
          init-world (state/update-for-new-questions init-world2 questions)
          ]
      (loop [world init-world]
        (->> world request/make (>!! request-channel))
        (when-let [input (<!! response-channel)]
          (recur (state/update-world world input)))))))

