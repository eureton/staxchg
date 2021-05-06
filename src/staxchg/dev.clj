(ns staxchg.dev
  (:require [clojure.string :as string])
  (:require [staxchg.util :as util])
  (:gen-class))

(defn truncate
  [x]
  (if-let [_ (string? x)]
    (let [length (count x)
          truncation-limit 128
          context 32]
      (if (> length truncation-limit)
        (str (subs x 0 context) " [...] " (subs x (- length context) length))
        x))
    x))

(defn log
  [& items]
  (when-let [pathname (util/config-hash "LOGFILE")]
    (with-open [writer (clojure.java.io/writer pathname :append true)]
      (.write writer (str (apply str (map truncate items)) "\n")))))

(defmulti log-recipe-step :function)

(defmethod log-recipe-step :staxchg.io/put-markdown!
  [{[_ plot _] :params}]
  (log "[put-markdown] " (->> (map second plot) (take 10) (apply str))
       " |>" (string/join (map (comp #(.getCharacter %) first) plot)) "<|"))

(defmethod log-recipe-step :staxchg.io/put-string!
  [{[_ string {:keys [x y]}] :params}]
  (log "[put-string] " [x y] " |>"  string "<|"))

(defmethod log-recipe-step :staxchg.io/scroll!
  [{[_ top bottom distance] :params}]
  (log "[scroll] at [" top " " bottom "] by " distance))

(defmethod log-recipe-step :staxchg.io/clear!
  [{[graphics left top width height] :params}]
  (log "[clear] rect [" width "x" height "] at [" left "x" top "]"))

(defmethod log-recipe-step :staxchg.io/refresh!
  [_]
  (log "[refresh]"))

(defmethod log-recipe-step :staxchg.io/read-key!
  [_]
  (log "[read-key]"))

(defmethod log-recipe-step :staxchg.io/query!
  [_]
  (log "[query]"))

(defmethod log-recipe-step :staxchg.io/fetch-questions!
  [{[_ url query-params] :params}]
  (log "[fetch-questions] url: " url ", query-params: " query-params))

(defmethod log-recipe-step :staxchg.io/fetch-answers!
  [{[_ url query-params question-id] :params}]
  (log "[fetch-answers] url: " url ", "
       "query-params: " query-params ", "
       "question-id: " question-id))

(defmethod log-recipe-step :staxchg.io/highlight-code!
  [{[code syntax question-id] :params}]
  (log "[highlight-code] BEGIN syntax: " syntax ", "
       "question-id: " question-id "\r\n" code
       "[highlight-code] END"))

(defmethod log-recipe-step :staxchg.io/quit!
  [_]
  (log "[quit]"))

(defmethod log-recipe-step :staxchg.io/register-theme!
  [{[theme-name filename] :params}]
  (log "[register-theme] name: " theme-name ", filename: " filename))

(defmethod log-recipe-step :staxchg.io/acquire-screen!
  [_]
  (log "[acquire-screen]"))

(defmethod log-recipe-step :staxchg.io/enable-screen!
  [_]
  (log "[enable-screen]"))

(defmethod log-recipe-step :default [_])

(defn log-request
  ""
  [{:keys [recipes]}]
  (log " /^^^ " (count recipes) " recipe(s)")
  (run! log (map
              (fn [r] (str "|----- " (string/join ", " (map :function r))))
              recipes))
  (log " \\___ Complete")
  (run! log-recipe-step (flatten recipes)))

(def response-body
{
  "items" [{"tags" ["javascript" "haskell" "functional-programming" "monads"]
           "owner" {"user_type" "does_not_exist" "display_name" "user10583507"}
           "is_answered" true
           "view_count" 817
           "answer_count" 4
           "score" -3
           "last_activity_date" 1541181915
           "creation_date" 1541109222
           "last_edit_date" 1592644375
           "question_id" 53109889
           "body_markdown" "The related Question is \n\n1. https://stackoverflow.com/questions/16439025/what-is-so-special-about-monads \n\n2. https://stackoverflow.com/questions/51376391/bind-can-be-composed-of-fmap-and-join-so-do-we-have-to-use-monadic-functions-a\n\n**In the first question:**\n\n&gt;What is so special about Monads?\n&gt;\n&gt;A monad is a mathematical structure which is heavily used in (pure) functional programming, basically Haskell. However, there are many other mathematical structures available, like for example applicative functors, strong monads, or monoids. Some have more specific, some are more generic. Yet, monads are much more popular. Why is that?\n\nThe comment to reply the question:\n\n&gt;As far as I recall, monads were popularised by Wadler, and at the time the idea of doing IO without tedious CPS and parsing without explicit state passing were huge selling points; it was a hugely exciting time. A.F.A.I.R., Haskell didn&#39;t do constructor classes, but Gofer (father of Hugs) did. Wadler proposed overloading list comprehension for monads, so the do notation came later. Once IO was monadic, monads became a big deal for beginners, cementing them as a major thing to grok. Applicatives are much nicer when you can, and Arrows more general, but they came later, and IO sells monads hard. – AndrewC May 9 &#39;13 at 1:34\n\nThe answer by @Conal is:\n\n&gt;I suspect that the disproportionately large attention given to this one particular type class (`Monad`) over the many others is mainly a historical fluke. People often associate `IO` with `Monad`, although the two are independently useful ideas ([as are list reversal and bananas](https://dl.dropboxusercontent.com/u/7810909/docs/what-does-monad-mean/what-does-monad-mean/chunk-html/ar01s02s02s02.html)). Because `IO` is magical (having an implementation but no denotation) and `Monad` is often associated with `IO`, it&#39;s easy to fall into magical thinking about `Monad`.\n\nFirst of all, I agree with them, and I think the usefulness of Monads mostly arises from Functors that we can embed many functions within the structure, and Monads is a little expansion for robustness of function composition by `join` : `M(M(X)) -&gt; M(X)` to avoid the nested type.\n\n**In the 2nd Question:**\n&gt;do we have to use monadic functions a -&gt; m b?\n&gt;\n&gt; so many tutorials around the web still insist to use a monadic functions since that is the Kleisli triple and the monad-laws.\n\nand many answers like\n\n&gt;I like to think of such an m as meaning &quot;plan-to-get&quot;, where &quot;plans&quot; involve some sort of additional interaction beyond pure computation. \n\nor\n\n&gt;In situations where `Monad` isn&#39;t necessary, it is often simpler to use `Applicative`, `Functor`, or just basic pure functions. In these cases, these things should be (and generally are) used in place of a `Monad`. For example:\n\n    ws &lt;- getLine &gt;&gt;= return . words  -- Monad\n    ws &lt;- words &lt;$&gt; getLine           -- Functor (much nicer)\n\n&gt;To be clear: If it&#39;s possible without a monad, and it&#39;s simpler and more readable without a monad, then you should do it without a monad! If a monad makes the code more complex or confusing than it needs to be, don&#39;t use a monad! Haskell has monads for the sole purpose of making certain complex computations simpler, easier to read, and easier to reason about. If that&#39;s not happening, *you shouldn&#39;t be using a monad*.\n\nReading their answers, I suppose their special feeling about Monad arises from the historical incident that Haskell community has happend to chose Monads in  Kleisli category to solve their problem(IO etc.)\n\nSo, again, I think the usefulness of Monads mostly arises from Functors that we can embed many functions within the structure, and Monads is a little expansion for robustness of function composition by `join` : `M(M(X)) -&gt; M(X)` to avoid the nested type.\n\nIn fact, in JavaScript I implemented as below..\n\n### Functor\n\n&lt;!-- begin snippet: js hide: false console: true babel: false --&gt;\n\n&lt;!-- language: lang-js --&gt;\n\n    console.log(&quot;Functor&quot;);\n    {\n      const unit = (val) =&gt; ({\n        // contextValue: () =&gt; val,\n        fmap: (f) =&gt; unit((() =&gt; {\n          //you can do pretty much anything here\n          const newVal = f(val);\n        //  console.log(newVal); //IO in the functional context\n          return newVal;\n        })()),\n      });\n\n      const a = unit(3)\n        .fmap(x =&gt; x * 2)  //6\n        .fmap(x =&gt; x + 1); //7\n    }\n\n\n&lt;!-- end snippet --&gt;\n\nThe point is we can implement whatever we like in the Functor structure, and in this case, I simply made it IO/`console.log` the value.\n\nAnother point is, to do this Monads is absolutely unnecessary.\n\n### Monad\n\nNow, based on the Functor implementation above, I add extra `join: MMX =&gt; MX` feature to avoid the nested structure that should be helpful for robustness of complex functional composition. \n\nThe functionality is exactly identical to the Functor above, and please note the usage is also identical to the Functor `fmap`. This does not require a &quot;monadic function&quot; to `bind` (Kleisli composition of monads). \n\n&lt;!-- begin snippet: js hide: false console: true babel: false --&gt;\n\n&lt;!-- language: lang-js --&gt;\n\n    console.log(&quot;Monad&quot;);\n    {\n      const unit = (val) =&gt; ({\n        contextValue: () =&gt; val,\n        bind: (f) =&gt; {\n          //fmap value operation\n          const result = (() =&gt; {\n            //you can do pretty much anything here\n            const newVal = f(val);\n            console.log(newVal);\n            return newVal;\n          })();\n          //join: MMX =&gt; MX\n          return (result.contextValue !== undefined)//result is MX\n            ? result //return MX\n            : unit(result) //result is X, so re-wrap and return MX\n        }\n      });\n      //the usage is identical to the Functor fmap.\n      const a = unit(3)\n        .bind(x =&gt; x * 2)  //6\n        .bind(x =&gt; x + 1); //7\n    }\n\n&lt;!-- end snippet --&gt;\n\n### Monad Laws\n\nJust in case, this implementation of the Monad satisfies the monad laws, and the Functor above does not.\n\n&lt;!-- begin snippet: js hide: false console: true babel: false --&gt;\n\n&lt;!-- language: lang-js --&gt;\n\n    console.log(&quot;Monad laws&quot;);\n    {\n      const unit = (val) =&gt; ({\n        contextValue: () =&gt; val,\n        bind: (f) =&gt; {\n          //fmap value operation\n          const result = (() =&gt; {\n            //you can do pretty much anything here\n            const newVal = f(val);\n            //console.log(newVal);\n            return newVal;\n          })();\n          //join: MMX =&gt; MX\n          return (result.contextValue !== undefined)\n            ? result\n            : unit(result)\n        }\n      });\n\n      const M = unit;\n      const a = 1;\n      const f = a =&gt; (a * 2);\n      const g = a =&gt; (a + 1);\n\n      const log = m =&gt; console.log(m.contextValue()) &amp;&amp; m;\n      log(\n        M(f(a))//==m , and f is not monadic\n      );//2\n      console.log(&quot;Left Identity&quot;);\n      log(\n        M(a).bind(f)\n      );//2\n      console.log(&quot;Right Identity&quot;);\n      log(\n        M(f(a))//m\n          .bind(M)// m.bind(M)\n      );//2\n      console.log(&quot;Associativity&quot;);\n      log(\n        M(5).bind(f).bind(g)\n      );//11\n      log(\n        M(5).bind(x =&gt; M(x).bind(f).bind(g))\n      );//11\n\n    }\n\n&lt;!-- end snippet --&gt;\n\nSo, here is my question.\n\nI may be wrong. \n\nIs there any counter example that Functors cannnot do what Monads can do except the robustness of functional composition by flattening the nested structure?\n\nWhat&#39;s so special about Monads in Kleisli category? It seems like it&#39;s fairly possible to implement Monads with a little expansion to avoid the nested structure of Functor and without the monadic functions `a -&gt; m b` that is the entity in Kleisli category.\n\nThanks.\n\n**edit(2018-11-01)**\n\nReading the answers, I agree it&#39;s not appropriate to perform `console.log` inside the IdentityFunctor that should satisfy Functor-laws, so I commented out like the Monad code.\n\nSo, eliminating that problem, my question still holds:\n\n*Is there any counter example that Functors cannnot do what Monads can do except the robustness of functional composition by flattening the nested structure?*\n\n*What&#39;s so special about Monads in Kleisli category? It seems like it&#39;s fairly possible to implement Monads with a little expansion to avoid the nested structure of Functor and without the monadic functions `a -&gt; m b` that is the entity in Kleisli category.*\n\nAn answer from @DarthFennec is:\n\n&gt;&quot;Avoiding the nested type&quot; is not in fact the purpose of `join`, it&#39;s just a neat side-effect. The way you put it makes it sound like `join` just strips the outer type, but the monad&#39;s value is unchanged. \n\nI believe &quot;Avoiding the nested type&quot; is not just a neat side-effect, but a definition of &quot;join&quot; of Monad in category theory,\n\n&gt;the multiplication natural transformation μ:T∘T⇒T of the monad provides for each object X a morphism μX:T(T(X))→T(X)\n\n[monad (in computer science): Relation to monads in category theory][1]\n\n and that&#39;s exactly what my code does.\n\nOn the other hand,\n\n&gt;This is not the case. `join` is the heart of a monad, and it&#39;s what allows the monad to *do things*.\n\nI know many people implements monads in Haskell in this manner, but the fact is, there is **Maybe functor** in Haskell, that does not has `join`, or there is **Free monad** that `join` is embedded from the first place into the defined structure. They are objects that users define **Functors to *do things***.\n\nTherefore, \n\n&gt;You can think of a functor as basically a container. There&#39;s an arbitrary inner type, and around it an outer structure that allows some variance, some extra values to &quot;decorate&quot; your inner value. `fmap` allows you to work on the things inside the container, the way you would work on them normally. This is basically the limit of what you can do with a functor.\n&gt;\n&gt;A monad is a functor with a special power: where `fmap` allows you to work on an inner value, `bind` allows you to *combine outer values* in a consistent way. This is much more powerful than a simple functor.\n\nThese observation does not fit the fact of the existence of Maybe functor and Free monad.\n\n\n  [1]: https://ncatlab.org/nlab/show/monad+%28in+computer+science%29"
           "title" "What&#39;s so special about Monads in Kleisli category?"}
           {"question_id" 1111
            "title" "Clojure"
            "body_markdown" "Q1\r\n\r\n``` clojure\r\n; lorem ipsum\r\n(defn foo\r\n  \"lorem ipsum\"\r\n  [x]\r\n  (* x x))\r\n```\r\n\r\nBye!"}
           {"question_id" 2222
            "title" "???"
            "body_markdown" "Q2\r\n\r\nFarewell!"}
           {"question_id" 3333
            "title" "Ruby"
            "body_markdown" "Q3\r\n\r\n``` ruby\r\nclass Foo\r\n  attr_accessor :bar, :baz\r\n\r\n  def dump(x)\r\n    return unless bar\r\n    return if nil?(baz)\r\n    x * x + bar * baz\r\n  end\r\nend\r\n```\r\n\r\nSee ya!"}]
  "has_more" true
  "quota_max" 10000
  "quota_remaining" 9966
})

