(ns staxchg.api-test
  (:require [clojure.test :refer :all]
            [staxchg.api :refer :all]))

(deftest query-params-test
  (testing "when no tags, no :tagged key out"
    (is (not (contains? (query-params "clojure repl driven development") :tagged))))
  (testing "when tags, :tagged key exists"
    (is (contains? (query-params "[clojure] repl driven development") :tagged)))
  (testing "colon-separated list of tags in :tagged"
    (is (=
         ((query-params "[clojure] lorem [repl] driven [development]") :tagged)
         "clojure;repl;development"))))

