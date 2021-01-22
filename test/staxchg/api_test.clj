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
         "clojure;repl;development")))
  (testing "surrounded by whitespace"
    (are [term] (= ((query-params term) :tagged) "tag")
         "abc [tag] xyz"
         "abc\t[tag] xyz"
         "abc [tag]\txyz"
         "abc\t[tag]\txyz"))
  (testing "surrounded by boundaries"
    (are [term] (= ((query-params term) :tagged) "tag")
         "[tag] xyz"
         "abc [tag]"
         "[tag]"))
  (testing "surrounded by neither whitespace nor boundaries"
    (are [term] (not (contains? (query-params term) :tagged))
         "abc[tag]xyz"
         "abc [tag]xyz"
         "abc[tag] xyz"
         "abc[tag]"
         "[tag]xyz"))
  (testing "when no users, no :user key out"
    (is (not (contains? (query-params "clojure repl driven development") :user))))
  (testing "when users, :user key exists"
    (are [term] (contains? (query-params term) :user)
         "clojure repl user:1234 development"
         "user:1234 development"))
  (testing "first user in :user, the rest ignored"
    (is (=
         ((query-params "clojure user:1234 repl user:4321 development") :user)
         "1234")))
  (testing "text ending with 'user' is ignored"
    (is (not (contains? (query-params "yuser:1234 repl") :user))))
  )

