(ns staxchg.presentation.recipe
  (:require [clojure.string :as string]
            [staxchg.presentation.common :refer :all])
  (:import [com.googlecode.lanterna SGR Symbols TerminalTextUtils TextCharacter
                                    TextColor$ANSI])
  (:gen-class))

(defn printable?
  "False if lantera throws an exception when given character, true otherwise."
  [character]
  (->> character
       ((juxt #(TerminalTextUtils/isControlCharacter %)
              #(Character/isHighSurrogate %)))
       (every? false?)))

(defn transform-character
  "Replaces the character c within item with (f c ts), where ts are its traits.
   Assumes item is a plot item."
  [item f]
  (update item 0 f (get-in item [2 :traits])))

(defn replace-with-symbols
  "Replaces the character within item with its lanterna symbol representation,
   according to its traits."
  [item]
  (transform-character item #(cond (contains? %2 :bullet) Symbols/BULLET
                                   (contains? %2 :horz) horz-bar
                                   :else %1)))

(defn convert-to-lanterna
  "Replaces the character within item with its lanterna object representation."
  [item]
  (transform-character item (fn [character _]
                              (TextCharacter. character))))

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
(def trait-clauses
  "Hash mapping domain-specific concepts to visual effects for text supported
   by lanterna."
  {:strong bold-txt
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
   :meta-reputation bold-txt})

(defn decorate
  "Applies visual effects to the character in item according to its traits and
   the trait-clauses hash."
  [item]
  (transform-character item (fn [character traits]
                              (reduce #((get trait-clauses %2 identity) %1)
                                      character
                                      traits))))

(defmulti groom-item
  "Post-processing hook for presentation logic on plot items."
  first)

(defmethod groom-item :staxchg.io/put-string!
  [item]
  (update item 2 (comp string/join
                       #(filter printable? %))))

(defmethod groom-item :staxchg.io/put-plot!
  [item]
  (update item 2 (partial eduction (comp (filter (comp printable? first))
                                         (map replace-with-symbols)
                                         (map convert-to-lanterna)
                                         (map decorate)))))

(defmethod groom-item :default
  [item]
  item)

(defn groom
  "Post-processing hook for presentation logic on plots."
  [recipe]
  (map groom-item recipe))

(def refresh
  "Recipe for refreshing the lanterna screen."
  [[:staxchg.io/refresh! :screen]])

