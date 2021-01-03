(ns staxchg.plot
  (:gen-class))

(def zero [])

(defn make
  ""
  [{:keys [type x y payload foreground-color modifiers]
    :viewport/keys [left top width height]
    :or {type :string
         x 0
         y 0
         left 0
         top 0
         width 0
         height 0
         payload ""
         modifiers []}}]
  [{:type type
    :x x
    :y y
    :viewport/left left
    :viewport/top top
    :viewport/width width
    :viewport/height height
    :payload payload
    :foreground-color foreground-color
    :modifiers modifiers}])

(defn add
  ""
  [x y & more]
  (apply concat x y more))

