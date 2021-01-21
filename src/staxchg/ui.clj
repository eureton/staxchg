(ns staxchg.ui
  (:require [clojure.string :as string])
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:require [staxchg.flow :as flow])
  (:require [staxchg.recipe :as recipe])
  (:require [staxchg.api :as api])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.util :as util])
  (:require [clj-http.client :as http])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TerminalPosition)
  (:import com.googlecode.lanterna.TerminalSize)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:import com.googlecode.lanterna.bundle.LanternaThemes)
  (:import com.googlecode.lanterna.graphics.PropertyTheme)
  (:import com.googlecode.lanterna.gui2.DefaultWindowManager)
  (:import com.googlecode.lanterna.gui2.MultiWindowTextGUI)
  (:import com.googlecode.lanterna.gui2.SameTextGUIThread$Factory)
  (:import com.googlecode.lanterna.gui2.Window$Hint)
  (:import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder)
  (:import com.googlecode.lanterna.screen.Screen$RefreshType)
  (:import com.googlecode.lanterna.screen.TerminalScreen)
  (:import com.googlecode.lanterna.terminal.DefaultTerminalFactory)
  (:gen-class))

(defn decorate-with-current
  ""
  [character graphics]
  (as->
    character v
    (TextCharacter. v)
    (.withForegroundColor v (.getForegroundColor graphics))
    (.withBackgroundColor v (.getBackgroundColor graphics))
    (reduce #(.withModifier %1 %2) v (.getActiveModifiers graphics))))

(defn rewrite-with-symbols
  ""
  [plot]
  (map
    #(assoc % 0 (if (contains? (nth % 2) :bullet-list) Symbols/BULLET (nth % 0)))
    plot))

(defn put-markdown!
  [graphics plot _]
  (let [plot (rewrite-with-symbols plot)]
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
            (decorate-with-current character graphics)
            categories
            :bold #(.withModifier % SGR/BOLD)
            :italic #(.withModifier % SGR/REVERSE)
            :monospace #(.withForegroundColor % TextColor$ANSI/GREEN)
            :code-block #(.withForegroundColor % TextColor$ANSI/GREEN)))))))

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
  (let [theme (LanternaThemes/getRegisteredTheme "staxchg")
        size (.getTerminalSize screen)
        gui (MultiWindowTextGUI.
              (SameTextGUIThread$Factory.)
              screen
              (DefaultWindowManager. size))
        dialog-width (int (* (.getColumns size) 0.8))
        dialog (->
                 (TextInputDialogBuilder.)
                 (.setTitle "")
                 (.setDescription presentation/search-legend)
                 (.setTextBoxSize (TerminalSize. dialog-width 1))
                 (.setExtraWindowHints #{Window$Hint/CENTERED})
                 (.build))]
    (.setTheme gui theme)
    (let [term (.showDialog dialog gui)]
      (dev/log "[query] " (if (some? term) (str "term: '" term "'") "<canceled>"))
      {:function :query!
       :params [term]})))

(defn fetch!
  [url query-params]
  (dev/log "[fetch] url: " url ", query-params: " query-params)
  {:function :fetch!
   :params [(http/request {:url url
                           :method "get"
                           :query-params query-params})]})

(defn read-input
  ""
  [screen
   {:as world :keys [query? search-term]}]
  (cond
    query? (query! screen)
    search-term (fetch! (api/url) (api/query-params search-term))
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
          (when-not (= world-before world-after) (write-output screen world-after))
          (when-not (world-after :quit?) (recur world-after)))))
    (.stopScreen screen)))

