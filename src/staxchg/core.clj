(ns staxchg.core
  (:require [clojure.string :as string])
  (:require [clj-http.client :as http])
  (:require [cheshire.core :as ccore])
  (:require [lanterna.terminal :refer :all])
  (:require [lanterna.screen :as screen])
  (:gen-class))

(defn search-url [tags]
  (let [base "https://api.stackexchange.com/"
        version "2.2"
        max-results 4
        attrs "!)E0g*O9qmR7mgq0(5iVWTHkwn2yFKq_EwjW)I5Su.G9cSaPur" ; \wo comments
        ;attrs "!-lBu6t1YOC3HpC*6dJNyfvSSv(GQOGATA*mdkXVYDlLgpetXIdvh(z" ; \w comments
        site "stackoverflow"
        tagged (string/join \; tags)]
    (string/join
      [base
       version
       "/search?page=1&pagesize="
       max-results
       "&order=desc&sort=activity&site="
       site
       "&tagged="
       tagged
       "&filter="
       attrs])))

(defn fetch
  []
  (let [response (http/get (search-url ["clojure"]))
        items (->> response :body ccore/parse-string)]
    items))

; sample response for testing purposes
(def items 
{"items" [{"question_id" 65245079,
           "owner" {"reputation" 1170, "display_name" "madeinQuant"},
           "score" 0,
           "view_count" 45,
           "tags" ["python" "clojure" "least-squares"],
           "last_activity_date" 1607675799,
           "answers" [{"owner" {"reputation" 5194, "display_name" "Gwang-Jin Kim"},
                       "is_accepted" false,
                       "score" 0,
                       "last_activity_date" 1607675799,
                       "body_markdown" "Least square method is also called linear regression.\r\nIn Python you use `numpy` and `scikitlearn` (`sklearn`).\r\nIn Cloure, you can use `incanter`.\r\n\r\n## Python linear regression\r\n```\r\nimport numpy as np\r\nfrom sklearn.linear_model import LinearRegression\r\n\r\nx = np.array([1, 2, 3, 4, 5]).reshape((-1, 1))\r\ny = np.array([5, 9 11, 20, 24])\r\nmodel = LinearRegression().fit(x, y)\r\nr_sq = model.score(x, y) ## 0.9573365231259968\r\n(model.intercept_, model.coef_[0])\r\n# (-0.8999999999999986, 4.8999999999999995)\r\n```\r\n## Clojure linear regression\r\n```\r\n;; I install with:\r\n;; $ lein try incanter &quot;1.9.3&quot;\r\n\r\n;; basic linear regression with `incanter`\r\n(ns linreg\r\n  (:use [incanter.charts :only [histogram scatter-plot pie-chart add-points add-lines]]\r\n        [incanter.core :only [view]]\r\n        [incanter.stats :only [sample-normal linear-model]]))\r\n\r\n(def x [1 2 3 4 5])\r\n(def y [5 9 11 20 24])\r\n;; linear regression\r\n(def model (linear-model y x))\r\n;; this plots the regression\r\n(view (add-lines (scatter-plot x y) \r\n                 x (:fitted model)))\r\n\r\n(:coefs model)\r\n;; =&gt; (-0.8999999999999915 4.900000000000002)\r\n(:r-square model)\r\n;; =&gt; 0.9573365231259969\r\n```\r\n[![This is the plot incanter plotted][1]][1]\r\n\r\n  [1]: https://i.stack.imgur.com/rQ7Cm.png\r\n\r\n`model` contains much more infos:\r\n```\r\n{:y [5 9 11 20 24], \r\n:sse 10.7, \r\n:msr 240.1000000000002, \r\n:design-matrix #vectorz/array [[1.0,1.0],[1.0,2.0],[1.0,3.0],[1.0,4.0],[1.0,5.0]], \r\n:mse 3.5666666666666664, \r\n:t-probs [0.6804197120333224 0.003789007903698405], \r\n:adj-r-square 0.9146730462519939, \r\n:df [1 3], \r\n:coef-var #vectorz/matrix [[3.9233333333333333,-1.0699999999999998],\r\n[-1.0699999999999998,0.3566666666666667]], \r\n:residuals (0.9999999999999893 0.09999999999998721 -2.800000000000015 1.299999999999983 0.3999999999999808), \r\n:ssr 240.1000000000002, \r\n:sst 250.80000000000018, \r\n:coefs (-0.8999999999999915 4.900000000000002), \r\n:f-stat 67.31775700934585, \r\n:r-square 0.9573365231259969, \r\n:f-prob 0.0037890079036982938, \r\n:t-tests [-0.4543754992377534 8.204739911133434], \r\n:x #vectorz/array [[1.0,1.0],[1.0,2.0],[1.0,3.0],[1.0,4.0],[1.0,5.0]], \r\n:std-errors #vectorz/vector [1.980740602232744,0.5972157622389639], \r\n:fitted (4.000000000000011 8.900000000000013 13.800000000000015 18.700000000000017 23.60000000000002), \r\n:coefs-ci ((-7.203539087942197 5.403539087942214) (2.999411453996058 6.800588546003946))}\r\n```"}],
           "title" "Can&#39;t find least square function in Clojure",
           "body_markdown" "Can&#39;t find least square function in Clojure. I try to translate the following python code into Clojure. However, I don&#39;t see any function that is similar in Clojure. Please feel fee to comment how to implement least square in Clojure.\r\n\r\n**problem statement; how to translate into clojure?**\r\n&gt; **p_lsq = leastsq(residuals_func, p_init, args=(x, y))**\r\n\r\n\r\n**List of code (Python)**\r\n\r\n        def fit_func(p, x):\r\n            f = np.poly1d(p)\r\n            return f(x)\r\n    \r\n        def residuals_func(p, x, y):\r\n            ret = fit_func(p, x) - y\r\n            return ret\r\n    \r\n        p_init = np.random.rand(3 + 1)\r\n\r\n        # least square\r\n        p_lsq = leastsq(residuals_func, p_init, args=(x, y))\r\n\r\n        print(&#39;Fitting Parameters:&#39;, p_lsq[0])\r\n\r\n**List of code (Clojure)**\r\n\r\n    (defn horner [coeffs x]\r\n      (reduce #(-&gt; %1 (* x) (+ %2)) (reverse coeffs)))\r\n    \r\n    (defn fit_func [p x]\r\n      (horner p x))\r\n    \r\n    (defn residuals_func [p x y]\r\n      (let [ret (- (fit_func p x) y)]))\r\n    \r\n    (def p_init (take 4 (repeatedly #(rand 0.1))))\r\n    ;; least square\r\n    ;; Can&#39;t find similar function in clojure\r\n    ;; p_lsq = leastsq(residuals_func, p_init, args=(x, y))\r\n    ;;\r\n    (println (first p_lsq))\r\n\r\n\r\n"}
          {"tags" ["clojure" "read-eval-print-loop"],
           "owner" {"reputation" 435, "display_name" "FunkyBaby"},
           "view_count" 16,
           "score" 0,
           "last_activity_date" 1607619739,
           "question_id" 65238963,
           "body_markdown" "For example, when typing `d&lt;TAB&gt;` in the repl, it only shows 10 candidates.  I want it to show all candidates.\r\n[![enter image description here][1]][1]\r\n\r\n\r\n  [1]: https://i.stack.imgur.com/zLNuv.png",
           "title" "How to make rebel-readline show more candiates"}
          {"question_id" 65222000,
           "owner" {"reputation" 143, "display_name" "rutchkiwi"},
           "score" 0,
           "view_count" 48,
           "tags" ["clojure" "timezone" "clj-time"],
           "last_activity_date" 1607615795,
           "answers" [{"owner" {"reputation" 22606, "display_name" "Alan Thompson"},
                       "is_accepted" false,
                       "score" 2,
                       "last_activity_date" 1607615795,
                       "body_markdown" "NOTE:\r\n---\r\n\r\n**The `clj-time` library is based on the old Joda Time library.  Both of these have been deprecated in favor of `java.time` (available since JDK 8).  It is best to use Java interop with java.time instead of the older `clj-time` library.**\r\n\r\nThe package `java.time` is basically Joda Time 2.0, but rewritten to be cleaner and to correct some corner cases.  Both were created by the same person.\r\n\r\n\r\n-----------------\r\nAnswer:\r\n----\r\nIn Java, a `LocalDate` doesn&#39;t have a timezone. That is the whole point.\r\n\r\nIt is used for things like a birthday, such as 1999-12-31, where we don&#39;t consider your birthday to have a timezone.\r\n\r\nFor the same reason, the `LocalDate` doesn&#39;t have any time associated with it, just as your birthday is considered to be the entire day.\r\n\r\nSee the `java.time` docs for more information:  https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/package-summary.html \r\n\r\n------------\r\nFrom the JavaDocs:\r\n\r\n&gt; **Dates and Times** \r\n&gt;\r\n&gt; Instant is essentially a numeric timestamp. The\r\n&gt; current Instant can be retrieved from a Clock. This is useful for\r\n&gt; logging and persistence of a point in time and has in the past been\r\n&gt; associated with storing the result from System.currentTimeMillis().\r\n&gt; \r\n&gt; LocalDate stores a date without a time. This stores a date like\r\n&gt; &#39;2010-12-03&#39; and could be used to store a birthday.\r\n&gt; \r\n&gt; LocalTime stores a time without a date. This stores a time like\r\n&gt; &#39;11:30&#39; and could be used to store an opening or closing time.\r\n&gt; \r\n&gt; LocalDateTime stores a date and time. This stores a date-time like\r\n&gt; &#39;2010-12-03T11:30&#39;.\r\n&gt; \r\n&gt; ZonedDateTime stores a date and time with a time-zone. This is useful\r\n&gt; if you want to perform accurate calculations of dates and times taking\r\n&gt; into account the ZoneId, such as &#39;Europe/Paris&#39;. Where possible, it is\r\n&gt; recommended to use a simpler class without a time-zone. The widespread\r\n&gt; use of time-zones tends to add considerable complexity to an\r\n&gt; application.\r\n\r\n--------\r\n**Helper functions:**\r\n\r\nI have added [some convenience functions][1] to aid the usage of `java.time` that you may find useful.\r\n\r\n\r\n  [1]: https://cljdoc.org/d/tupelo/tupelo/20.12.03b/api/tupelo.java-time"}
                      {"owner" {"reputation" 26697, "display_name" "cfrick"},
                       "is_accepted" true,
                       "score" 1,
                       "last_activity_date" 1607548935,
                       "body_markdown" "There seems not to be a function to get the `LocalDate` from\r\na `DateTime` or alike in `clj-time` -- but *Joda Time* has\r\n`.toLocalDate`.  So e.g. you can do something like this:\r\n\r\n```\r\n(defn date-at-zone\r\n  [instant zone-id]    \r\n  (.toLocalDate                                                                                                                                                         \r\n   (t/to-time-zone                             \r\n    instant                                                                   \r\n    (t/time-zone-for-id zone-id))))                                        \r\n                                                                  \r\n (let [instant (t/date-time 2020 12 12 1)                                  \r\n       london-date (date-at-zone instant &quot;Europe/London&quot;)\r\n       sao-paulo-date (date-at-zone instant &quot;America/Sao_Paulo&quot;)]\r\n   (print (map str [instant london-date sao-paulo-date (t/after? london-date sao-paulo-date)])))       \r\n; → (2020-12-12T01:00:00.000Z 2020-12-12 2020-12-11 true)\r\n```\r\n\r\n(e.g. on 2020-12-12T01:00 Zulu it is still 2020-12-11 in Sao Paulo and\r\ntherefor London &quot;is strictly after&quot; Sao Paulo when looking at just the\r\ndate -- or it&#39;s already &quot;tomorrow&quot; in London)"}
                      {"owner" {"reputation" 194393, "display_name" "Basil Bourque"},
                       "is_accepted" false,
                       "score" 2,
                       "last_activity_date" 1607590601,
                       "body_markdown" "I do not know [Clojure][1]. So I&#39;ll use Java syntax which you can translate.\r\n\r\n&gt;I&#39;m sitting in the America/Sao_Paulo (or some other random timezone) \r\n\r\nIrrelevant. Whether your tushy is in Tokyo, Reykjav&#237;k, or S&#227;o Paulo, that has nothing to do with asking for the current date in `Europe/London` time zone.\r\n\r\n&gt;and now I want to get a LocalDate for the current date in Europe/London.\r\n\r\nCall [`LocalDate.now`][2] while passing the time zone of interest.\r\n\r\n    ZoneId zoneId = ZoneId.of( &quot;Europe/London&quot; ) ;\r\n    LocalDate currentDateInLondon = LocalDate.now( zoneId ) ;\r\n\r\n&gt;I&#39;ve got a localDate that I know is in london, and I want to check if it&#39;s before the current date\r\n\r\nYou can compare [`LocalDate`][3] objects by calling `isEqual`, `isBefore`, and `isAfter`. \r\n\r\n    LocalDate localDate = LocalDate.of( 2021 , Month.JANUARY , 23 ) ;\r\n    boolean isCurrentDateInLondonBeforeThatDate = currentDateInLondon.isBefore( localDate ) ;\r\n\r\n\r\n  [1]: https://en.wikipedia.org/wiki/Clojure\r\n  [2]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/LocalDate.html#now(java.time.ZoneId)\r\n  [3]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/LocalDate.html"}],
           "title" "clj-time - how to get local-date from curent time in a different timezone",
           "body_markdown" "I&#39;m sitting in the America/Sao_Paulo (or some other random timezone) and now I want to get a LocalDate for the current date in Europe/London.\r\n\r\nHow can I achieve this?\r\n\r\n(I&#39;ve got a localDate that I know is in london, and I want to check if it&#39;s before the current date using clj-time.core/after?)"}
          {"question_id" 62684264,
           "owner" {"reputation" 306, "display_name" "newBieDev"},
           "score" 3,
           "view_count" 269,
           "tags" ["clojure" "functional-programming" "diamond-problem"],
           "last_activity_date" 1607613809,
           "answers" [{"owner" {"reputation" 3917, "display_name" "Sean Corfield"},
                       "is_accepted" false,
                       "score" 3,
                       "last_activity_date" 1593639763,
                       "body_markdown" "This flowchart is still good advice, nine years later: https://cemerick.com/2011/07/05/flowchart-for-choosing-the-right-clojure-type-definition-form/\r\n\r\nMy rule of thumb is: always use plain hash maps until you really need polymorphism and then decide whether you want multi-methods (dispatch on one or more arguments/attributes) or protocols (dispatch on just the type)."}
                      {"owner" {"reputation" 22606, "display_name" "Alan Thompson"},
                       "is_accepted" false,
                       "score" 1,
                       "last_activity_date" 1593643524,
                       "body_markdown" "To Sean&#39;s excellent answer, I would only add that records can slow down iterative development, especially using a tool like `lein-test-refresh` or similar. \r\n\r\nRecords form a separate Java class, and must be recompiled upon every change, which can slow down the iteration cycle.\r\n\r\nIn addition, recompilation breaks comparison with still-existing record objects, since the recompiled object (even if there are not changes!) will not be `=` to the original since it has a different class file.  As an example, suppose you have a `Point` record:\r\n\r\n    (defrecord Point [x y])\r\n    \r\n    (def p (-&gt;Point 1 2)) ; in file ppp.clj\r\n    (def q (-&gt;Point 1 2)) ; in file qqq.clj\r\n    \r\n    (is (= p q)) ; in a unit test\r\n\r\nIf file `ppp.clj` gets recompiled, it generates a new `Point` class with a different &quot;ID&quot; value than before.  Since records must have the same type AND values to be considered equal, the unit test will fail even though both are of type `Point` and both have values `[1 2]`.  This is an unintended pain point when using records.\r\n\r\n\r\n"}],
           "title" "Clojure - What&#39;s the benefit of Using Records Over Maps",
           "body_markdown" "I&#39;m having a hard time deciding when using ````defrecord```` is the right choice and more broadly if my use of protocols on my records is semantic clojure and functional. \r\n\r\nIn my current project I&#39;m building a game that has different types of enemies that all have the same set of actions, where those actions might be implemented differently. \r\n\r\nComing from an OOP background, I&#39;m tempted to do something like: \r\n\r\n````\r\n(defprotocol Enemy\r\n  &quot;Defines base function of an Enemy&quot;\r\n  (attack [this] &quot;attack function&quot;))\r\n\r\n(extend-protocol Enemy\r\n  Orc\r\n  (attack [_] &quot;Handles an orc attack&quot;)  \r\n  Troll\r\n  (attack [_] &quot;Handles a Troll attack&quot;))\r\n\r\n\r\n\r\n(defrecord Orc [health attackPower defense])\r\n(defrecord Troll [health attackPower defense])\r\n\r\n(def enemy (Orc. 1 20 3))\r\n(def enemy2 (Troll. 1 20 3))\r\n\r\n(println (attack enemy))\r\n; handles an orc attack\r\n\r\n(println (attack enemy2))\r\n;handles a troll attack\r\n\r\n````\r\n\r\nThis looks like it makes sense on the surface. I want every enemy to always have an attack method, but the actual implementation of that should be able to vary on the particular enemy. Using the ````extend-protocol```` I&#39;m able to create efficient dispatch of the methods that vary on my enemies, and I can easily add new enemy types as well as change the functionally on those types. \r\n\r\nThe problem I&#39;m having is why should I use a record over a generic map? The above feels a bit to OOP to me, and seems like I&#39;m going against a more functional style. So, my question is broken into two:\r\n\r\n 1. Is my above implementation of records and protocols a sound use case?\r\n 2. More generically, when is a record preferred over a map? I&#39;ve read you should favor records when you&#39;re re-building the same map multiple times (as I would be in this case). Is that logic sound? \r\n\r\n"}],
 "has_more" true,
 "quota_max" 300,
 "quota_remaining" 204})

(defn plot
  [s [cx cy] {:as args :keys [left top width height] :or {left 0 top 0}}]
  (if (or (empty? s) (>= cy (+ top height)))
    '()
    (let [head (first s)
          tail (rest s)
          tail-plot (cond
                      (= \return head) (plot tail [left cy] args)
                      (= \newline head) (plot tail [cx (+ cy 1)] args)
                      (>= (+ cx 1) (+ left width)) (plot tail [left (+ cy 1)] args)
                      :else (plot tail [(+ cx 1) cy] args))]
      (conj tail-plot [cx cy]))))

(defn clear-box
  [terminal {:as args :keys [left top width height] :or {left 0 top 0}}]
  (let [blank (string/join (repeat width \space))]
    (dotimes [y height]
      (put-string terminal blank left (+ top y)))))

(defn put-boxed-string
  [terminal s {:as args :keys [left top width height line-offset] :or {left 0 top 0 line-offset 0}}]
  (let [line-count (->> s string/split-lines count)
        boxed-offset (max 0 (min line-offset (- line-count 1)))
        offset-s (->> s string/split-lines (drop boxed-offset) (string/join "\r\n"))
        s-seq (seq offset-s)
        s-plot (plot s-seq [left top] args)]
    (clear-box terminal args)
    (doseq [[c [x y]] (map vector s-seq s-plot)]
      (put-character terminal c x y))))

(defn render-question-list [terminal world]
  (doseq [[i q] (map-indexed vector (world :questions))]
    (move-cursor terminal 1 (+ i 1))
    (put-string terminal (q "title"))))

(defn selected-line-offset [world]
  ((world :line-offsets) (get-in (world :questions) [(world :selected-question) "question_id"])))

(defn render-selected-question [terminal world]
  (put-boxed-string
    terminal
    (get-in world [:questions (world :selected-question) "body_markdown"])
    {:left 0 :top 6 :width 118 :height 30 :line-offset (selected-line-offset world)}))

(defn render [terminal world]
  (render-question-list terminal world)
  (render-selected-question terminal world))

(defn update-world [world keycode]
  (let [selected-question (world :selected-question)
        question_id (get-in world [:questions selected-question "question_id"])]
    (case keycode
      \k (update-in world [:line-offsets question_id] #(max 0 (dec %)))
      \j (update-in world [:line-offsets question_id] inc)
      \K (assoc world :selected-question (max 0 (dec selected-question)))
      \J (assoc world :selected-question (inc selected-question))
      world)))

(defn initialize-world [items]
  (let [questions (items "items")]
    {:line-offsets (->> questions (map #(% "question_id")) (reduce #(assoc %1 %2 0) {}))
     :selected-question 0
     :questions questions}))

(defn test-printing [terminal]
  (in-terminal
    terminal
    (let [multi-liner "_234567890\r\n_bcdefghij\r\n1_34567890\r\na_cdefghij\r\n12_4567890\r\nab_defghij\r\n123_567890\r\nabc_efghij\r\n"
          [cols rows] (lanterna.terminal/get-size terminal)
          scr (lanterna.screen/get-screen :auto {:cols cols :rows rows})]
      (put-string terminal (str "TERMINAL SIZE: " [cols rows]) 60 0)
      (put-string terminal (str "  SCREEN SIZE: " (lanterna.screen/get-size scr)) 60 1)
      (put-string terminal multi-liner 1 1)
      (put-boxed-string terminal multi-liner {:left 12 :top 12 :width 8 :height 31})
      (get-key-blocking terminal))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [terminal (get-terminal)]
    ;(test-printing terminal)
    (in-terminal
      terminal
      (loop [world-before (initialize-world items)]
        (let [keycode (get-key-blocking terminal)
              world-after (update-world world-before keycode)]
          (render terminal world-after)
          (when-not (= keycode \q) (recur world-after)))))))

