(ns staxchg.flow-test
  (:require [clojure.test :refer :all]
            [staxchg.flow :refer :all]))

(deftest match?-test
  (let [flow {:viewport/left (rand-int 100)
              :viewport/top (rand-int 100)
              :viewport/width (rand-int 100)
              :viewport/height (rand-int 100)}]
    (testing "zero to zero"
      (is (match? zero zero)))
    (testing "on itself"
      (is (match? flow flow)))
    (testing "irrelevant keys"
      (is (match? flow (assoc flow :key (rand-int 100)))))
    (testing "relevant keys modified: left"
      (is (not (match? flow (update flow :viewport/left inc)))))
    (testing "relevant keys modified: top"
      (is (not (match? flow (update flow :viewport/top inc)))))
    (testing "relevant keys modified: width"
      (is (not (match? flow (update flow :viewport/width inc)))))
    (testing "relevant keys modified: height"
      (is (not (match? flow (update flow :viewport/height inc)))))
    (testing "relevant keys missing left"
      (is (not (match? flow (dissoc flow :viewport/left)))))
    (testing "relevant keys missing top"
      (is (not (match? flow (dissoc flow :viewport/top)))))
    (testing "relevant keys missing width"
      (is (not (match? flow (dissoc flow :viewport/width)))))
    (testing "relevant keys missing height"
      (is (not (match? flow (dissoc flow :viewport/height)))))))

(deftest reframe-test
  (let [inner-keys [:inner/left :inner/top :inner/width :inner/height]
        viewport {:viewport/left (rand-int 100)
                  :viewport/top (rand-int 100)
                  :viewport/width (rand-int 100)
                  :viewport/height (rand-int 100)}
        master viewport]
    (testing "matching"
      (let [flow (merge viewport {:items [{} {}]})
            res (reframe master flow)
            inner (-> res first (select-keys inner-keys))]
        (is (empty? inner))))))

