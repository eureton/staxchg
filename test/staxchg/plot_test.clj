(ns staxchg.plot-test
  (:require [clojure.test :refer :all]
            [staxchg.plot :refer :all]))

(deftest map-sub-test
  (let [plot [[\a [0 0] {:traits #{}}]
              [\b [0 1] {:traits #{}}]
              [\c [0 2] {:traits #{}}]
              [\d [0 3] {:traits #{}}]
              [\e [0 4] {:traits #{}}]
              [\f [0 5] {:traits #{}}]]
        f (fn [x & args]
            (map #(update-in % [2 :traits] clojure.set/union (set args)) x))]
    (testing "maps"
      (is (= (map-sub plot 2 4 f :x :y) [[\a [0 0] {:traits #{}}]
                                         [\b [0 1] {:traits #{}}]
                                         [\c [0 2] {:traits #{:x :y}}]
                                         [\d [0 3] {:traits #{:x :y}}]
                                         [\e [0 4] {:traits #{}}]
                                         [\f [0 5] {:traits #{}}]])))
    (testing "end index out of bounds"
      (is (= (map-sub plot 3 9 f :x :y) [[\a [0 0] {:traits #{}}]
                                         [\b [0 1] {:traits #{}}]
                                         [\c [0 2] {:traits #{}}]
                                         [\d [0 3] {:traits #{:x :y}}]
                                         [\e [0 4] {:traits #{:x :y}}]
                                         [\f [0 5] {:traits #{:x :y}}]])))
    (testing "start index out of bounds"
      (is (= (map-sub plot 8 9 f :x :y) [[\a [0 0] {:traits #{}}]
                                         [\b [0 1] {:traits #{}}]
                                         [\c [0 2] {:traits #{}}]
                                         [\d [0 3] {:traits #{}}]
                                         [\e [0 4] {:traits #{}}]
                                         [\f [0 5] {:traits #{}}]])))))

