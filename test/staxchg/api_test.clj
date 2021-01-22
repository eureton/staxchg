(ns staxchg.api-test
  (:require [clojure.test :refer :all]
            [staxchg.api :refer :all]))

(deftest query-params-test
  ; tag
  (testing "when no tags, no :tagged key out"
    (is (not (contains? (query-params "clojure repl driven development") :tagged))))
  (testing "when tags, :tagged key exists"
    (is (contains? (query-params "[clojure] repl driven development") :tagged)))
  (testing "colon-separated list of tags in :tagged"
    (is (=
         ((query-params "[clojure] lorem [repl] driven [development]") :tagged)
         "clojure;repl;development")))
  (testing "tag: surrounded by whitespace"
    (are [term] (= ((query-params term) :tagged) "tag")
         "abc [tag] xyz"
         "abc\t[tag] xyz"
         "abc [tag]\txyz"
         "abc\t[tag]\txyz"))
  (testing "tag: surrounded by boundaries"
    (are [term] (= ((query-params term) :tagged) "tag")
         "[tag] xyz"
         "abc [tag]"
         "[tag]"))
  (testing "tag: surrounded by neither whitespace nor boundaries"
    (are [term] (not (contains? (query-params term) :tagged))
         "abc[tag]xyz"
         "abc [tag]xyz"
         "abc[tag] xyz"
         "abc[tag]"
         "[tag]xyz"))

  ; user
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

  ; isaccepted
  (testing "isaccepted: yes"
    (is (= ((query-params "abc isaccepted:yes xyz") :accepted) "yes")))
  (testing "isaccepted: no"
    (is (= ((query-params "abc isaccepted:no xyz") :accepted) "no")))
  (testing "isaccepted: valid values => key exists"
    (are [term] (contains? (query-params term) :accepted)
         "abc isaccepted:yes xyz"
         "abc isaccepted:no xyz"))
  (testing "isaccepted: invalid values => no key"
    (are [term] (not (contains? (query-params term) :accepted))
         "abc isaccepted:yess xyz"
         "abc isaccepted:nno xyz"))
  (testing "isaccepted: surrounded by whitespace"
    (are [term] (contains? (query-params term) :accepted)
         "abc isaccepted:yes xyz"
         "abc\tisaccepted:yes xyz"
         "abc isaccepted:yes\txyz"
         "abc\tisaccepted:yes\txyz"))
  (testing "isaccepted: first wins"
    (is (= ((query-params "isaccepted:no klm isaccepted:yes") :accepted) "no")))
  
  ; q
  (testing "q: removes"
    (are [term] (= ((query-params term) :q) "abc xyz")
         "abc [tag] xyz"
         "[tag] abc xyz"
         "abc xyz [tag]"
         "abc user:1234 xyz"
         "user:1234 abc xyz"
         "abc xyz user:1234"
         "abc isaccepted:yes xyz"
         "isaccepted:yes abc xyz"
         "abc xyz isaccepted:yes"
         "[tag] abc user:1234 xyz isaccepted:yes"))
  (testing "q: no value => no key"
    (are [term] (not (contains? (query-params term) :q))
         "[tag]"
         "user:1234"
         "isaccepted:yes"
         "")))

