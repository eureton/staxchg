(ns staxchg.markdown-test
  (:require [clojure.test :refer :all]
            [staxchg.markdown :refer :all]))

(deftest parse-test
  (testing "preformatted: surroundings"
    (are [prefix suffix] (let [block "    (defn double [x]\r\n      (* x 2))"
                               string (str prefix block suffix)
                               result (->> string parse :preformatted)
                               single? #(->> % count (= 1))
                               block? #(->> % first (apply subs string) clojure.string/trimr (= block))]
                           ((every-pred not-empty single? block?) result))
         "" ""
         "# xyz\r\n" ""
         "" "\r\n# xyz"))
  (testing "preformatted: match"
    (are [string] (->> string parse :preformatted not-empty)
         "    (def x 42)"
         "    (def x 42)\r\n"))
  (testing "preformatted: no match"
    (are [string] (->> string parse :preformatted empty?)
         "   (def x 42)"
         "!    (def x 42)")))

(deftest plot-test
  (let [s "123\r\n456\r\n789"
        opts {:left 10 :top 20 :width 100 :height 100}
        res #((plot s %) :plotted)
        xys #(->> (res %) (map second))
        xs #(map first (xys %))
        ys #(map second (xys %))]
    (testing ":left"
      (is (= (xs opts) [10 11 12 10 11 12 10 11 12])))
    (testing ":top"
      (is (= (ys opts) [20 20 20 21 21 21 22 22 22])))
    (testing ":x"
      (let [opts (assoc opts :x 5)]
        (is (= (xs opts) [15 16 17 10 11 12 10 11 12]))))
    (testing ":y"
      (let [opts (assoc opts :y 7)]
        (is (= (ys opts) [27 27 27 28 28 28 29 29 29]))))))

           ;[[\1 [5 10]] [\2 [6 10]] [\3 [7 10]]
            ;[\4 [0 11]] [\5 [1 11]] [\6 [2 11]]
            ;[\7 [0 12]] [\8 [1 12]] [\9 [2 12]]])))))
