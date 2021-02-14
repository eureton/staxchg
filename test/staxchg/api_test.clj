(ns staxchg.api-test
  (:require [clojure.test :refer :all]
            [staxchg.api :refer :all]))

(deftest query-params-test
  ; general
  (testing "delimitation"
    (are [expr param value] (let [terms [expr
                                         (str "xyz " expr)
                                         (str expr " xyz")
                                         (str "xyz\t" expr)
                                         (str expr "\txyz")
                                         (str "xyz " expr " abc")
                                         (str "xyz " expr "\tabc")
                                         (str "xyz\t" expr " abc")
                                         (str "xyz\t" expr "\tabc")]]
                              (->>
                                terms
                                (map query-params)
                                (map param)
                                (every? (partial = value))))
         "[tag]"          :tagged   "tag"
         "user:1234"      :user     "1234"
         "isaccepted:yes" :accepted "yes"
         "score:3"        :sort     "votes"
         "score:3"        :min      "3"))

  ; tag
  (testing "when no tags, no :tagged key out"
    (is (not (contains? (query-params "clojure repl driven development") :tagged))))
  (testing "when tags, :tagged key exists"
    (is (contains? (query-params "[clojure] repl driven development") :tagged)))
  (testing "colon-separated list of tags in :tagged"
    (is (=
         ((query-params "[clojure] lorem [repl] driven [development]") :tagged)
         "clojure;repl;development")))

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
  (testing "user: invalid values => no key"
    (are [term] (not (contains? (query-params term) :user))
         "abc user:x123 xyz"
         "abc user:123x xyz"
         "abc uuser:123 xyz"))

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
  (testing "isaccepted: first wins"
    (is (= ((query-params "isaccepted:no klm isaccepted:yes") :accepted) "no")))
  
  ; score
  (testing "score: valid values => keys set"
    (are [k v] (= ((query-params "abc score:3 xyz") k) v)
         :sort "votes"
         :min "3"))
  (testing "score: invalid values => no key"
    (are [term] (let [params (query-params term)]
                  (and (not= (params :sort) "votes")
                       (not (contains? params :min))))
         "abc score:x3 xyz"
         "abc score:3x xyz"
         "abc sscore:3 xyz"))

  ; exact
  (testing "exact: valid values => keys set"
    (is (= ((query-params "abc \"klm\" xyz") :title) "klm")))
  (testing "exact: first wins"
    (is (= ((query-params "\"abc\" klm \"xyz\"") :title) "abc")))

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
         "abc score:3 xyz"
         "score:3 abc xyz"
         "abc xyz score:3"
         "[tag] abc user:1234 score:3 xyz isaccepted:yes"))
  (testing "q: no value => no key"
    (are [term] (not (contains? (query-params term) :q))
         "[tag]"
         "user:1234"
         "isaccepted:yes"
         "")))

