(ns staxchg.presentation.recipe
  (:require [clojure.string :as string])
  (:require [staxchg.presentation.common :refer :all])
  (:require [staxchg.dev :as dev])
  (:require [staxchg.markdown :as markdown])
  (:import com.googlecode.lanterna.SGR)
  (:import com.googlecode.lanterna.Symbols)
  (:import com.googlecode.lanterna.TerminalTextUtils)
  (:import com.googlecode.lanterna.TextCharacter)
  (:import com.googlecode.lanterna.TextColor$ANSI)
  (:gen-class))

(defn printable?
  ""
  [character]
  (->>
    character
    ((juxt #(TerminalTextUtils/isControlCharacter %)
           #(Character/isHighSurrogate %)))
    (every? false?)))

(defn replace-with-symbols
  ""
  [[character xy {:keys [traits] :as extras}]]
  (let [rewritten (cond
                    (contains? traits :bullet) Symbols/BULLET
                    (contains? traits :horz) horz-bar
                    :else character)]
    [rewritten xy extras]))

(defn convert-to-lanterna
  ""
  [plot-item]
  (update plot-item 0 #(TextCharacter. %)))

; helper variables for composing lanterna characters
(def bold-txt #(.withModifier % SGR/BOLD))
(def reverse-txt #(.withModifier % SGR/REVERSE))
(def green-txt #(.withForegroundColor % TextColor$ANSI/GREEN))
(def cyan-txt #(.withForegroundColor % TextColor$ANSI/CYAN))
(def red-txt #(.withForegroundColor % TextColor$ANSI/RED))
(def white-txt #(.withForegroundColor % TextColor$ANSI/WHITE))
(def magenta-txt #(.withForegroundColor % TextColor$ANSI/MAGENTA))
(def yellow-txt #(.withForegroundColor % TextColor$ANSI/YELLOW))
(def frame-txt #(.withForegroundColor % frame-color))
(def blue-txt #(.withForegroundColor % TextColor$ANSI/BLUE))
(def trait-clauses [:strong bold-txt
                    :em reverse-txt
                    :code green-txt
                    :standout green-txt
                    :hilite-comment cyan-txt
                    :hilite-built-in red-txt
                    :hilite-function (comp bold-txt white-txt)
                    :hilite-documentation cyan-txt
                    :hilite-keyword (comp bold-txt green-txt)
                    :hilite-data-type (comp bold-txt white-txt)
                    :hilite-dec-val (comp bold-txt magenta-txt)
                    :hilite-base-n (comp bold-txt magenta-txt)
                    :hilite-float (comp bold-txt magenta-txt)
                    :hilite-constant (comp bold-txt magenta-txt)
                    :hilite-char (comp bold-txt yellow-txt)
                    :hilite-special-char (comp bold-txt red-txt)
                    :hilite-string (comp bold-txt green-txt)
                    :hilite-verbatim-string (comp bold-txt green-txt)
                    :hilite-special-string (comp bold-txt green-txt)
                    :hilite-import red-txt
                    :hilite-annotation yellow-txt
                    :hilite-comment-var (comp bold-txt white-txt)
                    :hilite-other blue-txt
                    :hilite-variable white-txt
                    :hilite-control-flow red-txt
                    :hilite-operator yellow-txt
                    :hilite-extension blue-txt
                    :hilite-preprocessor red-txt
                    :hilite-attribute (comp bold-txt blue-txt)
                    :hilite-region-marker yellow-txt
                    :hilite-information (comp bold-txt green-txt)
                    :hilite-warning (comp bold-txt yellow-txt)
                    :hilite-alert (comp bold-txt red-txt)
                    :hilite-error (comp bold-txt red-txt)
                    :comment cyan-txt
                    :h (comp bold-txt yellow-txt)
                    :frame frame-txt
                    :meta-answers (comp bold-txt frame-txt)
                    :meta-score (comp bold-txt green-txt)
                    :meta-views (comp bold-txt white-txt)
                    :meta-reputation bold-txt])

(defn apply-traits
  ""
  [[character xy {:keys [traits] :as extras}]]
  [(apply markdown/decorate character traits trait-clauses)
   xy
   extras])

(def string-groomer (comp string/join
                          #(filter printable? %)))

(def plot-groomer (partial eduction (comp (filter (comp printable? first))
                                          (map replace-with-symbols)
                                          (map convert-to-lanterna)
                                          (map apply-traits))))

(defmulti groom-item :function)

(defmethod groom-item :staxchg.io/put-string!
  [item]
  (update-in item [:params 1] string-groomer))

(defmethod groom-item :staxchg.io/put-plot!
  [item]
  (update-in item [:params 1] plot-groomer))

(defmethod groom-item :default
  [item]
  item)

(defn groom
  ""
  [recipe]
  (map groom-item recipe))

(def refresh [{:function :staxchg.io/refresh!
               :params [:screen]}])

