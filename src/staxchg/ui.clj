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

(defn fetch-questions!
  [url query-params screen]
  (dev/log "[fetch-questions] url: " url ", query-params: " query-params)
  (block-till-done! screen {:function :fetch-questions!
                            :params [(http/request {:url url
                                                    :cookie-policy :standard
                                                    :method "get"
                                                    :query-params query-params})]}))

(defn fetch-answers!
  [url query-params question-id screen]
  (dev/log "[fetch-answers] url: " url ", query-params: " query-params)
  (block-till-done! screen {:function :fetch-answers!
                            :params [(http/request {:url url
                                                    :cookie-policy :standard
                                                    :method "get"
                                                    :query-params query-params})
                                     question-id]}))

(defn read-input
  ""
  [screen
   {:as world
    :keys [query? search-term fetch-answers]}]
  (cond
    query? (query! screen)
    search-term (fetch-questions!
                  (api/questions-url)
                  (api/questions-query-params search-term)
                  screen)
    fetch-answers (fetch-answers!
                    (api/answers-url (fetch-answers :question-id))
                    (api/answers-query-params (fetch-answers :page))
                    (fetch-answers :question-id)
                    screen)
    :else (read-key! screen)))

(defn commit-recipe
  [recipe]
  (doseq [{:keys [function params]} recipe]
    (let [f (case function
              :scroll! scroll!
              :clear! clear!
              :put-markdown! put-markdown!
              :put-string! put-string!)]
      (apply f params))))

(defn write-output
  ""
  [screen world]
  (->>
    (presentation/recipes world)
    (map (partial recipe/inflate screen))
    (run! commit-recipe))
  (.refresh screen)) ; TODO provide refresh type according to outgoing recipes

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
        size (.getTerminalSize screen)]
    (.startScreen screen)
    (let [init-world (state/initialize-world questions (.getColumns size) (.getRows size))]
      (write-output screen init-world)
      (loop [world-before init-world]
        (let [input (read-input screen world-before)
              world-after (state/update-world world-before input)]
          (when-not (state/generated-output? world-before world-after)
            (write-output screen world-after))
          (when-not (world-after :quit?)
            (recur world-after)))))
    (.stopScreen screen)))

