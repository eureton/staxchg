(ns staxchg.core
  (:require [clojure.string :as string])
  (:require [clj-http.client :as http])
  (:require [cheshire.core :as ccore])
  (:require [staxchg.ui :as ui])
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
{
  "items" [
    {
      "answers" [
        {
          "comments" [
            {
              "owner" {
                "reputation" 2142,
                "display_name" "kurosch"
              },
              "score" 110,
              "post_type" "answer",
              "creation_date" 1224871864,
              "post_id" 231778,
              "comment_id" 99470,
              "body_markdown" "This is close, but not correct.  Every time you call a function with a yield statement in it, it returns a brand new generator object.  It&#39;s only when you call that generator&#39;s .next() method that execution resumes after the last yield.",
            }
          ],
          "owner" {
            "reputation" 17593,
            "display_name" "Douglas Mayle"
          },
          "is_accepted" false,
          "score" 357,
          "last_activity_date" 1548322799,
          "answer_id" 231778,
          "body_markdown" "`yield` is just like `return` - it returns whatever you tell it to (as a generator). The difference is that the next time you call the generator, execution starts from the last call to the `yield` statement. Unlike return, **the stack frame is not cleaned up when a yield occurs, however control is transferred back to the caller, so its state will resume the next time the function is called.**\r\n\r\nIn the case of your code, the function `get_child_candidates` is acting like an iterator so that when you extend your list, it adds one element at a time to the new list.\r\n\r\n`list.extend` calls an iterator until it&#39;s exhausted. In the case of the code sample you posted, it would be much clearer to just return a tuple and append that to the list.\r\n",
        },
        {
          "owner" {
            "reputation" 1229840,
            "display_name" "Jon Skeet"
          },
          "is_accepted" false,
          "score" 201,
          "last_activity_date" 1540975379,
          "answer_id" 231788,
          "body_markdown" "It&#39;s returning a generator. I&#39;m not particularly familiar with Python, but I believe it&#39;s the same kind of thing as [C#&#39;s iterator blocks][1] if you&#39;re familiar with those.\r\n\r\nThe key idea is that the compiler/interpreter/whatever does some trickery so that as far as the caller is concerned, they can keep calling next() and it will keep returning values - *as if the generator method was paused*. Now obviously you can&#39;t really &quot;pause&quot; a method, so the compiler builds a state machine for you to remember where you currently are and what the local variables etc look like. This is much easier than writing an iterator yourself.\r\n\r\n  [1]: http://csharpindepth.com/Articles/Chapter11/StreamingAndIterators.aspx\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 333389,
                "display_name" "jfs"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1224900218,
              "post_id" 231801,
              "comment_id" 100409,
              "body_markdown" "`__getitem__` could be defined instead of `__iter__`. For example: `class it: pass; it.__getitem__ = lambda self, i: i*10 if i &lt; 10 else [][0]; for i in it(): print(i)`, It will print: 0, 10, 20, ..., 90",
            },
            {
              "owner" {
                "reputation" 885,
                "display_name" "Peter"
              },
              "score" 19,
              "post_type" "answer",
              "creation_date" 1494081475,
              "post_id" 231801,
              "comment_id" 74681581,
              "body_markdown" "I tried this example in Python 3.6 and if I create `iterator = some_function()`, the variable `iterator` does not have a function called `next()` anymore, but only a `__next__()` function. Thought I&#39;d mention it.",
            },
            {
              "owner" {
                "reputation" 121,
                "display_name" "SystematicDisintegration"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1589237442,
              "post_id" 231801,
              "comment_id" 109209205,
              "body_markdown" "Where does the `for` loop implementation you wrote call the `__iter__` method of `iterator`, the instantiated instance  of `it`?",
            },
            {
              "owner" {
                "reputation" 325,
                "display_name" "gioxc88"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1602769959,
              "post_id" 231801,
              "comment_id" 113829987,
              "body_markdown" "Unfortunately this answer is not true at all. This is not what python interpreter does with generators. It is not creating a class starting from the generator function and implement `__iter__` and `__next__`. What it is acutally doing under the hood is explained in this post https://stackoverflow.com/questions/45723893/how-does-a-generator-function-work-internally#comment78413677_45727729. To cite @Raymond Hettinger  *&quot;generators are not implemented internally as shown in your pure python class. Instead, they share most of the same logic as regular functions&quot;*",
            }
          ],
          "owner" {
            "reputation" 166979,
            "display_name" "Jason Baker"
          },
          "is_accepted" false,
          "score" 597,
          "last_activity_date" 1557235715,
          "answer_id" 231801,
          "body_markdown" "Think of it this way:\r\n\r\nAn iterator is just a fancy sounding term for an object that has a `next()` method.  So a yield-ed function ends up being something like this:\r\n\r\nOriginal version:\r\n\r\n    def some_function():\r\n        for i in xrange(4):\r\n            yield i\r\n\r\n    for i in some_function():\r\n        print i\r\n\r\nThis is basically what the Python interpreter does with the above code:\r\n\r\n    class it:\r\n        def __init__(self):\r\n            # Start at -1 so that we get 0 when we add 1 below.\r\n            self.count = -1\r\n\r\n        # The __iter__ method will be called once by the &#39;for&#39; loop.\r\n        # The rest of the magic happens on the object returned by this method.\r\n        # In this case it is the object itself.\r\n        def __iter__(self):\r\n            return self\r\n\r\n        # The next method will be called repeatedly by the &#39;for&#39; loop\r\n        # until it raises StopIteration.\r\n        def next(self):\r\n            self.count += 1\r\n            if self.count &lt; 4:\r\n                return self.count\r\n            else:\r\n                # A StopIteration exception is raised\r\n                # to signal that the iterator is done.\r\n                # This is caught implicitly by the &#39;for&#39; loop.\r\n                raise StopIteration\r\n\r\n    def some_func():\r\n        return it()\r\n\r\n    for i in some_func():\r\n        print i\r\n\r\nFor more insight as to what&#39;s happening behind the scenes, the `for` loop can be rewritten to this:\r\n\r\n    iterator = some_func()\r\n    try:\r\n        while 1:\r\n            print iterator.next()\r\n    except StopIteration:\r\n        pass\r\n\r\nDoes that make more sense or just confuse you more?  :)\r\n\r\nI should note that this *is* an oversimplification for illustrative purposes. :)\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 12436,
                "display_name" "Matthias Fripp"
              },
              "score" 435,
              "post_type" "answer",
              "creation_date" 1495575713,
              "post_id" 231855,
              "comment_id" 75308701,
              "body_markdown" "`yield` is not as magical this answer suggests. When you call a function that contains a `yield` statement anywhere, you get a generator object, but no code runs. Then each time you extract an object from the generator, Python executes code in the function until it comes to a `yield` statement, then pauses and delivers the object. When you extract another object, Python resumes just after the `yield` and continues until it reaches another `yield` (often the same one, but one iteration later). This continues until the function runs off the end, at which point the generator is deemed exhausted.",
            },
            {
              "owner" {
                "reputation" 3077,
                "display_name" "picmate 涅"
              },
              "score" 44,
              "post_type" "answer",
              "creation_date" 1518722471,
              "post_id" 231855,
              "comment_id" 84633217,
              "body_markdown" "&quot;These iterables are handy... but you store all the values in memory and this is not always what you want&quot;, is either wrong or confusing. An iterable returns an iterator upon calling the iter() on the iterable, and an iterator doesn&#39;t always have to store its values in memory, depending on the implementation of the __iter__ method, it can also generate values in the sequence on demand.",
            },
            {
              "owner" {
                "reputation" 17568,
                "display_name" "WoJ"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588846341,
              "post_id" 231855,
              "comment_id" 109059326,
              "body_markdown" "It would be nice to add to this **great** answer why *It is just the same except you used `()` instead of `[]`*, specifically what `()` is (there may be confusion with a tuple).",
            },
            {
              "owner" {
                "reputation" 563,
                "display_name" "aderchox"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588939592,
              "post_id" 231855,
              "comment_id" 109099646,
              "body_markdown" "I may be wrong, but a generator is not an iterator, a &quot;called generator&quot; is an iterator.",
            },
            {
              "owner" {
                "reputation" 11057,
                "display_name" "alani"
              },
              "score" 8,
              "post_type" "answer",
              "creation_date" 1591423421,
              "post_id" 231855,
              "comment_id" 110055463,
              "body_markdown" "@MatthiasFripp &quot;This continues until the function runs off the end&quot; -- or it encounters a `return` statement. (`return` is permitted in a function containing `yield`, provided that it does not specify a return value.)",
            },
            {
              "owner" {
                "reputation" 12436,
                "display_name" "Matthias Fripp"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1591660993,
              "post_id" 231855,
              "comment_id" 110134904,
              "body_markdown" "@alaniwi: good point. I guess that&#39;s pretty much what happens when the function runs out on its own too.",
            },
            {
              "owner" {
                "reputation" 150,
                "display_name" "Jacob Ward"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1606958616,
              "post_id" 231855,
              "comment_id" 115124375,
              "body_markdown" "The yield statement suspends function’s execution and sends a value back to the caller, but retains enough state to enable function to resume where it is left off. When resumed, the function continues execution immediately after the last yield run. This allows its code to produce a series of values over time, rather than computing them at once and sending them back like a list.",
            }
          ],
          "owner" {
            "reputation" 500965,
            "display_name" "e-satis"
          },
          "is_accepted" true,
          "score" 15381,
          "last_activity_date" 1583368772,
          "answer_id" 231855,
          "body_markdown" "To understand what `yield` does, you must understand what *generators* are. And before you can understand generators, you must understand *iterables*.\r\n\r\nIterables\r\n---------\r\n\r\nWhen you create a list, you can read its items one by one. Reading its items one by one is called iteration:\r\n\r\n    &gt;&gt;&gt; mylist = [1, 2, 3]\r\n    &gt;&gt;&gt; for i in mylist:\r\n    ...    print(i)\r\n    1\r\n    2\r\n    3\r\n\r\n`mylist` is an *iterable*. When you use a list comprehension, you create a list, and so an iterable:\r\n\r\n    &gt;&gt;&gt; mylist = [x*x for x in range(3)]\r\n    &gt;&gt;&gt; for i in mylist:\r\n    ...    print(i)\r\n    0\r\n    1\r\n    4\r\n\r\nEverything you can use &quot;`for... in...`&quot; on is an iterable; `lists`, `strings`, files...\r\n\r\nThese iterables are handy because you can read them as much as you wish, but you store all the values in memory and this is not always what you want when you have a lot of values.\r\n\r\nGenerators\r\n----------\r\n\r\nGenerators are iterators, a kind of iterable **you can only iterate over once**. Generators do not store all the values in memory, **they generate the values on the fly**:\r\n\r\n    &gt;&gt;&gt; mygenerator = (x*x for x in range(3))\r\n    &gt;&gt;&gt; for i in mygenerator:\r\n    ...    print(i)\r\n    0\r\n    1\r\n    4\r\n\r\nIt is just the same except you used `()` instead of `[]`. BUT, you **cannot** perform `for i in mygenerator` a second time since generators can only be used once: they calculate 0, then forget about it and calculate 1, and end calculating 4, one by one.\r\n\r\nYield\r\n-----\r\n\r\n`yield` is a keyword that is used like `return`, except the function will return a generator.\r\n\r\n    &gt;&gt;&gt; def createGenerator():\r\n    ...    mylist = range(3)\r\n    ...    for i in mylist:\r\n    ...        yield i*i\r\n    ...\r\n    &gt;&gt;&gt; mygenerator = createGenerator() # create a generator\r\n    &gt;&gt;&gt; print(mygenerator) # mygenerator is an object!\r\n    &lt;generator object createGenerator at 0xb7555c34&gt;\r\n    &gt;&gt;&gt; for i in mygenerator:\r\n    ...     print(i)\r\n    0\r\n    1\r\n    4\r\n\r\nHere it&#39;s a useless example, but it&#39;s handy when you know your function will return a huge set of values that you will only need to read once.\r\n\r\nTo master `yield`, you must understand that **when you call the function, the code you have written in the function body does not run.** The function only returns the generator object, this is a bit tricky :-)\r\n\r\nThen, your code will continue from where it left off each time `for` uses the generator.\r\n\r\nNow the hard part:\r\n\r\nThe first time the `for` calls the generator object created from your function, it will run the code in your function from the beginning until it hits `yield`, then it&#39;ll return the first value of the loop. Then, each subsequent call will run another iteration of the loop you have written in the function and return the next value. This will continue until the generator is considered empty, which happens when the function runs without hitting `yield`. That can be because the loop has come to an end, or because you no longer satisfy an `&quot;if/else&quot;`.\r\n\r\n---\r\n\r\nYour code explained\r\n-------------------\r\n\r\n*Generator:*\r\n\r\n    # Here you create the method of the node object that will return the generator\r\n    def _get_child_candidates(self, distance, min_dist, max_dist):\r\n\r\n        # Here is the code that will be called each time you use the generator object:\r\n\r\n        # If there is still a child of the node object on its left\r\n        # AND if the distance is ok, return the next child\r\n        if self._leftchild and distance - max_dist &lt; self._median:\r\n            yield self._leftchild\r\n\r\n        # If there is still a child of the node object on its right\r\n        # AND if the distance is ok, return the next child\r\n        if self._rightchild and distance + max_dist &gt;= self._median:\r\n            yield self._rightchild\r\n\r\n        # If the function arrives here, the generator will be considered empty\r\n        # there is no more than two values: the left and the right children\r\n\r\n*Caller:*\r\n\r\n    # Create an empty list and a list with the current object reference\r\n    result, candidates = list(), [self]\r\n\r\n    # Loop on candidates (they contain only one element at the beginning)\r\n    while candidates:\r\n\r\n        # Get the last candidate and remove it from the list\r\n        node = candidates.pop()\r\n\r\n        # Get the distance between obj and the candidate\r\n        distance = node._get_dist(obj)\r\n\r\n        # If distance is ok, then you can fill the result\r\n        if distance &lt;= max_dist and distance &gt;= min_dist:\r\n            result.extend(node._values)\r\n\r\n        # Add the children of the candidate in the candidate&#39;s list\r\n        # so the loop will keep running until it will have looked\r\n        # at all the children of the children of the children, etc. of the candidate\r\n        candidates.extend(node._get_child_candidates(distance, min_dist, max_dist))\r\n\r\n    return result\r\n\r\nThis code contains several smart parts:\r\n\r\n- The loop iterates on a list, but the list expands while the loop is being iterated :-) It&#39;s a concise way to go through all these nested data even if it&#39;s a bit dangerous since you can end up with an infinite loop. In this case, `candidates.extend(node._get_child_candidates(distance, min_dist, max_dist))` exhaust all the values of the generator, but `while` keeps creating new generator objects which will produce different values from the previous ones since it&#39;s not applied on the same node.\r\n\r\n- The `extend()` method is a list object method that expects an iterable and adds its values to the list.\r\n\r\nUsually we pass a list to it:\r\n\r\n    &gt;&gt;&gt; a = [1, 2]\r\n    &gt;&gt;&gt; b = [3, 4]\r\n    &gt;&gt;&gt; a.extend(b)\r\n    &gt;&gt;&gt; print(a)\r\n    [1, 2, 3, 4]\r\n\r\nBut in your code, it gets a generator, which is good because:\r\n\r\n1. You don&#39;t need to read the values twice.\r\n2. You may have a lot of children and you don&#39;t want them all stored in memory.\r\n\r\nAnd it works because Python does not care if the argument of a method is a list or not. Python expects iterables so it will work with strings, lists, tuples, and generators! This is called duck typing and is one of the reasons why Python is so cool. But this is another story, for another question...\r\n\r\nYou can stop here, or read a little bit to see an advanced use of a generator:\r\n\r\nControlling a generator exhaustion\r\n------\r\n\r\n    &gt;&gt;&gt; class Bank(): # Let&#39;s create a bank, building ATMs\r\n    ...    crisis = False\r\n    ...    def create_atm(self):\r\n    ...        while not self.crisis:\r\n    ...            yield &quot;$100&quot;\r\n    &gt;&gt;&gt; hsbc = Bank() # When everything&#39;s ok the ATM gives you as much as you want\r\n    &gt;&gt;&gt; corner_street_atm = hsbc.create_atm()\r\n    &gt;&gt;&gt; print(corner_street_atm.next())\r\n    $100\r\n    &gt;&gt;&gt; print(corner_street_atm.next())\r\n    $100\r\n    &gt;&gt;&gt; print([corner_street_atm.next() for cash in range(5)])\r\n    [&#39;$100&#39;, &#39;$100&#39;, &#39;$100&#39;, &#39;$100&#39;, &#39;$100&#39;]\r\n    &gt;&gt;&gt; hsbc.crisis = True # Crisis is coming, no more money!\r\n    &gt;&gt;&gt; print(corner_street_atm.next())\r\n    &lt;type &#39;exceptions.StopIteration&#39;&gt;\r\n    &gt;&gt;&gt; wall_street_atm = hsbc.create_atm() # It&#39;s even true for new ATMs\r\n    &gt;&gt;&gt; print(wall_street_atm.next())\r\n    &lt;type &#39;exceptions.StopIteration&#39;&gt;\r\n    &gt;&gt;&gt; hsbc.crisis = False # The trouble is, even post-crisis the ATM remains empty\r\n    &gt;&gt;&gt; print(corner_street_atm.next())\r\n    &lt;type &#39;exceptions.StopIteration&#39;&gt;\r\n    &gt;&gt;&gt; brand_new_atm = hsbc.create_atm() # Build a new one to get back in business\r\n    &gt;&gt;&gt; for cash in brand_new_atm:\r\n    ...    print cash\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    $100\r\n    ...\r\n\r\n**Note:** For Python 3, use`print(corner_street_atm.__next__())` or `print(next(corner_street_atm))`\r\n\r\nIt can be useful for various things like controlling access to a resource.\r\n\r\nItertools, your best friend\r\n-----\r\n\r\nThe itertools module contains special functions to manipulate iterables. Ever wish to duplicate a generator?\r\nChain two generators? Group values in a nested list with a one-liner? `Map / Zip` without creating another list?\r\n\r\nThen just `import itertools`.\r\n\r\nAn example? Let&#39;s see the possible orders of arrival for a four-horse race:\r\n\r\n    &gt;&gt;&gt; horses = [1, 2, 3, 4]\r\n    &gt;&gt;&gt; races = itertools.permutations(horses)\r\n    &gt;&gt;&gt; print(races)\r\n    &lt;itertools.permutations object at 0xb754f1dc&gt;\r\n    &gt;&gt;&gt; print(list(itertools.permutations(horses)))\r\n    [(1, 2, 3, 4),\r\n     (1, 2, 4, 3),\r\n     (1, 3, 2, 4),\r\n     (1, 3, 4, 2),\r\n     (1, 4, 2, 3),\r\n     (1, 4, 3, 2),\r\n     (2, 1, 3, 4),\r\n     (2, 1, 4, 3),\r\n     (2, 3, 1, 4),\r\n     (2, 3, 4, 1),\r\n     (2, 4, 1, 3),\r\n     (2, 4, 3, 1),\r\n     (3, 1, 2, 4),\r\n     (3, 1, 4, 2),\r\n     (3, 2, 1, 4),\r\n     (3, 2, 4, 1),\r\n     (3, 4, 1, 2),\r\n     (3, 4, 2, 1),\r\n     (4, 1, 2, 3),\r\n     (4, 1, 3, 2),\r\n     (4, 2, 1, 3),\r\n     (4, 2, 3, 1),\r\n     (4, 3, 1, 2),\r\n     (4, 3, 2, 1)]\r\n\r\n\r\nUnderstanding the inner mechanisms of iteration\r\n------\r\n\r\nIteration is a process implying iterables (implementing the `__iter__()` method) and iterators (implementing the `__next__()` method).\r\nIterables are any objects you can get an iterator from. Iterators are objects that let you iterate on iterables.\r\n\r\nThere is more about it in this article about [how `for` loops work][1].\r\n\r\n  [1]: http://effbot.org/zone/python-for-statement.htm\r\n",
        },
        {
          "owner" {
            "reputation" 78300,
            "display_name" "tzot"
          },
          "is_accepted" false,
          "score" 162,
          "last_activity_date" 1526810765,
          "answer_id" 232111,
          "body_markdown" "Here is an example in plain language. I will provide a correspondence between high-level human concepts to low-level Python concepts.\r\n\r\nI want to operate on a sequence of numbers, but I don&#39;t want to bother my self with the creation of that sequence, I want only to focus on the operation I want to do. So, I do the following:\r\n\r\n- I call you and tell you that I want a sequence of numbers which is produced in a specific way, and I let you know what the algorithm is. &lt;br/&gt;\r\n&lt;b&gt;This step corresponds to `def`ining the generator function, i.e. the function containing a `yield`.&lt;/b&gt;\r\n- Sometime later, I tell you, &quot;OK, get ready to tell me the sequence of numbers&quot;. &lt;br/&gt;\r\n&lt;b&gt;This step corresponds to calling the generator function which returns a generator object.&lt;/b&gt; Note that you don&#39;t tell me any numbers yet; you just grab your paper and pencil.\r\n- I ask you, &quot;tell me the next number&quot;, and you tell me the first number; after that, you wait for me to ask you for the next number. It&#39;s your job to remember where you were, what numbers you have already said, and what is the next number. I don&#39;t care about the details. &lt;br/&gt;\r\n&lt;b&gt;This step corresponds to calling `.next()` on the generator object.&lt;/b&gt;\r\n- … repeat previous step, until…\r\n- eventually, you might come to an end. You don&#39;t tell me a number; you just shout, &quot;hold your horses! I&#39;m done! No more numbers!&quot; &lt;br/&gt;\r\n&lt;b&gt;This step corresponds to the generator object ending its job, and raising a `StopIteration` exception&lt;/b&gt; The generator function does not need to raise the exception. It&#39;s raised automatically when the function ends or issues a `return`.\r\n\r\nThis is what a generator does (a function that contains a `yield`); it starts executing, pauses whenever it does a `yield`, and when asked for a `.next()` value it continues from the point it was last. It fits perfectly by design with the iterator protocol of Python, which describes how to sequentially request values.\r\n\r\nThe most famous user of the iterator protocol is the `for` command in Python. So, whenever you do a:\r\n\r\n    for item in sequence:\r\n\r\nit doesn&#39;t matter if `sequence` is a list, a string, a dictionary or a generator _object_ like described above; the result is the same: you read items off a sequence one by one.\r\n\r\nNote that `def`ining a function which contains a `yield` keyword is not the only way to create a generator; it&#39;s just the easiest way to create one.\r\n\r\nFor more accurate information, read about [iterator types](http://docs.python.org/library/stdtypes.html#iterator-types), the [yield statement](http://docs.python.org/reference/simple_stmts.html#yield) and [generators](http://docs.python.org/glossary.html#term-generator) in the Python documentation.\r\n",
        },
        {
          "owner" {
            "reputation" 202486,
            "display_name" "Claudiu"
          },
          "is_accepted" false,
          "score" 257,
          "last_activity_date" 1366558934,
          "answer_id" 232853,
          "body_markdown" "There&#39;s one extra thing to mention: a function that yields doesn&#39;t actually have to terminate. I&#39;ve written code like this:\r\n\r\n    def fib():\r\n        last, cur = 0, 1\r\n        while True: \r\n            yield cur\r\n            last, cur = cur, last + cur\r\n\r\nThen I can use it in other code like this:\r\n    \r\n    for f in fib():\r\n        if some_condition: break\r\n        coolfuncs(f);\r\n\r\nIt really helps simplify some problems, and makes some things easier to work with. ",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 2827,
                "display_name" "DanielSank"
              },
              "score" 26,
              "post_type" "answer",
              "creation_date" 1497739294,
              "post_id" 237028,
              "comment_id" 76206780,
              "body_markdown" "*&quot;When you see a function with yield statements, apply this easy trick to understand what will happen&quot;* Doesn&#39;t this completely ignore the fact that you can `send` into a generator, which is a huge part of the point of generators?",
            },
            {
              "owner" {
                "reputation" 325,
                "display_name" "Pedro"
              },
              "score" 11,
              "post_type" "answer",
              "creation_date" 1505400497,
              "post_id" 237028,
              "comment_id" 79407236,
              "body_markdown" "&quot;it could be a for loop, but it could also be code like `otherlist.extend(mylist)`&quot; -&gt; This is incorrect. `extend()` modifies the list in-place and does not return an iterable. Trying to loop over `otherlist.extend(mylist)` will fail with a `TypeError` because `extend()` implicitly returns `None`, and you can&#39;t loop over `None`.",
            },
            {
              "owner" {
                "reputation" 24972,
                "display_name" "today"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1514314437,
              "post_id" 237028,
              "comment_id" 82933650,
              "body_markdown" "@pedro You have misunderstood that sentence. It means that python performs the two mentioned steps on `mylist` (not on `otherlist`) when executing `otherlist.extend(mylist)`.",
            }
          ],
          "owner" {
            "reputation" 31946,
            "display_name" "user28409"
          },
          "is_accepted" false,
          "score" 2125,
          "last_activity_date" 1589372986,
          "answer_id" 237028,
          "body_markdown" "## Shortcut to understanding `yield` ##\r\n\r\nWhen you see a function with `yield` statements, apply this easy trick to understand what will happen:\r\n\r\n 1. Insert a line `result = []` at the start of the function.\r\n 2. Replace each `yield expr` with `result.append(expr)`.\r\n 3. Insert a line `return result` at the bottom of the function.\r\n 4. Yay - no more `yield` statements! Read and figure out code.\r\n 5. Compare function to the original definition.\r\n\r\nThis trick may give you an idea of the logic behind the function, but what actually happens with `yield` is significantly different than what happens in the list based approach. In many cases, the yield approach will be a lot more memory efficient and faster too. In other cases, this trick will get you stuck in an infinite loop, even though the original function works just fine. Read on to learn more...\r\n\r\n## Don&#39;t confuse your Iterables, Iterators, and Generators\r\n\r\nFirst, the **iterator protocol** - when you write\r\n\r\n    for x in mylist:\r\n        ...loop body...\r\n\r\nPython performs the following two steps:\r\n\r\n1. Gets an iterator for `mylist`:\r\n   \r\n   Call `iter(mylist)` -&gt; this returns an object with a `next()` method (or `__next__()` in Python 3).\r\n\r\n   [This is the step most people forget to tell you about]\r\n\r\n2. Uses the iterator to loop over items:\r\n\r\n   Keep calling the `next()` method on the iterator returned from step 1. The return value from `next()` is assigned to `x` and the loop body is executed. If an exception `StopIteration` is raised from within `next()`, it means there are no more values in the iterator and the loop is exited.\r\n\r\nThe truth is Python performs the above two steps anytime it wants to *loop over* the contents of an object - so it could be a for loop, but it could also be code like `otherlist.extend(mylist)` (where `otherlist` is a Python list).\r\n\r\nHere `mylist` is an *iterable* because it implements the iterator protocol. In a user-defined class, you can implement the `__iter__()` method to make instances of your class iterable. This method should return an *iterator*. An iterator is an object with a `next()` method. It is possible to implement both `__iter__()` and `next()` on the same class, and have `__iter__()` return `self`. This will work for simple cases, but not when you want two iterators looping over the same object at the same time.\r\n\r\nSo that&#39;s the iterator protocol, many objects implement this protocol:\r\n\r\n 1. Built-in lists, dictionaries, tuples, sets, files.\r\n 2. User-defined classes that implement `__iter__()`.\r\n 3. Generators.\r\n\r\nNote that a `for` loop doesn&#39;t know what kind of object it&#39;s dealing with - it just follows the iterator protocol, and is happy to get item after item as it calls `next()`. Built-in lists return their items one by one, dictionaries return the *keys* one by one, files return the *lines* one by one, etc. And generators return... well that&#39;s where `yield` comes in:\r\n\r\n    def f123():\r\n        yield 1\r\n        yield 2\r\n        yield 3\r\n\r\n    for item in f123():\r\n        print item\r\n\r\nInstead of `yield` statements, if you had three `return` statements in `f123()` only the first would get executed, and the function would exit. But `f123()` is no ordinary function. When `f123()` is called, it *does not* return any of the values in the yield statements! It returns a generator object. Also, the function does not really exit - it goes into a suspended state. When the `for` loop tries to loop over the generator object, the function resumes from its suspended state at the very next line after the `yield` it previously returned from, executes the next line of code, in this case, a `yield` statement, and returns that as the next item. This happens until the function exits, at which point the generator raises `StopIteration`, and the loop exits. \r\n\r\nSo the generator object is sort of like an adapter - at one end it exhibits the iterator protocol, by exposing `__iter__()` and `next()` methods to keep the `for` loop happy. At the other end, however, it runs the function just enough to get the next value out of it, and puts it back in suspended mode.\r\n\r\n## Why Use Generators? ##\r\n\r\nUsually, you can write code that doesn&#39;t use generators but implements the same logic. One option is to use the temporary list &#39;trick&#39; I mentioned before. That will not work in all cases, for e.g. if you have infinite loops, or it may make inefficient use of memory when you have a really long list. The other approach is to implement a new iterable class SomethingIter that keeps the state in instance members and performs the next logical step in it&#39;s `next()` (or `__next__()` in Python 3) method. Depending on the logic, the code inside the `next()` method may end up looking very complex and be prone to bugs. Here generators provide a clean and easy solution.",
        },
        {
          "owner" {
            "reputation" 74384,
            "display_name" "ninjagecko"
          },
          "is_accepted" false,
          "score" 486,
          "last_activity_date" 1489910855,
          "answer_id" 6400990,
          "body_markdown" "The `yield` keyword is reduced to two simple facts:\r\n\r\n1. If the compiler detects the `yield` keyword *anywhere* inside a function, that function no longer returns via the `return` statement. ***Instead***, it **immediately** returns a **lazy &quot;pending list&quot; object** called a generator\r\n2. A generator is iterable. What is an *iterable*? It&#39;s anything like a `list` or `set` or `range` or dict-view, with a *built-in protocol for visiting each element in a certain order*.\r\n\r\nIn a nutshell: **a generator is a lazy, incrementally-pending list**, and **`yield` statements allow you to use function notation to program the list values** the generator should incrementally spit out.\r\n\r\n    generator = myYieldingFunction(...)\r\n    x = list(generator)\r\n\r\n       generator\r\n           v\r\n    [x[0], ..., ???]\r\n\r\n             generator\r\n                 v\r\n    [x[0], x[1], ..., ???]\r\n\r\n                   generator\r\n                       v\r\n    [x[0], x[1], x[2], ..., ???]\r\n\r\n                           StopIteration exception\r\n    [x[0], x[1], x[2]]     done\r\n\r\n    list==[x[0], x[1], x[2]]\r\n\r\n---\r\nExample\r\n---\r\n\r\nLet&#39;s define a function `makeRange` that&#39;s just like Python&#39;s `range`. Calling `makeRange(n)` RETURNS A GENERATOR:\r\n\r\n    def makeRange(n):\r\n        # return 0,1,2,...,n-1\r\n        i = 0\r\n        while i &lt; n:\r\n            yield i\r\n            i += 1\r\n\r\n    &gt;&gt;&gt; makeRange(5)\r\n    &lt;generator object makeRange at 0x19e4aa0&gt;\r\n\r\nTo force the generator to immediately return its pending values, you can pass it into `list()` (just like you could any iterable):\r\n\r\n    &gt;&gt;&gt; list(makeRange(5))\r\n    [0, 1, 2, 3, 4]\r\n\r\n---\r\nComparing example to &quot;just returning a list&quot;\r\n---\r\n\r\nThe above example can be thought of as merely creating a list which you append to and return:\r\n\r\n    # list-version                   #  # generator-version\r\n    def makeRange(n):                #  def makeRange(n):\r\n        &quot;&quot;&quot;return [0,1,2,...,n-1]&quot;&quot;&quot; #~     &quot;&quot;&quot;return 0,1,2,...,n-1&quot;&quot;&quot;\r\n        TO_RETURN = []               #&gt;\r\n        i = 0                        #      i = 0\r\n        while i &lt; n:                 #      while i &lt; n:\r\n            TO_RETURN += [i]         #~         yield i\r\n            i += 1                   #          i += 1  ## indented\r\n        return TO_RETURN             #&gt;\r\n\r\n    &gt;&gt;&gt; makeRange(5)\r\n    [0, 1, 2, 3, 4]\r\n\r\nThere is one major difference, though; see the last section.\r\n\r\n---\r\nHow you might use generators\r\n---\r\n\r\nAn iterable is the last part of a list comprehension, and all generators are iterable, so they&#39;re often used like so:\r\n\r\n    #                   _ITERABLE_\r\n    &gt;&gt;&gt; [x+10 for x in makeRange(5)]\r\n    [10, 11, 12, 13, 14]\r\n\r\nTo get a better feel for generators, you can play around with the `itertools` module (be sure to use `chain.from_iterable` rather than `chain` when warranted). For example, you might even use generators to implement infinitely-long lazy lists like `itertools.count()`. You could implement your own `def enumerate(iterable): zip(count(), iterable)`, or alternatively do so with the `yield` keyword in a while-loop.\r\n\r\nPlease note: generators can actually be used for many more things, such as [implementing coroutines][1] or non-deterministic programming or other elegant things. However, the &quot;lazy lists&quot; viewpoint I present here is the most common use you will find.\r\n\r\n---\r\nBehind the scenes\r\n---\r\n\r\nThis is how the &quot;Python iteration protocol&quot; works. That is, what is going on when you do `list(makeRange(5))`. This is what I describe earlier as a &quot;lazy, incremental list&quot;.\r\n\r\n    &gt;&gt;&gt; x=iter(range(5))\r\n    &gt;&gt;&gt; next(x)\r\n    0\r\n    &gt;&gt;&gt; next(x)\r\n    1\r\n    &gt;&gt;&gt; next(x)\r\n    2\r\n    &gt;&gt;&gt; next(x)\r\n    3\r\n    &gt;&gt;&gt; next(x)\r\n    4\r\n    &gt;&gt;&gt; next(x)\r\n    Traceback (most recent call last):\r\n      File &quot;&lt;stdin&gt;&quot;, line 1, in &lt;module&gt;\r\n    StopIteration\r\n\r\nThe built-in function `next()` just calls the objects `.next()` function, which is a part of the &quot;iteration protocol&quot; and is found on all iterators. You can manually use the `next()` function (and other parts of the iteration protocol) to implement fancy things, usually at the expense of readability, so try to avoid doing that...\r\n\r\n---\r\nMinutiae\r\n---\r\n\r\nNormally, most people would not care about the following distinctions and probably want to stop reading here.\r\n\r\nIn Python-speak, an *iterable* is any object which &quot;understands the concept of a for-loop&quot; like a list `[1,2,3]`, and an *iterator* is a specific instance of the requested for-loop like `[1,2,3].__iter__()`. A *generator* is exactly the same as any iterator, except for the way it was written (with function syntax).\r\n\r\nWhen you request an iterator from a list, it creates a new iterator. However, when you request an iterator from an iterator (which you would rarely do), it just gives you a copy of itself.\r\n\r\nThus, in the unlikely event that you are failing to do something like this...\r\n\r\n    &gt; x = myRange(5)\r\n    &gt; list(x)\r\n    [0, 1, 2, 3, 4]\r\n    &gt; list(x)\r\n    []\r\n\r\n... then remember that a generator is an *iterator*; that is, it is one-time-use. If you want to reuse it, you should call `myRange(...)` again. If you need to use the result twice, convert the result to a list and store it in a variable `x = list(myRange(5))`. Those who absolutely need to clone a generator (for example, who are doing terrifyingly hackish metaprogramming) can use [`itertools.tee`][2] if absolutely necessary, since the copyable iterator Python [PEP][3] standards proposal has been deferred.\r\n\r\n\r\n  [1]: http://www.dabeaz.com/coroutines/index.html\r\n  [2]: https://docs.python.org/2/library/itertools.html#itertools.tee\r\n  [3]: http://en.wikipedia.org/wiki/Python_Enhancement_Proposal#Development",
        },
        {
          "owner" {
            "reputation" 19582,
            "display_name" "Dustin Getz"
          },
          "is_accepted" false,
          "score" 112,
          "last_activity_date" 1508841965,
          "answer_id" 12716515,
          "body_markdown" "Here are some Python examples of how to actually implement generators as if Python did not provide syntactic sugar for them:\r\n\r\n**As a Python generator:**\r\n\r\n    from itertools import islice\r\n    \r\n    def fib_gen():\r\n        a, b = 1, 1\r\n        while True:\r\n            yield a\r\n            a, b = b, a + b\r\n    \r\n    assert [1, 1, 2, 3, 5] == list(islice(fib_gen(), 5))\r\n\r\n\r\n**Using lexical closures instead of generators**\r\n\r\n    def ftake(fnext, last):\r\n        return [fnext() for _ in xrange(last)]\r\n    \r\n    def fib_gen2():\r\n        #funky scope due to python2.x workaround\r\n        #for python 3.x use nonlocal\r\n        def _():\r\n            _.a, _.b = _.b, _.a + _.b\r\n            return _.a\r\n        _.a, _.b = 0, 1\r\n        return _\r\n\r\n    assert [1,1,2,3,5] == ftake(fib_gen2(), 5)\r\n\r\n**Using object closures instead of generators** (because [ClosuresAndObjectsAreEquivalent][3])\r\n\r\n    class fib_gen3:\r\n        def __init__(self):\r\n            self.a, self.b = 1, 1\r\n    \r\n        def __call__(self):\r\n            r = self.a\r\n            self.a, self.b = self.b, self.a + self.b\r\n            return r\r\n    \r\n    assert [1,1,2,3,5] == ftake(fib_gen3(), 5)\r\n\r\n  [1]: https://github.com/dustingetz/sandbox/blob/master/etc/lazy.py\r\n  [2]: http://en.wikipedia.org/wiki/JavaScript\r\n  [3]: http://c2.com/cgi/wiki?ClosuresAndObjectsAreEquivalent\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 20265,
                "display_name" "It&#39;sNotALie."
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1553193193,
              "post_id" 14352675,
              "comment_id" 97303895,
              "body_markdown" "Just a note - in Python 3, `range` also returns a generator instead of a list, so you&#39;d also see a similar idea, except that `__repr__`/`__str__` are overridden to show a nicer result, in this case `range(1, 10, 2)`.",
            }
          ],
          "owner" {
            "reputation" 2069,
            "display_name" "RBansal"
          },
          "is_accepted" false,
          "score" 206,
          "last_activity_date" 1552457048,
          "answer_id" 14352675,
          "body_markdown" "Yield gives you a generator. \r\n\r\n    def get_odd_numbers(i):\r\n        return range(1, i, 2)\r\n    def yield_odd_numbers(i):\r\n        for x in range(1, i, 2):\r\n           yield x\r\n    foo = get_odd_numbers(10)\r\n    bar = yield_odd_numbers(10)\r\n    foo\r\n    [1, 3, 5, 7, 9]\r\n    bar\r\n    &lt;generator object yield_odd_numbers at 0x1029c6f50&gt;\r\n    bar.next()\r\n    1\r\n    bar.next()\r\n    3\r\n    bar.next()\r\n    5\r\n\r\nAs you can see, in the first case `foo` holds the entire list in memory at once. It&#39;s not a big deal for a list with 5 elements, but what if you want a list of 5 million? Not only is this a huge memory eater, it also costs a lot of time to build at the time that the function is called.\r\n\r\nIn the second case, `bar` just gives you a generator. A generator is an iterable--which means you can use it in a `for` loop, etc, but each value can only be accessed once. All the values are also not stored in memory at the same time; the generator object &quot;remembers&quot; where it was in the looping the last time you called it--this way, if you&#39;re using an iterable to (say) count to 50 billion, you don&#39;t have to count to 50 billion all at once and store the 50 billion numbers to count through.\r\n\r\nAgain, this is a pretty contrived example, you probably would use itertools if you really wanted to count to 50 billion. :)\r\n\r\nThis is the most simple use case of generators. As you said, it can be used to write efficient permutations, using yield to push things up through the call stack instead of using some sort of stack variable. Generators can also be used for specialized tree traversal, and all manner of other things.",
        },
        {
          "owner" {
            "reputation" 2766,
            "display_name" "Daniel"
          },
          "is_accepted" false,
          "score" 253,
          "last_activity_date" 1580681359,
          "answer_id" 14404292,
          "body_markdown" "For those who prefer a minimal working example, meditate on this interactive Python session:\r\n\r\n    &gt;&gt;&gt; def f():\r\n    ...   yield 1\r\n    ...   yield 2\r\n    ...   yield 3\r\n    ... \r\n    &gt;&gt;&gt; g = f()\r\n    &gt;&gt;&gt; for i in g:\r\n    ...   print(i)\r\n    ... \r\n    1\r\n    2\r\n    3\r\n    &gt;&gt;&gt; for i in g:\r\n    ...   print(i)\r\n    ... \r\n    &gt;&gt;&gt; # Note that this time nothing was printed\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 2235,
            "display_name" "johnzachary"
          },
          "is_accepted" false,
          "score" 103,
          "last_activity_date" 1359337030,
          "answer_id" 14554322,
          "body_markdown" "I was going to post &quot;read page 19 of Beazley&#39;s &#39;Python: Essential Reference&#39; for a quick description of generators&quot;, but so many others have posted good descriptions already.\r\n\r\nAlso, note that `yield` can be used in coroutines as the dual of their use in generator functions.  Although it isn&#39;t the same use as your code snippet, `(yield)` can be used as an expression in a function.  When a caller sends a value to the method using the `send()` method, then the coroutine will execute until the next `(yield)` statement is encountered.\r\n\r\nGenerators and coroutines are a cool way to set up data-flow type applications.  I thought it would be worthwhile knowing about the other use of the `yield` statement in functions.",
        },
        {
          "owner" {
            "reputation" 4480,
            "display_name" "aestrivex"
          },
          "is_accepted" false,
          "score" 180,
          "last_activity_date" 1526811932,
          "answer_id" 15814755,
          "body_markdown" "There is one type of answer that I don&#39;t feel has been given yet, among the many great answers that describe how to use generators. Here is the programming language theory answer:\r\n\r\nThe `yield` statement in Python returns a generator. A generator in Python is a function that returns &lt;i&gt;continuations&lt;/i&gt; (and specifically a type of coroutine, but continuations represent the more general mechanism to understand what is going on).\r\n\r\nContinuations in programming languages theory are a much more fundamental kind of computation, but they are not often used, because they are extremely hard to reason about and also very difficult to implement. But the idea of what a continuation is, is straightforward: it is the state of a computation that has not yet finished. In this state, the current values of variables, the operations that have yet to be performed, and so on, are saved. Then at some point later in the program the continuation can be invoked, such that the program&#39;s variables are reset to that state and the operations that were saved are carried out.\r\n\r\nContinuations, in this more general form, can be implemented in two ways. In the `call/cc` way, the program&#39;s stack is literally saved and then when the continuation is invoked, the stack is restored.\r\n\r\nIn continuation passing style (CPS), continuations are just normal functions (only in languages where functions are first class) which the programmer explicitly manages and passes around to subroutines. In this style, program state is represented by closures (and the variables that happen to be encoded in them) rather than variables that reside somewhere on the stack. Functions that manage control flow accept continuation as arguments (in some variations of CPS, functions may accept multiple continuations) and manipulate control flow by invoking them by simply calling them and returning afterwards. A very simple example of continuation passing style is as follows:\r\n\r\n    def save_file(filename):\r\n      def write_file_continuation():\r\n        write_stuff_to_file(filename)\r\n\r\n      check_if_file_exists_and_user_wants_to_overwrite(write_file_continuation)\r\n\r\nIn this (very simplistic) example, the programmer saves the operation of actually writing the file into a continuation (which can potentially be a very complex operation with many details to write out), and then passes that continuation (i.e, as a first-class closure) to another operator which does some more processing, and then calls it if necessary. (I use this design pattern a lot in actual GUI programming, either because it saves me lines of code or, more importantly, to manage control flow after GUI events trigger.)\r\n\r\nThe rest of this post will, without loss of generality, conceptualize continuations as CPS, because it is a hell of a lot easier to understand and read.\r\n\r\n&lt;br&gt;\r\n\r\nNow let&#39;s talk about generators in Python. Generators are a specific subtype of continuation. Whereas **continuations are able in general to save the state of a *computation*** (i.e., the program&#39;s call stack), **generators are only able to save the state of iteration over an *iterator***. Although, this definition is slightly misleading for certain use cases of generators. For instance:\r\n\r\n    def f():\r\n      while True:\r\n        yield 4\r\n\r\nThis is clearly a reasonable iterable whose behavior is well defined -- each time the generator iterates over it, it returns 4 (and does so forever). But it isn&#39;t probably the prototypical type of iterable that comes to mind when thinking of iterators (i.e., `for x in collection: do_something(x)`). This example illustrates the power of generators: if anything is an iterator, a generator can save the state of its iteration.\r\n\r\nTo reiterate: Continuations can save the state of a program&#39;s stack and generators can save the state of iteration. This means that continuations are more a lot powerful than generators, but also that generators are a lot, lot easier. They are easier for the language designer to implement, and they are easier for the programmer to use (if you have some time to burn, try to read and understand [this page about continuations and call/cc][1]).\r\n\r\nBut you could easily implement (and conceptualize) generators as a simple, specific case of continuation passing style:\r\n\r\nWhenever `yield` is called, it tells the function to return a continuation.  When the function is called again, it starts from wherever it left off. So, in pseudo-pseudocode (i.e., not pseudocode, but not code) the generator&#39;s `next` method is basically as follows:\r\n\r\n    class Generator():\r\n      def __init__(self,iterable,generatorfun):\r\n        self.next_continuation = lambda:generatorfun(iterable)\r\n\r\n      def next(self):\r\n        value, next_continuation = self.next_continuation()\r\n        self.next_continuation = next_continuation\r\n        return value\r\n\r\nwhere the `yield` keyword is actually syntactic sugar for the real generator function, basically something like:\r\n\r\n    def generatorfun(iterable):\r\n      if len(iterable) == 0:\r\n        raise StopIteration\r\n      else:\r\n        return (iterable[0], lambda:generatorfun(iterable[1:]))\r\n\r\nRemember that this is just pseudocode and the actual implementation of generators in Python is more complex. But as an exercise to understand what is going on, try to use continuation passing style to implement generator objects without use of the `yield` keyword.\r\n\r\n  [1]: http://www.madore.org/~david/computers/callcc.html\r\n",
        },
        {
          "owner" {
            "reputation" 17689,
            "display_name" "Evgeni Sergeev"
          },
          "is_accepted" false,
          "score" 73,
          "last_activity_date" 1488375418,
          "answer_id" 17113322,
          "body_markdown" "Here is a mental image of what ``yield`` does.\r\n\r\nI like to think of a thread as having a stack (even when it&#39;s not implemented that way).\r\n\r\nWhen a normal function is called, it puts its local variables on the stack, does some computation, then clears the stack and returns. The values of its local variables are never seen again.\r\n\r\nWith a `yield` function, when its code begins to run (i.e. after the function is called, returning a generator object, whose `next()` method is then invoked), it similarly puts its local variables onto the stack and computes for a while. But then, when it hits the `yield` statement, before clearing its part of the stack and returning, it takes a snapshot of its local variables and stores them in the generator object. It also writes down the place where it&#39;s currently up to in its code (i.e. the particular `yield` statement).\r\n\r\nSo it&#39;s a kind of a frozen function that the generator is hanging onto.\r\n\r\nWhen `next()` is called subsequently, it retrieves the function&#39;s belongings onto the stack and re-animates it. The function continues to compute from where it left off, oblivious to the fact that it had just spent an eternity in cold storage.\r\n\r\nCompare the following examples:\r\n\r\n    def normalFunction():\r\n        return\r\n        if False:\r\n            pass\r\n\r\n    def yielderFunction():\r\n        return\r\n        if False:\r\n            yield 12\r\n\r\nWhen we call the second function, it behaves very differently to the first. The `yield` statement might be unreachable, but if it&#39;s present anywhere, it changes the nature of what we&#39;re dealing with.\r\n\r\n    &gt;&gt;&gt; yielderFunction()\r\n    &lt;generator object yielderFunction at 0x07742D28&gt;\r\n\r\nCalling `yielderFunction()` doesn&#39;t run its code, but makes a generator out of the code. (Maybe it&#39;s a good idea to name such things with the `yielder` prefix for readability.)\r\n\r\n    &gt;&gt;&gt; gen = yielderFunction()\r\n    &gt;&gt;&gt; dir(gen)\r\n    [&#39;__class__&#39;,\r\n     ...\r\n     &#39;__iter__&#39;,    #Returns gen itself, to make it work uniformly with containers\r\n     ...            #when given to a for loop. (Containers return an iterator instead.)\r\n     &#39;close&#39;,\r\n     &#39;gi_code&#39;,\r\n     &#39;gi_frame&#39;,\r\n     &#39;gi_running&#39;,\r\n     &#39;next&#39;,        #The method that runs the function&#39;s body.\r\n     &#39;send&#39;,\r\n     &#39;throw&#39;]\r\n\r\nThe `gi_code` and `gi_frame` fields are where the frozen state is stored. Exploring them with `dir(..)`, we can confirm that our mental model above is credible.",
        },
        {
          "owner" {
            "reputation" 12912,
            "display_name" "alinsoar"
          },
          "is_accepted" false,
          "score" 115,
          "last_activity_date" 1593675391,
          "answer_id" 18365578,
          "body_markdown" "From a programming viewpoint, the iterators are implemented as [thunks][1].\r\n\r\nTo implement iterators, generators, and thread pools for concurrent execution, etc. as thunks, one uses [messages sent to a closure object][2], which has a dispatcher, and the [dispatcher answers to &quot;messages&quot;][3].\r\n\r\n\r\n[&quot;*next*&quot;][4] is a message sent to a closure, created by the &quot;*iter*&quot; call.\r\n\r\nThere are lots of ways to implement this computation. I used mutation, but it is possible to do this kind of computation without mutation, by returning the current value and the next yielder (making it [referential transparent][5]).  Racket uses a sequence of transformations of the initial program in some intermediary languages, one of such rewriting making the yield operator to be transformed in some language with simpler operators.\r\n\r\nHere is a demonstration of how yield could be rewritten, which uses the structure of R6RS, but the semantics is identical to Python&#39;s. It&#39;s the same model of computation, and only a change in syntax is required to rewrite it using yield of Python.\r\n\r\n&gt;     Welcome to Racket v6.5.0.3.\r\n&gt;\r\n&gt;     -&gt; (define gen\r\n&gt;          (lambda (l)\r\n&gt;            (define yield\r\n&gt;              (lambda ()\r\n&gt;                (if (null? l)\r\n&gt;                    &#39;END\r\n&gt;                    (let ((v (car l)))\r\n&gt;                      (set! l (cdr l))\r\n&gt;                      v))))\r\n&gt;            (lambda(m)\r\n&gt;              (case m\r\n&gt;                (&#39;yield (yield))\r\n&gt;                (&#39;init  (lambda (data)\r\n&gt;                          (set! l data)\r\n&gt;                          &#39;OK))))))\r\n&gt;     -&gt; (define stream (gen &#39;(1 2 3)))\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     1\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     2\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     3\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     &#39;END\r\n&gt;     -&gt; ((stream &#39;init) &#39;(a b))\r\n&gt;     &#39;OK\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     &#39;a\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     &#39;b\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     &#39;END\r\n&gt;     -&gt; (stream &#39;yield)\r\n&gt;     &#39;END\r\n&gt;     -&gt;\r\n\r\n\r\n  [1]: http://en.wikipedia.org/wiki/Thunk_(functional_programming)\r\n  [2]: https://wiki.c2.com/?ClosuresAndObjectsAreEquivalent\r\n  [3]: http://en.wikipedia.org/wiki/Message_passing\r\n  [4]: https://docs.python.org/3/library/functions.html#next\r\n  [5]: https://en.wikipedia.org/wiki/Referential_transparency",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1785,
                "display_name" "Engin OZTURK"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1530495848,
              "post_id" 20704301,
              "comment_id" 89241413,
              "body_markdown" "You are correct. But what is the effect on flow which is to see the behaviour of &quot;yield&quot; ? I can change the algorithm in the name of mathmatics. Will it help to get different assessment of &quot;yield&quot; ?",
            }
          ],
          "owner" {
            "reputation" 1785,
            "display_name" "Engin OZTURK"
          },
          "is_accepted" false,
          "score" 89,
          "last_activity_date" 1526812261,
          "answer_id" 20704301,
          "body_markdown" "Here is a simple example:\r\n\r\n    def isPrimeNumber(n):\r\n        print &quot;isPrimeNumber({}) call&quot;.format(n)\r\n        if n==1:\r\n            return False\r\n        for x in range(2,n):\r\n            if n % x == 0:\r\n                return False\r\n        return True\r\n\r\n    def primes (n=1):\r\n        while(True):\r\n            print &quot;loop step ---------------- {}&quot;.format(n)\r\n            if isPrimeNumber(n): yield n\r\n            n += 1\r\n\r\n    for n in primes():\r\n        if n&gt; 10:break\r\n        print &quot;wiriting result {}&quot;.format(n)\r\n\r\nOutput:\r\n\r\n    loop step ---------------- 1\r\n    isPrimeNumber(1) call\r\n    loop step ---------------- 2\r\n    isPrimeNumber(2) call\r\n    loop step ---------------- 3\r\n    isPrimeNumber(3) call\r\n    wiriting result 3\r\n    loop step ---------------- 4\r\n    isPrimeNumber(4) call\r\n    loop step ---------------- 5\r\n    isPrimeNumber(5) call\r\n    wiriting result 5\r\n    loop step ---------------- 6\r\n    isPrimeNumber(6) call\r\n    loop step ---------------- 7\r\n    isPrimeNumber(7) call\r\n    wiriting result 7\r\n    loop step ---------------- 8\r\n    isPrimeNumber(8) call\r\n    loop step ---------------- 9\r\n    isPrimeNumber(9) call\r\n    loop step ---------------- 10\r\n    isPrimeNumber(10) call\r\n    loop step ---------------- 11\r\n    isPrimeNumber(11) call\r\n\r\nI am not a Python developer, but it looks to me `yield` holds the position of program flow and the next loop start from &quot;yield&quot; position. It seems like it is waiting at that position, and just before that, returning a value outside, and next time continues to work.\r\n\r\nIt seems to be an interesting and nice ability :D\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 617,
                "display_name" "00prometheus"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1449253877,
              "post_id" 21541902,
              "comment_id" 55942433,
              "body_markdown" "Cute! A [trampoline](https://en.wikipedia.org/wiki/Trampoline_(computing)) (in the Lisp sense). Not often one sees those!",
            }
          ],
          "owner" {
            "reputation" 25924,
            "display_name" "Mike McKerns"
          },
          "is_accepted" false,
          "score" 142,
          "last_activity_date" 1391480855,
          "answer_id" 21541902,
          "body_markdown" "While a lot of answers show why you&#39;d use a `yield` to create a generator, there are more uses for `yield`.  It&#39;s quite easy to make a coroutine, which enables the passing of information between two blocks of code.  I won&#39;t repeat any of the fine examples that have already been given about using `yield` to create a generator.\r\n\r\nTo help understand what a `yield` does in the following code, you can use your finger to trace the cycle through any code that has a `yield`.  Every time your finger hits the `yield`, you have to wait for a `next` or a `send` to be entered.  When a `next` is called, you trace through the code until you hit the `yield`… the code on the right of the `yield` is evaluated and returned to the caller… then you wait.  When `next` is called again, you perform another loop through the code.  However, you&#39;ll note that in a coroutine, `yield` can also be used with a `send`… which will send a value from the caller *into* the yielding function. If a `send` is given, then `yield` receives the value sent, and spits it out the left hand side… then the trace through the code progresses until you hit the `yield` again (returning the value at the end, as if `next` was called).\r\n\r\nFor example:\r\n\r\n    &gt;&gt;&gt; def coroutine():\r\n    ...     i = -1\r\n    ...     while True:\r\n    ...         i += 1\r\n    ...         val = (yield i)\r\n    ...         print(&quot;Received %s&quot; % val)\r\n    ...\r\n    &gt;&gt;&gt; sequence = coroutine()\r\n    &gt;&gt;&gt; sequence.next()\r\n    0\r\n    &gt;&gt;&gt; sequence.next()\r\n    Received None\r\n    1\r\n    &gt;&gt;&gt; sequence.send(&#39;hello&#39;)\r\n    Received hello\r\n    2\r\n    &gt;&gt;&gt; sequence.close()\r\n",
        },
        {
          "owner" {
            "reputation" 4569,
            "display_name" "Sławomir Lenart"
          },
          "is_accepted" false,
          "score" 139,
          "last_activity_date" 1526812443,
          "answer_id" 24944096,
          "body_markdown" "There is another `yield` use and meaning (since Python 3.3):\n\n    yield from &lt;expr&gt;\n\nFrom *[PEP 380 -- Syntax for Delegating to a Subgenerator][1]*:\n\n&gt; A syntax is proposed for a generator to delegate part of its operations to another generator. This allows a section of code containing &#39;yield&#39; to be factored out and placed in another generator. Additionally, the subgenerator is allowed to return with a value, and the value is made available to the delegating generator.\n&gt;\n&gt; The new syntax also opens up some opportunities for optimisation when one generator re-yields values produced by another.\n\nMoreover [this][2] will introduce (since Python 3.5):\n\n    async def new_coroutine(data):\n       ...\n       await blocking_action()\n\nto avoid coroutines being confused with a regular generator (today `yield` is used in both).\n\n  [1]: http://www.python.org/dev/peps/pep-0380/\n  [2]: https://www.python.org/dev/peps/pep-0492/\n",
        },
        {
          "owner" {
            "reputation" 796,
            "display_name" "Will Dereham"
          },
          "is_accepted" false,
          "score" 44,
          "last_activity_date" 1432102772,
          "answer_id" 30341713,
          "body_markdown" "`yield` is like a return element for a function. The difference is, that the `yield` element turns a function into a generator. A generator behaves just like a function until something is &#39;yielded&#39;. The generator stops until it is next called, and continues from exactly the same point as it started. You can get a sequence of all the &#39;yielded&#39; values in one, by calling `list(generator())`.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 259,
                "display_name" "Ricardo Barros Louren&#231;o"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1592164945,
              "post_id" 31042491,
              "comment_id" 110320420,
              "body_markdown" "Aaron, at the section you described `yield from`, on the second line you have `under_management = yield     # must receive deposited value`. Can you explain what does it means equaling to `yield`? It seems that you are getting a value, but not clear from where.",
            },
            {
              "owner" {
                "reputation" 270870,
                "display_name" "Aaron Hall"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1592183242,
              "post_id" 31042491,
              "comment_id" 110324607,
              "body_markdown" "@RicardoBarrosLouren&#231;o I have updated that code&#39;s comments a bit - does that seem more clear now?",
            },
            {
              "owner" {
                "reputation" 259,
                "display_name" "Ricardo Barros Louren&#231;o"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1592183406,
              "post_id" 31042491,
              "comment_id" 110324636,
              "body_markdown" "Nice. Got better now (from send argument).",
            }
          ],
          "owner" {
            "reputation" 270870,
            "display_name" "Aaron Hall"
          },
          "is_accepted" false,
          "score" 421,
          "last_activity_date" 1592184650,
          "answer_id" 31042491,
          "body_markdown" "&gt; **What does the `yield` keyword do in Python?**\r\n\r\n# Answer Outline/Summary\r\n\r\n* A function with [**`yield`**][1], when called, **returns a [Generator][2].**\r\n* Generators are iterators because they implement the [**iterator protocol**][3], so you can iterate over them.\r\n* A generator can also be **sent information**, making it conceptually a **coroutine**.\r\n* In Python 3, you can **delegate** from one generator to another in both directions with **`yield from`**.\r\n* (Appendix critiques a couple of answers, including the top one, and discusses the use of `return` in a generator.)\r\n\r\n# Generators:\r\n\r\n**`yield`** is only legal inside of a function definition, and **the inclusion of `yield` in a function definition makes it return a generator.**\r\n\r\nThe idea for generators comes from other languages (see footnote 1) with varying implementations. In Python&#39;s Generators, the execution of the code is [frozen][4] at the point of the yield. When the generator is called (methods are discussed below) execution resumes and then freezes at the next yield.\r\n\r\n`yield` provides an \r\neasy way of [implementing the iterator protocol][5], defined by the following two methods: \r\n`__iter__` and `next` (Python 2) or `__next__` (Python 3).  Both of those methods\r\nmake an object an iterator that you could type-check with the `Iterator` Abstract Base \r\nClass from the `collections` module.\r\n\r\n    &gt;&gt;&gt; def func():\r\n    ...     yield &#39;I am&#39;\r\n    ...     yield &#39;a generator!&#39;\r\n    ... \r\n    &gt;&gt;&gt; type(func)                 # A function with yield is still a function\r\n    &lt;type &#39;function&#39;&gt;\r\n    &gt;&gt;&gt; gen = func()\r\n    &gt;&gt;&gt; type(gen)                  # but it returns a generator\r\n    &lt;type &#39;generator&#39;&gt;\r\n    &gt;&gt;&gt; hasattr(gen, &#39;__iter__&#39;)   # that&#39;s an iterable\r\n    True\r\n    &gt;&gt;&gt; hasattr(gen, &#39;next&#39;)       # and with .next (.__next__ in Python 3)\r\n    True                           # implements the iterator protocol.\r\n\r\nThe generator type is a sub-type of iterator:\r\n\r\n    &gt;&gt;&gt; import collections, types\r\n    &gt;&gt;&gt; issubclass(types.GeneratorType, collections.Iterator)\r\n    True\r\n\r\nAnd if necessary, we can type-check like this:\r\n\r\n    &gt;&gt;&gt; isinstance(gen, types.GeneratorType)\r\n    True\r\n    &gt;&gt;&gt; isinstance(gen, collections.Iterator)\r\n    True\r\n\r\nA feature of an `Iterator` [is that once exhausted][6], you can&#39;t reuse or reset it:\r\n\r\n    &gt;&gt;&gt; list(gen)\r\n    [&#39;I am&#39;, &#39;a generator!&#39;]\r\n    &gt;&gt;&gt; list(gen)\r\n    []\r\n\r\nYou&#39;ll have to make another if you want to use its functionality again (see footnote 2):\r\n\r\n    &gt;&gt;&gt; list(func())\r\n    [&#39;I am&#39;, &#39;a generator!&#39;]\r\n\r\n\r\nOne can yield data programmatically, for example:\r\n\r\n    def func(an_iterable):\r\n        for item in an_iterable:\r\n            yield item\r\n\r\nThe above simple generator is also equivalent to the below - as of Python 3.3 (and not available in Python 2), you can use [`yield from`][7]:\r\n\r\n    def func(an_iterable):\r\n        yield from an_iterable\r\n\r\nHowever, `yield from` also allows for delegation to subgenerators, \r\nwhich will be explained in the following section on cooperative delegation with sub-coroutines.\r\n\r\n# Coroutines:\r\n\r\n`yield` forms an expression that allows data to be sent into the generator (see footnote 3)\r\n\r\nHere is an example, take note of the `received` variable, which will point to the data that is sent to the generator:\r\n\r\n    def bank_account(deposited, interest_rate):\r\n        while True:\r\n            calculated_interest = interest_rate * deposited \r\n            received = yield calculated_interest\r\n            if received:\r\n                deposited += received\r\n\r\n\r\n    &gt;&gt;&gt; my_account = bank_account(1000, .05)\r\n\r\nFirst, we must queue up the generator with the builtin function, [`next`][8]. It will \r\ncall the appropriate `next` or `__next__` method, depending on the version of\r\nPython you are using:\r\n\r\n    &gt;&gt;&gt; first_year_interest = next(my_account)\r\n    &gt;&gt;&gt; first_year_interest\r\n    50.0\r\n\r\nAnd now we can send data into the generator. ([Sending `None` is \r\nthe same as calling `next`][9].) :\r\n\r\n    &gt;&gt;&gt; next_year_interest = my_account.send(first_year_interest + 1000)\r\n    &gt;&gt;&gt; next_year_interest\r\n    102.5\r\n\r\n## Cooperative Delegation to Sub-Coroutine with `yield from`\r\n\r\nNow, recall that `yield from` is available in Python 3. This allows us to delegate coroutines to a subcoroutine:\r\n\r\n\r\n```python\r\n\r\ndef money_manager(expected_rate):\r\n    # must receive deposited value from .send():\r\n    under_management = yield                   # yield None to start.\r\n    while True:\r\n        try:\r\n            additional_investment = yield expected_rate * under_management \r\n            if additional_investment:\r\n                under_management += additional_investment\r\n        except GeneratorExit:\r\n            &#39;&#39;&#39;TODO: write function to send unclaimed funds to state&#39;&#39;&#39;\r\n            raise\r\n        finally:\r\n            &#39;&#39;&#39;TODO: write function to mail tax info to client&#39;&#39;&#39;\r\n        \r\n\r\ndef investment_account(deposited, manager):\r\n    &#39;&#39;&#39;very simple model of an investment account that delegates to a manager&#39;&#39;&#39;\r\n    # must queue up manager:\r\n    next(manager)      # &lt;- same as manager.send(None)\r\n    # This is where we send the initial deposit to the manager:\r\n    manager.send(deposited)\r\n    try:\r\n        yield from manager\r\n    except GeneratorExit:\r\n        return manager.close()  # delegate?\r\n```\r\n\r\nAnd now we can delegate functionality to a sub-generator and it can be used\r\nby a generator just as above:\r\n\r\n```python\r\nmy_manager = money_manager(.06)\r\nmy_account = investment_account(1000, my_manager)\r\nfirst_year_return = next(my_account) # -&gt; 60.0\r\n```\r\nNow simulate adding another 1,000 to the account plus the return on the account (60.0):\r\n\r\n```python\r\nnext_year_return = my_account.send(first_year_return + 1000)\r\nnext_year_return # 123.6\r\n```\r\nYou can read more about the precise semantics of `yield from` in [PEP 380.][10]\r\n\r\n## Other Methods: close and throw\r\n\r\nThe `close` method raises `GeneratorExit` at the point the function \r\nexecution was frozen. This will also be called by `__del__` so you \r\ncan put any cleanup code where you handle the `GeneratorExit`:\r\n```python\r\nmy_account.close()\r\n```\r\nYou can also throw an exception which can be handled in the generator\r\nor propagated back to the user:\r\n\r\n```python\r\nimport sys\r\ntry:\r\n    raise ValueError\r\nexcept:\r\n    my_manager.throw(*sys.exc_info())\r\n```\r\n\r\nRaises:\r\n```none\r\nTraceback (most recent call last):\r\n  File &quot;&lt;stdin&gt;&quot;, line 4, in &lt;module&gt;\r\n  File &quot;&lt;stdin&gt;&quot;, line 6, in money_manager\r\n  File &quot;&lt;stdin&gt;&quot;, line 2, in &lt;module&gt;\r\nValueError\r\n```\r\n# Conclusion\r\n\r\nI believe I have covered all aspects of the following question:\r\n\r\n&gt; **What does the `yield` keyword do in Python?**\r\n\r\nIt turns out that `yield` does a lot. I&#39;m sure I could add even more \r\nthorough examples to this. If you want more or have some constructive criticism, let me know by commenting\r\nbelow.\r\n\r\n-----\r\n\r\n# Appendix:\r\n\r\n## Critique of the Top/Accepted Answer**\r\n\r\n* It is confused on what makes an **iterable**, just using a list as an example. See my references above, but in summary: an iterable has an `__iter__` method returning an **iterator**. An **iterator** provides a `.next` (Python 2 or `.__next__` (Python 3) method, which is implicitly called by `for` loops until it raises `StopIteration`, and once it does, it will continue to do so.\r\n* It then uses a generator expression to describe what a generator is. Since a generator is simply a convenient way to create an **iterator**, it only confuses the matter, and we still have not yet gotten to the `yield` part.\r\n* In **Controlling a generator exhaustion** he calls the `.next` method, when instead he should use the builtin function, `next`. It would be an appropriate layer of indirection, because his code does not work in Python 3.\r\n* Itertools? This was not relevant to what `yield` does at all.\r\n* No discussion of the methods that `yield` provides along with the new functionality `yield from` in Python 3. **The top/accepted answer is a very incomplete answer.**\r\n\r\n## Critique of answer suggesting `yield` in a generator expression or comprehension.\r\n\r\nThe grammar currently allows any expression in a list comprehension. \r\n\r\n    expr_stmt: testlist_star_expr (annassign | augassign (yield_expr|testlist) |\r\n                         (&#39;=&#39; (yield_expr|testlist_star_expr))*)\r\n    ...\r\n    yield_expr: &#39;yield&#39; [yield_arg]\r\n    yield_arg: &#39;from&#39; test | testlist\r\n\r\nSince yield is an expression, it has been touted by some as interesting to use it in comprehensions or generator expression - in spite of citing no particularly good use-case.\r\n\r\nThe CPython core developers are [discussing deprecating its allowance][11].\r\nHere&#39;s a relevant post from the mailing list:\r\n\r\n&gt; On 30 January 2017 at 19:05, Brett Cannon &lt;brett at python.org&gt; wrote:\r\n&gt; &gt; On Sun, 29 Jan 2017 at 16:39 Craig Rodrigues &lt;rodrigc at freebsd.org&gt; wrote:\r\n&gt; &gt;&gt; I&#39;m OK with either approach.  Leaving things the way they are in Python 3\r\n&gt; &gt;&gt; is no good, IMHO.\r\n&gt; &gt;\r\n&gt; &gt; My vote is it be a SyntaxError since you&#39;re not getting what you expect from\r\n&gt; &gt; the syntax.\r\n&gt; \r\n&gt; I&#39;d agree that&#39;s a sensible place for us to end up, as any code\r\n&gt; relying on the current behaviour is really too clever to be\r\n&gt; maintainable.\r\n&gt; \r\n&gt; In terms of getting there, we&#39;ll likely want:\r\n&gt; \r\n&gt; - SyntaxWarning or DeprecationWarning in 3.7\r\n&gt; - Py3k warning in 2.7.x\r\n&gt; - SyntaxError in 3.8\r\n&gt; \r\n&gt; Cheers, Nick.\r\n&gt; \r\n&gt; \r\n&gt; --  Nick Coghlan   |   ncoghlan at gmail.com   |   Brisbane, Australia\r\n\r\nFurther, there is an [outstanding issue (10544)][12] which seems to be pointing in the direction of this *never* being a good idea (PyPy, a Python implementation written in Python, is already raising syntax warnings.)\r\n\r\nBottom line, until the developers of CPython tell us otherwise: **Don&#39;t put `yield` in a generator expression or comprehension.**\r\n\r\n## The `return` statement in a generator\r\n\r\n\r\n\r\nIn [Python 2][13]:\r\n\r\n&gt; In a generator function, the `return` statement is not allowed to include an `expression_list`. In that context, a bare `return` indicates that the generator is done and will cause `StopIteration` to be raised.\r\n\r\nAn `expression_list` is basically any number of expressions separated by commas - essentially, in Python 2, you can stop the generator with `return`, but you can&#39;t return a value.\r\n\r\nIn [Python 3][14]: \r\n\r\n&gt; In a generator function, the `return` statement indicates that the generator is done and will cause `StopIteration` to be raised. The returned value (if any) is used as an argument to construct `StopIteration` and becomes the `StopIteration.value` attribute.\r\n\r\n## Footnotes\r\n\r\n1. &lt;sub&gt;The languages CLU, Sather, and Icon were referenced in the proposal\r\nto introduce the concept of generators to Python. The general idea is\r\nthat a function can maintain internal state and yield intermediate \r\ndata points on demand by the user. This promised to be [superior in performance \r\nto other approaches, including Python threading][15], which isn&#39;t even available on some systems.&lt;/sub&gt;\r\n\r\n2. &lt;sub&gt; This means, for example, that `xrange` objects (`range` in Python 3) aren&#39;t `Iterator`s, even though they are iterable, because they can be reused. Like lists, their `__iter__` methods return iterator objects.&lt;/sub&gt;\r\n\r\n3. &lt;sub&gt; \r\n`yield` was originally introduced as a statement, meaning that it \r\ncould only appear at the beginning of a line in a code block. \r\nNow `yield` creates a yield expression. \r\nhttps://docs.python.org/2/reference/simple_stmts.html#grammar-token-yield_stmt \r\nThis change was [proposed][9] to allow a user to send data into the generator just as\r\none might receive it. To send data, one must be able to assign it to something, and\r\nfor that, a statement just won&#39;t work.&lt;/sub&gt;\r\n\r\n\r\n  [1]: https://docs.python.org/reference/expressions.html#yieldexpr\r\n  [2]: https://docs.python.org/2/tutorial/classes.html#generators\r\n  [3]: https://docs.python.org/2/library/stdtypes.html#iterator-types\r\n  [4]: https://docs.python.org/3.5/glossary.html#term-generator-iterator\r\n  [5]: https://docs.python.org/2/library/stdtypes.html#generator-types\r\n  [6]: https://docs.python.org/2/glossary.html#term-iterator\r\n  [7]: https://www.python.org/dev/peps/pep-0380/\r\n  [8]: https://docs.python.org/2/library/functions.html#next\r\n  [9]: https://www.python.org/dev/peps/pep-0342/#specification-sending-values-into-generators\r\n  [10]: https://www.python.org/dev/peps/pep-0380/#formal-semantics\r\n  [11]: https://mail.python.org/pipermail/python-dev/2017-January/147301.html\r\n  [12]: http://bugs.python.org/issue10544\r\n  [13]: https://docs.python.org/2/reference/simple_stmts.html#the-return-statement\r\n  [14]: https://docs.python.org/3/reference/simple_stmts.html#the-return-statement\r\n  [15]: https://www.python.org/dev/peps/pep-0255/\r\n",
        },
        {
          "owner" {
            "reputation" 8394,
            "display_name" "Mangu Singh Rajpurohit"
          },
          "is_accepted" false,
          "score" 64,
          "last_activity_date" 1526812979,
          "answer_id" 31692481,
          "body_markdown" "Like every answer suggests, `yield` is used for creating a sequence generator. It&#39;s used for generating some sequence dynamically. For example, while reading a file line by line on a network, you can use the `yield` function as follows:\r\n\r\n    def getNextLines():\r\n       while con.isOpen():\r\n           yield con.read()\r\n\r\nYou can use it in your code as follows:\r\n\r\n    for line in getNextLines():\r\n        doSomeThing(line)\r\n\r\n***Execution Control Transfer gotcha***\r\n\r\nThe execution control will be transferred from getNextLines() to the `for` loop when yield is executed. Thus, every time getNextLines() is invoked, execution begins from the point where it was paused last time.\r\n\r\nThus in short, a function with the following code\r\n\r\n    def simpleYield():\r\n        yield &quot;first time&quot;\r\n        yield &quot;second time&quot;\r\n        yield &quot;third time&quot;\r\n        yield &quot;Now some useful value {}&quot;.format(12)\r\n\r\n    for i in simpleYield():\r\n        print i\r\n\r\nwill print\r\n\r\n    &quot;first time&quot;\r\n    &quot;second time&quot;\r\n    &quot;third time&quot;\r\n    &quot;Now some useful value 12&quot;\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 5270,
            "display_name" "Kaleem Ullah"
          },
          "is_accepted" false,
          "score" 56,
          "last_activity_date" 1526813150,
          "answer_id" 32331953,
          "body_markdown" "**Yield is an object**\r\n\r\nA `return` in a function will return a single value.\r\n\r\nIf you want **a function to return a huge set of values**, use `yield`.\r\n\r\nMore importantly, `yield` is a **barrier**.\r\n\r\n&gt; like barrier in the CUDA language, it will not transfer control until it gets\r\n&gt; completed.\r\n\r\nThat is, it will run the code in your function from the beginning until it hits `yield`. Then, it’ll return the first value of the loop.\r\n\r\nThen, every other call will run the loop you have written in the function one more time, returning the next value until there isn&#39;t any value to return.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 1449,
            "display_name" "Bahtiyar &#214;zdere"
          },
          "is_accepted" false,
          "score" 44,
          "last_activity_date" 1461381379,
          "answer_id" 33788856,
          "body_markdown" "The `yield` keyword simply collects returning results. Think of `yield` like `return +=`\r\n",
        },
        {
          "owner" {
            "reputation" 112444,
            "display_name" "Dimitris Fasarakis Hilliard"
          },
          "is_accepted" false,
          "score" 38,
          "last_activity_date" 1499863464,
          "answer_id" 35526740,
          "body_markdown" "Here&#39;s a simple `yield` based approach, to compute the fibonacci series, explained:\r\n\r\n    def fib(limit=50):\r\n        a, b = 0, 1\r\n        for i in range(limit):\r\n           yield b\r\n           a, b = b, a+b\r\n\r\nWhen you enter this into your REPL and then try and call it, you&#39;ll get a mystifying result:\r\n\r\n    &gt;&gt;&gt; fib()\r\n    &lt;generator object fib at 0x7fa38394e3b8&gt;\r\n\r\nThis is because the presence of `yield` signaled to Python that you want to create a *generator*, that is, an object that generates values on demand.\r\n\r\nSo, how do you generate these values? This can either be done directly by using the built-in function `next`, or, indirectly by feeding it to a construct that consumes values. \r\n\r\nUsing the built-in `next()` function, you directly invoke `.next`/`__next__`, forcing the generator to produce a value:\r\n\r\n    &gt;&gt;&gt; g = fib()\r\n    &gt;&gt;&gt; next(g)\r\n    1\r\n    &gt;&gt;&gt; next(g)\r\n    1\r\n    &gt;&gt;&gt; next(g)\r\n    2\r\n    &gt;&gt;&gt; next(g)\r\n    3\r\n    &gt;&gt;&gt; next(g)\r\n    5\r\n\r\nIndirectly, if you provide `fib` to a `for` loop, a `list` initializer, a `tuple` initializer, or anything else that expects an object that generates/produces values, you&#39;ll &quot;consume&quot; the generator until no more values can be produced by it (and it returns):\r\n\r\n    results = []\r\n    for i in fib(30):       # consumes fib\r\n        results.append(i) \r\n    # can also be accomplished with\r\n    results = list(fib(30)) # consumes fib\r\n    \r\nSimilarly, with a `tuple` initializer: \r\n\r\n    &gt;&gt;&gt; tuple(fib(5))       # consumes fib\r\n    (1, 1, 2, 3, 5)\r\n\r\nA generator differs from a function in the sense that it is lazy. It accomplishes this by maintaining it&#39;s local state and allowing you to resume whenever you need to. \r\n\r\nWhen you first invoke `fib` by calling it:\r\n\r\n    f = fib()\r\n\r\nPython compiles the function, encounters the `yield` keyword and simply returns a generator object back at you. Not very helpful it seems. \r\n\r\nWhen you then request it generates the first value, directly or indirectly, it executes all statements that it finds, until it encounters a `yield`, it then yields back the value you supplied to `yield` and pauses. For an example that better demonstrates this, let&#39;s use some `print` calls (replace with `print &quot;text&quot;` if on Python 2):\r\n\r\n    def yielder(value):\r\n        &quot;&quot;&quot; This is an infinite generator. Only use next on it &quot;&quot;&quot; \r\n        while 1:\r\n            print(&quot;I&#39;m going to generate the value for you&quot;)\r\n            print(&quot;Then I&#39;ll pause for a while&quot;)\r\n            yield value\r\n            print(&quot;Let&#39;s go through it again.&quot;)\r\n\r\nNow, enter in the REPL:\r\n\r\n    &gt;&gt;&gt; gen = yielder(&quot;Hello, yield!&quot;)\r\n\r\nyou have a generator object now waiting for a command for it to generate a value. Use `next` and see what get&#39;s printed:\r\n    \r\n    &gt;&gt;&gt; next(gen) # runs until it finds a yield\r\n    I&#39;m going to generate the value for you\r\n    Then I&#39;ll pause for a while\r\n    &#39;Hello, yield!&#39;\r\n\r\nThe unquoted results are what&#39;s printed. The quoted result is what is returned from `yield`. Call `next` again now:\r\n\r\n    &gt;&gt;&gt; next(gen) # continues from yield and runs again\r\n    Let&#39;s go through it again.\r\n    I&#39;m going to generate the value for you\r\n    Then I&#39;ll pause for a while\r\n    &#39;Hello, yield!&#39;\r\n\r\nThe generator remembers it was paused at `yield value` and resumes from there. The next message is printed and the search for the `yield` statement to pause at it performed again (due to the `while` loop).\r\n\r\n  [1]: https://www.python.org/dev/peps/pep-0255/\r\n  [2]: https://stackoverflow.com/questions/1756096/understanding-generators-in-python\r\n  [3]: https://docs.python.org/2/library/stdtypes.html#iterator-types",
        },
        {
          "owner" {
            "reputation" 50616,
            "display_name" "smwikipedia"
          },
          "is_accepted" false,
          "score" 61,
          "last_activity_date" 1542937139,
          "answer_id" 36214653,
          "body_markdown" "(My below answer only speaks from the perspective of using Python generator, not the [underlying implementation of generator mechanism][1], which involves some tricks of stack and heap manipulation.)\r\n\r\nWhen `yield` is used instead of a `return` in a python function, that function is turned into something special called `generator function`. That function will return an object of `generator` type. **The `yield` keyword is a flag to notify the python compiler to treat such function specially.** Normal functions will terminate once some value is returned from it. But with the help of the compiler, the generator function **can be thought of** as resumable. That is, the execution context will be restored and the execution will continue from last run. Until you explicitly call return, which will raise a `StopIteration` exception (which is also part of the iterator protocol), or reach the end of the function. I found a lot of references about `generator` but this [one][2] from the `functional programming perspective` is the most digestable.\r\n\r\n(Now I want to talk about the rationale behind `generator`, and the `iterator` based on my own understanding. I hope this can help you grasp the ***essential motivation*** of iterator and generator. Such concept shows up in other languages as well such as C#.)\r\n\r\n\r\nAs I understand, when we want to process a bunch of data, we usually first store the data somewhere and then process it one by one. But this *naive* approach is problematic. If the data volume is huge, it&#39;s expensive to store them as a whole beforehand. **So instead of storing the `data` itself directly, why not store some kind of `metadata` indirectly, i.e. `the logic how the data is computed`**. \r\n\r\nThere are 2 approaches to wrap such metadata.\r\n\r\n 1. The OO approach, we wrap the metadata `as a class`. This is the so-called `iterator` who implements the iterator protocol (i.e. the `__next__()`, and `__iter__()` methods). This is also the commonly seen [iterator design pattern][3].\r\n 2. The functional approach, we wrap the metadata `as a function`. This is\r\n    the so-called `generator function`. But under the hood, the returned `generator object` still `IS-A` iterator because it also implements the iterator protocol.\r\n\r\nEither way, an iterator is created, i.e. some object that can give you the data you want. The OO approach may be a bit complex. Anyway, which one to use is up to you.\r\n\r\n\r\n  [1]: https://stackoverflow.com/questions/8389812/how-are-generators-and-coroutines-implemented-in-cpython\r\n  [2]: https://docs.python.org/dev/howto/functional.html#generators\r\n  [3]: https://en.wikipedia.org/wiki/Iterator_pattern#Python",
        },
        {
          "owner" {
            "reputation" 11540,
            "display_name" "Bob Stein"
          },
          "is_accepted" false,
          "score" 226,
          "last_activity_date" 1546615821,
          "answer_id" 36220775,
          "body_markdown" "**TL;DR**\r\n\r\nInstead of this:\r\n=====================================\r\n\r\n    def square_list(n):\r\n        the_list = []                         # Replace\r\n        for x in range(n):\r\n            y = x * x\r\n            the_list.append(y)                # these\r\n        return the_list                       # lines\r\n\r\ndo this:\r\n=====================\r\n\r\n    def square_yield(n):\r\n        for x in range(n):\r\n            y = x * x\r\n            yield y                           # with this one.\r\n\r\nWhenever you find yourself building a list from scratch, `yield` each piece instead. \r\n\r\nThis was my first &quot;aha&quot; moment with yield.\r\n\r\n---\r\n\r\n`yield` is a [sugary](https://en.wikipedia.org/wiki/Syntactic_sugar) way to say \r\n\r\n &gt; build a series of stuff\r\n\r\nSame behavior:\r\n\r\n    &gt;&gt;&gt; for square in square_list(4):\r\n    ...     print(square)\r\n    ...\r\n    0\r\n    1\r\n    4\r\n    9\r\n    &gt;&gt;&gt; for square in square_yield(4):\r\n    ...     print(square)\r\n    ...\r\n    0\r\n    1\r\n    4\r\n    9\r\n\r\nDifferent behavior:\r\n\r\nYield is **single-pass**: you can only iterate through once. When a function has a yield in it we call it a [generator function](https://stackoverflow.com/a/1756342/673991). And an [iterator](https://stackoverflow.com/a/9884501/673991) is what it returns. Those terms are revealing. We lose the convenience of a container, but gain the power of a series that&#39;s computed as needed, and arbitrarily long.\r\n\r\nYield is **lazy**, it puts off computation. A function with a yield in it *doesn&#39;t actually execute at all when you call it.* It returns an [iterator object](https://docs.python.org/3/reference/expressions.html#yieldexpr) that remembers where it left off. Each time you call `next()` on the iterator (this happens in a for-loop) execution inches forward to the next yield. `return` raises StopIteration and ends the series (this is the natural end of a for-loop).\r\n\r\nYield is **versatile**. Data doesn&#39;t have to be stored all together, it can be made available one at a time. It can be infinite.\r\n\r\n    &gt;&gt;&gt; def squares_all_of_them():\r\n    ...     x = 0\r\n    ...     while True:\r\n    ...         yield x * x\r\n    ...         x += 1\r\n    ...\r\n    &gt;&gt;&gt; squares = squares_all_of_them()\r\n    &gt;&gt;&gt; for _ in range(4):\r\n    ...     print(next(squares))\r\n    ...\r\n    0\r\n    1\r\n    4\r\n    9\r\n\r\n---\r\nIf you need **multiple passes** and the series isn&#39;t too long, just call `list()` on it:\r\n\r\n    &gt;&gt;&gt; list(square_yield(4))\r\n    [0, 1, 4, 9]\r\n\r\n---\r\nBrilliant choice of the word `yield` because [both meanings](https://www.google.com/search?q=yield+meaning) apply:\r\n\r\n&gt; **yield** &amp;mdash; produce or provide (as in agriculture)\r\n\r\n...provide the next data in the series.\r\n\r\n&gt; **yield** &amp;mdash; give way or relinquish (as in political power)\r\n\r\n...relinquish CPU execution until the iterator advances.",
        },
        {
          "owner" {
            "reputation" 12892,
            "display_name" "Christophe Roussy"
          },
          "is_accepted" false,
          "score" 36,
          "last_activity_date" 1526813905,
          "answer_id" 37964180,
          "body_markdown" "Yet another TL;DR\r\n\r\n**Iterator on list**: `next()` returns the next element of the list\r\n\r\n**Iterator generator**: `next()` will compute the next element on the fly (execute code)\r\n\r\nYou can see the yield/generator as a way to manually run the **control flow** from outside (like continue loop one step), by calling `next`, however complex the flow.\r\n\r\n**Note**: The generator is **NOT** a normal function. It remembers the previous state like local variables (stack). See other answers or articles for detailed explanation. The generator can only be **iterated on once**. You could do without `yield`, but it would not be as nice, so it can be considered &#39;very nice&#39; language sugar.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 4440,
            "display_name" "Tom Fuller"
          },
          "is_accepted" false,
          "score" 54,
          "last_activity_date" 1526814172,
          "answer_id" 39425637,
          "body_markdown" "Many people use `return` rather than `yield`, but in some cases `yield` can be more efficient and easier to work with.\r\n\r\nHere is an example which `yield` is definitely best for:\r\n\r\n&gt; **return** (in function)\r\n\r\n    import random\r\n\r\n    def return_dates():\r\n        dates = [] # With &#39;return&#39; you need to create a list then return it\r\n        for i in range(5):\r\n            date = random.choice([&quot;1st&quot;, &quot;2nd&quot;, &quot;3rd&quot;, &quot;4th&quot;, &quot;5th&quot;, &quot;6th&quot;, &quot;7th&quot;, &quot;8th&quot;, &quot;9th&quot;, &quot;10th&quot;])\r\n            dates.append(date)\r\n        return dates\r\n\r\n&gt; **yield** (in function)\r\n\r\n    def yield_dates():\r\n        for i in range(5):\r\n            date = random.choice([&quot;1st&quot;, &quot;2nd&quot;, &quot;3rd&quot;, &quot;4th&quot;, &quot;5th&quot;, &quot;6th&quot;, &quot;7th&quot;, &quot;8th&quot;, &quot;9th&quot;, &quot;10th&quot;])\r\n            yield date # &#39;yield&#39; makes a generator automatically which works\r\n                       # in a similar way. This is much more efficient.\r\n\r\n&gt; **Calling functions**\r\n\r\n    dates_list = return_dates()\r\n    print(dates_list)\r\n    for i in dates_list:\r\n        print(i)\r\n\r\n    dates_generator = yield_dates()\r\n    print(dates_generator)\r\n    for i in dates_generator:\r\n        print(i)\r\n\r\nBoth functions do the same thing, but `yield` uses three lines instead of five and has one less variable to worry about.\r\n\r\n&gt;&gt; **This is the result from the code:**\r\n\r\n[![Output][1]][1]\r\n\r\nAs you can see both functions do the same thing. The only difference is `return_dates()` gives a list and `yield_dates()` gives a generator.\r\n\r\nA real life example would be something like reading a file line by line or if you just want to make a generator.\r\n\r\n  [1]: http://i.stack.imgur.com/iUFNJ.png\r\n",
        },
        {
          "owner" {
            "reputation" 1733,
            "display_name" "redbandit"
          },
          "is_accepted" false,
          "score" 59,
          "last_activity_date" 1526814276,
          "answer_id" 40022748,
          "body_markdown" "In summary, the `yield` statement transforms your function into a factory that produces a special object called a `generator` which wraps around the body of your original function. When the `generator` is iterated, it executes your function  until it reaches the next `yield` then suspends execution and evaluates to the value passed to `yield`. It repeats this process on each iteration until the path of execution exits the function. For instance,\r\n\r\n    def simple_generator():\r\n        yield &#39;one&#39;\r\n        yield &#39;two&#39;\r\n        yield &#39;three&#39;\r\n\r\n    for i in simple_generator():\r\n        print i\r\n\r\nsimply outputs\r\n\r\n    one\r\n    two\r\n    three\r\n\r\nThe power comes from using the generator with a loop that calculates a sequence, the generator executes the loop stopping each time to &#39;yield&#39; the next result of the calculation, in this way it calculates a list on the fly, the benefit being the memory saved for especially large calculations\r\n\r\nSay you wanted to create a your own `range` function that produces an iterable range of numbers, you could do it like so,\r\n\r\n    def myRangeNaive(i):\r\n        n = 0\r\n        range = []\r\n        while n &lt; i:\r\n            range.append(n)\r\n            n = n + 1\r\n        return range\r\n\r\nand use it like this;\r\n\r\n    for i in myRangeNaive(10):\r\n        print i\r\n\r\nBut this is inefficient because\r\n\r\n * You create an array that you only use once (this wastes memory)\r\n * This code actually loops over that array twice! :(\r\n\r\nLuckily Guido and his team were generous enough to develop generators so we could just do this;\r\n\r\n    def myRangeSmart(i):\r\n        n = 0\r\n        while n &lt; i:\r\n           yield n\r\n           n = n + 1\r\n        return\r\n\r\n    for i in myRangeSmart(10):\r\n        print i\r\n\r\nNow upon each iteration a function on the generator called `next()` executes the function until it either reaches a &#39;yield&#39; statement in which it stops and  &#39;yields&#39; the value or reaches the end of the function. In this case on the first call, `next()` executes up to the yield statement and yield &#39;n&#39;, on the next call it will execute the  increment statement, jump back to the &#39;while&#39;, evaluate it, and if true, it will stop and yield &#39;n&#39; again, it will continue that way until the while condition returns false and the generator jumps to the end of the function.\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1271,
                "display_name" "user9074332"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1580875556,
              "post_id" 41426583,
              "comment_id" 106237799,
              "body_markdown" "are you sure about that output?  wouldnt that only be printed on a single line if you ran that print statement using `print(i, end=&#39; &#39;)`?  Otherwise, i believe the default behavior would put each number on a new line",
            },
            {
              "owner" {
                "reputation" 3030,
                "display_name" "Gavriel Cohen"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1580914694,
              "post_id" 41426583,
              "comment_id" 106255539,
              "body_markdown" "@user9074332, You&#39;re right, but it is written on one line to facilitate understanding",
            }
          ],
          "owner" {
            "reputation" 3030,
            "display_name" "Gavriel Cohen"
          },
          "is_accepted" false,
          "score" 66,
          "last_activity_date" 1580667698,
          "answer_id" 41426583,
          "body_markdown" "An easy example to understand what it is: `yield`\r\n\r\n    def f123():\r\n        for _ in range(4):\r\n            yield 1\r\n            yield 2\r\n\r\n\r\n    for i in f123():\r\n        print (i)\r\n\r\nThe output is: \r\n    \r\n    1 2 1 2 1 2 1 2\r\n\r\n    ",
        },
        {
          "owner" {
            "reputation" 5816,
            "display_name" "blueray"
          },
          "is_accepted" false,
          "score" 30,
          "last_activity_date" 1510661060,
          "answer_id" 43698502,
          "body_markdown" "**yield** is similar to return. The difference is: \r\n \r\n**yield** makes a function iterable (in the following example `primes(n = 1)` function becomes iterable).   \r\nWhat it essentially means is the next time the function is called, it will continue from where it left (which is after the line of `yield expression`).\r\n\r\n    def isprime(n):\r\n        if n == 1:\r\n            return False\r\n        for x in range(2, n):\r\n            if n % x == 0:\r\n                return False\r\n        else:\r\n            return True\r\n    \r\n    def primes(n = 1):\r\n       while(True):\r\n           if isprime(n): yield n\r\n           n += 1 \r\n    \r\n    for n in primes():\r\n        if n &gt; 100: break\r\n        print(n)\r\n\r\nIn the above example if `isprime(n)` is true it will return the prime number. In the next iteration it will continue from the next line \r\n\r\n    n += 1  ",
        },
        {
          "owner" {
            "reputation" 6767,
            "display_name" "Chen A."
          },
          "is_accepted" false,
          "score" 18,
          "last_activity_date" 1507030217,
          "answer_id" 46543549,
          "body_markdown" "All of the answers here are great; but only one of them (the most voted one) relates to **how your code works**. Others are relating to *generators* in general, and how they work.\r\n\r\nSo I won&#39;t repeat what generators are or what yields do; I think these are covered by great existing answers. However, after spending few hours trying to understand a similar code to yours, I&#39;ll break it down how it works.\r\n\r\nYour code traverse a binary tree structure. Let&#39;s take this tree for example:\r\n\r\n        5\r\n       / \\\r\n      3   6\r\n     / \\   \\\r\n    1   4   8\r\n\r\nAnd another simpler implementation of a binary-search tree traversal:\r\n\r\n    class Node(object):\r\n    ..\r\n    def __iter__(self):\r\n        if self.has_left_child():\r\n            for child in self.left:\r\n                yield child\r\n\r\n        yield self.val\r\n\r\n        if self.has_right_child():\r\n            for child in self.right:\r\n                yield child\r\n\r\nThe execution code is on the `Tree` object, which implements `__iter__` as this:\r\n\r\n    def __iter__(self):\r\n\r\n        class EmptyIter():\r\n            def next(self):\r\n                raise StopIteration\r\n\r\n        if self.root:\r\n            return self.root.__iter__()\r\n        return EmptyIter()\r\n\r\nThe `while candidates` statement can be replaced with `for element in tree`; Python translate this to\r\n\r\n    it = iter(TreeObj)  # returns iter(self.root) which calls self.root.__iter__()\r\n    for element in it: \r\n        .. process element .. \r\n\r\nBecause `Node.__iter__` function is a generator, the code **inside it** is executed per iteration. So the execution would look like this:\r\n\r\n 1. root element is first; check if it has left childs and `for` iterate them (let&#39;s call it it1 because its the first iterator object)\r\n 2. it has a child so the `for` is executed. The `for child in self.left` creates a **new iterator** from `self.left`, which is a Node object itself (it2)\r\n 3. Same logic as 2, and a new `iterator` is created (it3)\r\n 4. Now we reached the left end of the tree. `it3` has no left childs so it continues and `yield self.value`\r\n 5. On the next call to `next(it3)` it raises `StopIteration` and exists since it has no right childs (it reaches to the end of the function without yield anything)\r\n 6. `it1` and `it2` are still active - they are not exhausted and calling `next(it2)` would yield values, not raise `StopIteration`\r\n 7. Now we are back to `it2` context, and call `next(it2)` which continues where it stopped: right after the `yield child` statement. Since it has no more left childs it continues and yields it&#39;s `self.val`.\r\n\r\nThe catch here is that every iteration **creates sub-iterators** to traverse the tree, and holds the state of the current iterator. Once it reaches the end it traverse back the stack, and values are returned in the correct order (smallest yields value first).\r\n\r\nYour code example did something similar in a different technique: it populated a **one-element list** for every child, then on the next iteration it pops it and run the function code on the current object (hence the `self`).\r\n\r\nI hope this contributed a little to this legendary topic. I spent several good hours drawing this process to understand it.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 990,
                "display_name" "Mike S"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1535030841,
              "post_id" 47285378,
              "comment_id" 90923314,
              "body_markdown" "This is understandable, but one major difference is that you can have multiple yields in a function/method. The analogy totally breaks down at that point. Yield remembers its place in a function, so the next time you call next(), your function continues on to the next `yield`. This is important, I think, and should be expressed.",
            }
          ],
          "owner" {
            "reputation" 13029,
            "display_name" "Calculus"
          },
          "is_accepted" false,
          "score" 125,
          "last_activity_date" 1527498382,
          "answer_id" 47285378,
          "body_markdown" "All great answers, however a bit difficult for newbies.\r\n\r\nI assume you have learned the `return` statement.\r\n\r\nAs an analogy, `return` and `yield` are twins. `return` means &#39;return and stop&#39; whereas &#39;yield` means &#39;return, but continue&#39;\r\n\r\n&gt; 1. Try to get a num_list with `return`.\r\n\r\n    def num_list(n):\r\n        for i in range(n):\r\n            return i\r\nRun it:\r\n\r\n    In [5]: num_list(3)\r\n    Out[5]: 0\r\n\r\nSee, you get only a single number rather than a list of them. `return` never allows you prevail happily, just implements once and quit.\r\n\r\n&gt; 2. There comes `yield`\r\n\r\nReplace `return` with `yield`:\r\n\r\n    In [10]: def num_list(n):\r\n        ...:     for i in range(n):\r\n        ...:         yield i\r\n        ...:\r\n\r\n    In [11]: num_list(3)\r\n    Out[11]: &lt;generator object num_list at 0x10327c990&gt;\r\n\r\n    In [12]: list(num_list(3))\r\n    Out[12]: [0, 1, 2]\r\n\r\nNow, you win to get all the numbers.\r\n\r\nComparing to `return` which runs once and stops, `yield` runs times you planed.\r\nYou can interpret `return` as `return one of them`, and `yield` as `return all of them`. This is called `iterable`.\r\n\r\n&gt; 3. One more step we can rewrite `yield` statement with `return`\r\n\r\n    In [15]: def num_list(n):\r\n        ...:     result = []\r\n        ...:     for i in range(n):\r\n        ...:         result.append(i)\r\n        ...:     return result\r\n\r\n    In [16]: num_list(3)\r\n    Out[16]: [0, 1, 2]\r\n\r\nIt&#39;s the core about `yield`.\r\n\r\nThe difference between a list `return` outputs and the object `yield` output is:\r\n\r\nYou will always get [0, 1, 2] from a list object but only could retrieve them from &#39;the object `yield` output&#39; once. So, it has a new name `generator` object as displayed in `Out[11]: &lt;generator object num_list at 0x10327c990&gt;`.\r\n\r\nIn conclusion, as a metaphor to grok it:\r\n\r\n * `return` and `yield` are twins\r\n * `list` and `generator` are twins\r\n\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 3030,
            "display_name" "Gavriel Cohen"
          },
          "is_accepted" false,
          "score" 10,
          "last_activity_date" 1520103431,
          "answer_id" 48301132,
          "body_markdown" "Yield\r\n-----\r\n\r\n\r\n    &gt;&gt;&gt; def create_generator():\r\n    ...    my_list = range(3)\r\n    ...    for i in my_list:\r\n    ...        yield i*i\r\n    ...\r\n    &gt;&gt;&gt; my_generator = create_generator() # create a generator\r\n    &gt;&gt;&gt; print(my_generator) # my_generator is an object!\r\n    &lt;generator object create_generator at 0xb7555c34&gt;\r\n    &gt;&gt;&gt; for i in my_generator:\r\n    ...     print(i)\r\n    0\r\n    1\r\n    4\r\r\n\r\n\r\n\r\n**In short**, you can see that the loop does not stop and continues to function even after the object or variable is sent (unlike `return` where the loop stops after execution).",
        },
        {
          "owner" {
            "reputation" 271,
            "display_name" "Savai Maheshwari"
          },
          "is_accepted" false,
          "score" 11,
          "last_activity_date" 1534510716,
          "answer_id" 51895612,
          "body_markdown" "A simple generator function\r\n\r\n    def my_gen():\r\n        n = 1\r\n        print(&#39;This is printed first&#39;)\r\n        # Generator function contains yield statements\r\n        yield n\r\n    \r\n        n += 1\r\n        print(&#39;This is printed second&#39;)\r\n        yield n\r\n    \r\n        n += 1\r\n        print(&#39;This is printed at last&#39;)\r\n        yield n\r\n\r\nyield statement pauses the function saving all its states and later continues from there on successive calls.\r\n\r\nhttps://www.programiz.com/python-programming/generator\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 22770,
            "display_name" "Andy Fedoroff"
          },
          "is_accepted" false,
          "score" 24,
          "last_activity_date" 1586412796,
          "answer_id" 52244968,
          "body_markdown" "In Python `generators` (a special type of `iterators`) are used to generate series of values and `yield` keyword is just like the `return` keyword of generator functions. \r\n\r\n**The other fascinating thing `yield` keyword  does is saving the `state` of a generator function**. \r\n\r\nSo, we can set a `number` to a different value each time the `generator` yields. \r\n\r\nHere&#39;s an instance:\r\n\r\n    def getPrimes(number):\r\n        while True:\r\n            if isPrime(number):\r\n                number = yield number     # a miracle occurs here\r\n            number += 1\r\n\r\n    def printSuccessivePrimes(iterations, base=10):\r\n        primeGenerator = getPrimes(base)\r\n        primeGenerator.send(None)\r\n        for power in range(iterations):\r\n            print(primeGenerator.send(base ** power))\r\n",
        },
        {
          "owner" {
            "reputation" 2127,
            "display_name" "thavan"
          },
          "is_accepted" false,
          "score" 24,
          "last_activity_date" 1568291880,
          "answer_id" 54826880,
          "body_markdown" "`yield` yields something. It&#39;s like somebody asks you to make 5 cupcakes. If you are done with at least one cupcake, you can give it to them to eat while you make other cakes.\r\n\r\n    In [4]: def make_cake(numbers):\r\n       ...:     for i in range(numbers):\r\n       ...:         yield &#39;Cake {}&#39;.format(i)\r\n       ...:\r\n    \r\n    In [5]: factory = make_cake(5)\r\n\r\nHere `factory` is called a generator, which makes you cakes. If you call `make_function`, you get a generator instead of running that function. It is because when `yield` keyword is present in a function, it becomes a generator.\r\n\r\n    In [7]: next(factory)\r\n    Out[7]: &#39;Cake 0&#39;\r\n    \r\n    In [8]: next(factory)\r\n    Out[8]: &#39;Cake 1&#39;\r\n    \r\n    In [9]: next(factory)\r\n    Out[9]: &#39;Cake 2&#39;\r\n    \r\n    In [10]: next(factory)\r\n    Out[10]: &#39;Cake 3&#39;\r\n    \r\n    In [11]: next(factory)\r\n    Out[11]: &#39;Cake 4&#39;\r\n\r\nThey consumed all the cakes, but they ask for one again.\r\n\r\n    In [12]: next(factory)\r\n    ---------------------------------------------------------------------------\r\n    StopIteration                             Traceback (most recent call last)\r\n    &lt;ipython-input-12-0f5c45da9774&gt; in &lt;module&gt;\r\n    ----&gt; 1 next(factory)\r\n    \r\n    StopIteration:\r\n\r\nand they are being told to stop asking more. So once you consumed a generator you are done with it. You need to call `make_cake` again if you want more cakes. It is like placing another order for cupcakes.\r\n\r\n    In [13]: factory = make_cake(3)\r\n    \r\n    In [14]: for cake in factory:\r\n        ...:     print(cake)\r\n        ...:\r\n    Cake 0\r\n    Cake 1\r\n    Cake 2\r\n\r\nYou can also use for loop with a generator like the one above.\r\n\r\nOne more example: Lets say you want a random password whenever you ask for it.\r\n\r\n    In [22]: import random\r\n    \r\n    In [23]: import string\r\n    \r\n    In [24]: def random_password_generator():\r\n        ...:     while True:\r\n        ...:         yield &#39;&#39;.join([random.choice(string.ascii_letters) for _ in range(8)])\r\n        ...:\r\n    \r\n    In [25]: rpg = random_password_generator()\r\n    \r\n    In [26]: for i in range(3):\r\n        ...:     print(next(rpg))\r\n        ...:\r\n    FXpUBhhH\r\n    DdUDHoHn\r\n    dvtebEqG\r\n    \r\n    In [27]: next(rpg)\r\n    Out[27]: &#39;mJbYRMNo&#39;\r\n\r\nHere `rpg` is a generator, which can generate an infinite number of random passwords. So we can also say that generators are useful when we don&#39;t know the length of the sequence, unlike list which has a finite number of elements.",
        },
        {
          "owner" {
            "reputation" 5307,
            "display_name" "Rafael"
          },
          "is_accepted" false,
          "score" 28,
          "last_activity_date" 1553349351,
          "answer_id" 55314423,
          "body_markdown" "An analogy could help to grasp the idea here:\r\n\r\n&lt;img src=&quot;https://i.stack.imgur.com/AJZlN.png&quot; width=&quot;300&quot; /&gt;\r\n\r\nImagine that you have created an amazing machine that is capable of generating thousands and thousands of lightbulbs per day. The machine generates these lightbulbs in boxes with a unique serial number. You don&#39;t have enough space to store all these lightbulbs at the same time (i.e., you cannot keep up with the speed of the machine due to storage limitation), so you would like to adjust this machine to generate lightbulbs on demand.\r\n\r\nPython generators don&#39;t differ much from this concept.\r\n\r\nImagine that you have a function `x` that generates unique serial numbers for the boxes. Obviously, you can have a very large number of such barcodes generated by the function. A wiser, and space efficient, option is to generate those serial numbers on-demand.\r\n\r\nMachine&#39;s code:\r\n\r\n    def barcode_generator():\r\n        serial_number = 10000  # Initial barcode\r\n        while True:\r\n            yield serial_number\r\n            serial_number += 1\r\n    \r\n    \r\n    barcode = barcode_generator()\r\n    while True:\r\n        number_of_lightbulbs_to_generate = int(input(&quot;How many lightbulbs to generate? &quot;))\r\n        barcodes = [next(barcode) for _ in range(number_of_lightbulbs_to_generate)]\r\n        print(barcodes)\r\n    \r\n        # function_to_create_the_next_batch_of_lightbulbs(barcodes)\r\n    \r\n        produce_more = input(&quot;Produce more? [Y/n]: &quot;)\r\n        if produce_more == &quot;n&quot;:\r\n            break\r\n\r\nAs you can see we have a self-contained &quot;function&quot; to generate the next unique serial number each time. This function returns back a generator! As you can see we are not calling the function each time we need a new serial number, but we are using `next()` given the generator to obtain the next serial number.\r\n\r\nOutput:\r\n\r\n    How many lightbulbs to generate? 5\r\n    [10000, 10001, 10002, 10003, 10004]\r\n    Produce more? [Y/n]: y\r\n    How many lightbulbs to generate? 6\r\n    [10005, 10006, 10007, 10008, 10009, 10010]\r\n    Produce more? [Y/n]: y\r\n    How many lightbulbs to generate? 7\r\n    [10011, 10012, 10013, 10014, 10015, 10016, 10017]\r\n    Produce more? [Y/n]: n\r\n\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 447,
                "display_name" "Funny Geeks"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588269827,
              "post_id" 59785342,
              "comment_id" 108841467,
              "body_markdown" "If you try to run this code, the `print(fun())` does not print numbers. Instead, it prints the representation of the generator object returned by `fun()` (something along the lines of `&lt;generator object fun at 0x6fffffe795c8&gt;`)",
            },
            {
              "owner" {
                "reputation" 721,
                "display_name" "Swati Srivastava"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588450611,
              "post_id" 59785342,
              "comment_id" 108902900,
              "body_markdown" "@FunnyGeeks I ran the same code on Jupyter Notebook, and it works fine. Also, the point here was to explain the working of yield keyword. The snippet is just for demo purpose.",
            },
            {
              "owner" {
                "reputation" 447,
                "display_name" "Funny Geeks"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588541469,
              "post_id" 59785342,
              "comment_id" 108931558,
              "body_markdown" "I tried it in python2 and python3 in my cygwin console. It didn&#39;t work. https://github.com/ImAmARobot/PythonTest",
            }
          ],
          "owner" {
            "reputation" 721,
            "display_name" "Swati Srivastava"
          },
          "is_accepted" false,
          "score" 6,
          "last_activity_date" 1579256254,
          "answer_id" 59785342,
          "body_markdown" "yield in python is in a way similar to the return statement, except for some differences. If multiple values have to be returned from a function, return statement will return all the values as a list and it has to be stored in the memory in the caller block. But what if we don&#39;t want to use extra memory? Instead, we want to get the value from the function when we need it. This is where yield comes in. Consider the following function :-\r\n```\r\ndef fun():\r\n   yield 1\r\n   yield 2\r\n   yield 3\r\n```\r\nAnd the caller is :-\r\n```\r\ndef caller():\r\n   print (&#39;First value printing&#39;)\r\n   print (fun())\r\n   print (&#39;Second value printing&#39;)\r\n   print (fun())\r\n   print (&#39;Third value printing&#39;)\r\n   print (fun())\r\n```\r\nThe above code segment (caller function) when called, outputs :-\r\n```\r\nFirst value printing\r\n1\r\nSecond value printing\r\n2\r\nThird value printing\r\n3\r\n```\r\nAs can be seen from above, yield returns a value to its caller, but when the function is called again, it doesn&#39;t start from the first statement, but from the statement right after the yield. In the above example, &quot;First value printing&quot; was printed and the function was called. 1 was returned and printed. Then &quot;Second value printing&quot; was printed and again fun() was called. Instead of printing 1 (the first statement), it returned 2, i.e., the statement just after yield 1. The same process is repeated further.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 1068,
            "display_name" "Harish Kumawat"
          },
          "is_accepted" false,
          "score" 8,
          "last_activity_date" 1583490182,
          "answer_id" 60562054,
          "body_markdown" "**First yield program** \r\n\r\n\r\n   \r\n\r\n    def countdown_gen(x):\r\n          count = x\r\n          while count &gt; 0:\r\n               yield count\r\n               count -= 1\r\n        \r\n    g = countdown_gen(5)\r\n        \r\n    for item in g:\r\n         print(item)\r\n\r\n**Output**\r\n\r\n    5\r\n    4\r\n    3\r\n    2\r\n    1\r\n\r\n**Understand flow** \r\n\r\n[![enter image description here][1]][1]\r\n\r\n\r\n  [1]: https://i.stack.imgur.com/pis87.png\r\n\r\n\r\n**Note**\r\n\r\n \r\n\r\n   1. Return a generator obj Suspend a functon execution and save its\r\n   2. status, function can be executed again.",
        },
        {
          "owner" {
            "reputation" 394,
            "display_name" "Aditya patil"
          },
          "is_accepted" false,
          "score" 7,
          "last_activity_date" 1589025763,
          "answer_id" 61306333,
          "body_markdown" "**The yield keyword is going to replace return in a function definition to create a generator.** \r\n\r\n```\r\ndef create_generator():\r\n   for i in range(100):\r\n   yield i\r\nmyGenerator = create_generator()\r\nprint(myGenerator)\r\n# &lt;generator object create_generator at 0x102dd2480&gt;\r\nfor i in myGenerator:\r\n   print(i) # prints 0-99\r\n```\r\nWhen the returned generator is first used—not in the assignment but the for loop—the function definition will execute until it reaches the yield statement. There, it will pause (see why it’s called yield) until used again. Then, it will pick up where it left off. Upon the final iteration of the generator, any code after the yield command will execute.\r\n\r\n```\r\ndef create_generator():\r\n   print(&quot;Beginning of generator&quot;)\r\n   for i in range(3):\r\n      yield i\r\n   print(&quot;After yield&quot;)\r\nprint(&quot;Before assignment&quot;)\r\n\r\nmyGenerator = create_generator()\r\n\r\nprint(&quot;After assignment&quot;)\r\nfor i in myGenerator :\r\n   print(i) # prints 0-99\r\n&quot;&quot;&quot;\r\nBefore assignment\r\nAfter assignment\r\nBeginning of generator\r\n0\r\n1\r\n2\r\nAfter yield\r\n```\r\n\r\n The **yield** keyword modifies a function’s behavior to produce a generator that’s paused at each yield command during iteration. The function isn’t executed except upon iteration, which leads to improved resource management, and subsequently, a better overall performance. Use generators (and yielded functions) for creating large data sets meant for single-use iteration.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 2467,
            "display_name" "Aaron_ab"
          },
          "is_accepted" false,
          "score" 9,
          "last_activity_date" 1598089857,
          "answer_id" 63533381,
          "body_markdown" "## Can also send data back to the generator!\r\n\r\nIndeed, as many answers here explain, using `yield` creates a `generator`. \r\n\r\nYou can use the `yield` keyword to **send data back to a &quot;live&quot; generator**. \r\n\r\n### Example:\r\n\r\nLet&#39;s say we have a method which translates from english to some other language. And in the beginning of it, it does something which is heavy and should be done once. We want this method run forever (don&#39;t really know why.. :)), and receive words words to be translated. \r\n\r\n\r\n```\r\ndef translator():\r\n    # load all the words in English language and the translation to &#39;other lang&#39;\r\n    my_words_dict = {&#39;hello&#39;: &#39;hello in other language&#39;, &#39;dog&#39;: &#39;dog in other language&#39;}\r\n\r\n    while True:\r\n        word = (yield)\r\n        yield my_words_dict.get(word, &#39;Unknown word...&#39;)\r\n```\r\nRunning:\r\n\r\n```\r\nmy_words_translator = translator()\r\n\r\nnext(my_words_translator)\r\nprint(my_words_translator.send(&#39;dog&#39;))\r\n\r\nnext(my_words_translator)\r\nprint(my_words_translator.send(&#39;cat&#39;))\r\n```\r\nwill print:\r\n```\r\ndog in other language\r\nUnknown word...\r\n```\r\n\r\n### To summarise: \r\n\r\nuse `send` method inside a generator to send data back to the generator. To allow that, a `(yield)` is used. ",
        }
      ],
      "owner" {
        "reputation" 121960,
        "display_name" "Alex. S."
      },
      "view_count" 2387706,
      "score" 10780,
      "last_activity_date" 1606762352,
      "question_id" 231767,
      "body_markdown" "What is the use of the `yield` keyword in Python, and what does it do?\r\n\r\nFor example, I&#39;m trying to understand this code&lt;sup&gt;**1**&lt;/sup&gt;:\r\n\r\n    def _get_child_candidates(self, distance, min_dist, max_dist):\r\n        if self._leftchild and distance - max_dist &lt; self._median:\r\n            yield self._leftchild\r\n        if self._rightchild and distance + max_dist &gt;= self._median:\r\n            yield self._rightchild\t\r\n\r\nAnd this is the caller:\r\n\r\n    result, candidates = [], [self]\r\n    while candidates:\r\n        node = candidates.pop()\r\n        distance = node._get_dist(obj)\r\n        if distance &lt;= max_dist and distance &gt;= min_dist:\r\n            result.extend(node._values)\r\n        candidates.extend(node._get_child_candidates(distance, min_dist, max_dist))\r\n    return result\r\n\r\nWhat happens when the method `_get_child_candidates` is called?\r\nIs a list returned? A single element? Is it called again? When will subsequent calls stop?\r\n\r\n\r\n----------\r\n\r\n\r\n&lt;sub&gt;\r\n1. This piece of code was written by Jochen Schulz (jrschulz), who made a great Python library for metric spaces. This is the link to the complete source: [Module mspace][1].&lt;/sub&gt;\r\n\r\n\r\n  [1]: http://well-adjusted.de/~jrschulz/mspace/",
      "title" "What does the &quot;yield&quot; keyword do?"
    },
    {
      "tags" [
        "python",
        "namespaces",
        "main",
        "python-module",
        "idioms"
      ],
      "answers" [
        {
          "comments" [
            {
              "owner" {
                "reputation" 1619,
                "display_name" "hi15"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1566491972,
              "post_id" 419174,
              "comment_id" 101682583,
              "body_markdown" "Why does a file `helloworld.py` with just ```print(&quot;hello world&quot;)``` in it can run with command ```python helloworld.py``` even when there is no ```if __name__ == &quot;__main__&quot;```?",
            }
          ],
          "owner" {
            "reputation" 148877,
            "display_name" "Harley Holcombe"
          },
          "is_accepted" false,
          "score" 131,
          "last_activity_date" 1436543353,
          "answer_id" 419174,
          "body_markdown" "`if __name__ == &quot;__main__&quot;` is the part that runs when the script is run from (say) the command line using a command like `python myscript.py`.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 324,
                "display_name" "hajef"
              },
              "score" 18,
              "post_type" "answer",
              "creation_date" 1550506154,
              "post_id" 419185,
              "comment_id" 96284627,
              "body_markdown" "Out of curiosity: What hapens if I run `subprocess.run(&#39;foo_bar.py&#39;)` in a python script? I suppose that `foo_bar` will be started with `__name__ = &#39;__main__&#39;` just like when I tipe `foo_bar.py` in cmd manually. Is that the case?\nTaking @MrFooz&#39; Answer into account there should not be any problem doing this and having as many &quot;main&quot; modules at a time as I like. Even changing the `__name__` value or having several independantly creates instances (or instances that created each other by `subprocess`) interact with each other should be business as usual for Python.\nDo I miss something?",
            },
            {
              "owner" {
                "reputation" 92069,
                "display_name" "Mr Fooz"
              },
              "score" 18,
              "post_type" "answer",
              "creation_date" 1550592963,
              "post_id" 419185,
              "comment_id" 96321241,
              "body_markdown" "@hajef You&#39;re correct about how things would work with `subprocess.run`. That said, a generally better way of sharing code between scripts is to create modules and have the scripts call the shared modules instead of invoking each other as scripts. It&#39;s hard to debug `subprocess.run` calls since most debuggers don&#39;t jump across process boundaries, it can add non-trivial system overhead to create and destroy the extra processes, etc.",
            },
            {
              "owner" {
                "reputation" 121,
                "display_name" "user471651"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1551016023,
              "post_id" 419185,
              "comment_id" 96476792,
              "body_markdown" "i have a doubt in foo2.py example in the food for thought section.what does from foo2.py import functionB do? In my view it just imports foo2.py from functionB",
            },
            {
              "owner" {
                "reputation" 324,
                "display_name" "hajef"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1551109876,
              "post_id" 419185,
              "comment_id" 96510378,
              "body_markdown" "@MrFooz I never intended to do anything like this xD\nIt just came to my mind and I realized that it was strange enought to possibly help ppl. wrapping their minds around this sort of stuff.\n\n@user471651 Why should `from foo2 import functionB` import foo2 from functionB? That&#39;s a semantic contortion. `from module import method` imports the method from the modul.",
            },
            {
              "owner" {
                "reputation" 13494,
                "display_name" "Yann Vernier"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1568735491,
              "post_id" 419185,
              "comment_id" 102365381,
              "body_markdown" "One of the modules that may import your code is `multiprocessing`, in particular making this test necessary on Windows.",
            },
            {
              "owner" {
                "reputation" 55342,
                "display_name" "Ben"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1580849036,
              "post_id" 419185,
              "comment_id" 106231301,
              "body_markdown" "Extremely minor point, but I believe python actually determines the `__name__` of an imported module from the import statement, not from stripping &quot;.py&quot; off the filename. Because python identifiers are case sensitive but file names may not be (e.g. on windows), there isn&#39;t necessarily enough information in the filename to determine the correct python module name.",
            },
            {
              "owner" {
                "reputation" 92069,
                "display_name" "Mr Fooz"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1581003005,
              "post_id" 419185,
              "comment_id" 106291724,
              "body_markdown" "@Ben Thanks. Post updated. I didn&#39;t bother finding the actual code for the logic you describe, but it rings true.",
            },
            {
              "owner" {
                "reputation" 92069,
                "display_name" "Mr Fooz"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1585608559,
              "post_id" 419185,
              "comment_id" 107816278,
              "body_markdown" "@user471651 In the food for thought section, `from foo2 import functionB` will trigger the Python interpreter to reload `foo2.py` a second time, but this second time it&#39;ll have `__name__=&#39;foo2&#39;` instead of `__name__=&#39;__main__&#39;`. There will be *two* copies of `functionB` in RAM. `functionA` in `__main__` will call the version loaded as `foo2.functionB`, not the `__main__.functionB` copy.",
            },
            {
              "owner" {
                "reputation" 567,
                "display_name" "BAKE ZQ"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1591935563,
              "post_id" 419185,
              "comment_id" 110249335,
              "body_markdown" "Then, how does this relate to `__main__.py`.",
            },
            {
              "owner" {
                "reputation" 141,
                "display_name" "Wayne Filkins"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1592711451,
              "post_id" 419185,
              "comment_id" 110520509,
              "body_markdown" "This makes utterly no sense to me whatsoever.  I feel like most of this stuff is just useless crap and people just want to make things overly complicated.  Coding should be so much more simple...it&#39;s like humans ruin the entire process.  All of the code is run.  In order.  WTF is the point of stuff like &quot;if __name__ == &#39;__main__&#39;:&quot;??  Either you want it to run, or you don&#39;t.  Either it&#39;s in the right place or it isn&#39;t.  Humans just over complicate everything.  I can program literally anything without all this garbage.  Putting this in all these frameworks just makes them stupidly complex.",
            },
            {
              "owner" {
                "reputation" 131,
                "display_name" "Vaibhav Gupta"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1592798556,
              "post_id" 419185,
              "comment_id" 110542817,
              "body_markdown" "Why do I get error -  ModuleNotFoundError: No module named &#39;functionB&#39; on running foo2.py &amp; foo3.py?",
            },
            {
              "owner" {
                "reputation" 92069,
                "display_name" "Mr Fooz"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1592932551,
              "post_id" 419185,
              "comment_id" 110600256,
              "body_markdown" "@VaibhavGupta You&#39;re probably using an interpreter that doesn&#39;t automatically add the script&#39;s directory to `sys.path`. The auto-add feature has been around a long time (forever?) and makes Python easier to use, but it&#39;s insecure. To improve security, some people disable that feature. I&#39;m having trouble finding a public record of it, but there have been recent discussions about removing the auto-add feature from Python 3. Maybe it&#39;s actually happened now. Regardless, I&#39;ve added a line that should make `foo2.py` and `foo3.py` work for the safer interpreter setup.",
            },
            {
              "owner" {
                "reputation" 161,
                "display_name" "Abhimanyu Shekhawat"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1599647994,
              "post_id" 419185,
              "comment_id" 112836021,
              "body_markdown" "I loved how subtly this line solved my confusion: &quot; &quot;Running&quot; the script is a side effect of importing the script&#39;s module.&quot;",
            }
          ],
          "owner" {
            "reputation" 92069,
            "display_name" "Mr Fooz"
          },
          "is_accepted" true,
          "score" 7199,
          "last_activity_date" 1599866572,
          "answer_id" 419185,
          "body_markdown" "Whenever the Python interpreter reads a source file, it does two things:\r\n\r\n * it sets a few special variables like `__name__`, and then\r\n\r\n * it executes all of the code found in the file.\r\n \r\nLet&#39;s see how this works and how it relates to your question about the `__name__` checks we always see in Python scripts.\r\n\r\nCode Sample\r\n===========\r\n\r\nLet&#39;s use a slightly different code sample to explore how imports and scripts work.  Suppose the following is in a file called `foo.py`.\r\n\r\n    # Suppose this is foo.py.\r\n\r\n    print(&quot;before import&quot;)\r\n    import math\r\n    \r\n    print(&quot;before functionA&quot;)\r\n    def functionA():\r\n        print(&quot;Function A&quot;)\r\n    \r\n    print(&quot;before functionB&quot;)\r\n    def functionB():\r\n        print(&quot;Function B {}&quot;.format(math.sqrt(100)))\r\n    \r\n    print(&quot;before __name__ guard&quot;)\r\n    if __name__ == &#39;__main__&#39;:\r\n        functionA()\r\n        functionB()\r\n    print(&quot;after __name__ guard&quot;)\r\n\r\nSpecial Variables\r\n=================\r\n\r\nWhen the Python interpreter reads a source file, it first defines a few special variables. In this case, we care about the `__name__` variable.\r\n\r\n**When Your Module Is the Main Program**\r\n\r\nIf you are running your module (the source file) as the main program, e.g.\r\n\r\n    python foo.py\r\n\r\nthe interpreter will assign the hard-coded string `&quot;__main__&quot;` to the `__name__` variable, i.e.\r\n\r\n    # It&#39;s as if the interpreter inserts this at the top\r\n    # of your module when run as the main program.\r\n    __name__ = &quot;__main__&quot; \r\n    \r\n**When Your Module Is Imported By Another**\r\n\r\nOn the other hand, suppose some other module is the main program and it imports your module. This means there&#39;s a statement like this in the main program, or in some other module the main program imports:\r\n\r\n    # Suppose this is in some other main program.\r\n    import foo\r\n\r\nThe interpreter will search for your `foo.py` file (along with searching for a few other variants), and prior to executing that module, it will assign the name `&quot;foo&quot;` from the import statement to the `__name__` variable, i.e.\r\n\r\n    # It&#39;s as if the interpreter inserts this at the top\r\n    # of your module when it&#39;s imported from another module.\r\n    __name__ = &quot;foo&quot;\r\n    \r\n\r\nExecuting the Module&#39;s Code\r\n===========================\r\n\r\nAfter the special variables are set up, the interpreter executes all the code in the module, one statement at a time. You may want to open another window on the side with the code sample so you can follow along with this explanation.\r\n\r\n**Always**\r\n\r\n1. It prints the string `&quot;before import&quot;` (without quotes).\r\n\r\n2. It loads the `math` module and assigns it to a variable called `math`. This is equivalent to replacing `import math` with the following (note that `__import__` is a low-level function in Python that takes a string and triggers the actual import):\r\n```\r\n# Find and load a module given its string name, &quot;math&quot;,\r\n# then assign it to a local variable called math.\r\nmath = __import__(&quot;math&quot;)\r\n```\r\n3. It prints the string `&quot;before functionA&quot;`.\r\n\r\n4. It executes the `def` block, creating a function object, then assigning that function object to a variable called `functionA`.\r\n\r\n5. It prints the string `&quot;before functionB&quot;`.\r\n\r\n6. It executes the second `def` block, creating another function object, then assigning it to a variable called `functionB`.\r\n\r\n7. It prints the string `&quot;before __name__ guard&quot;`.\r\n\r\n**Only When Your Module Is the Main Program**\r\n\r\n8. If your module is the main program, then it will see that `__name__` was indeed set to `&quot;__main__&quot;` and it calls the two functions, printing the strings `&quot;Function A&quot;` and `&quot;Function B 10.0&quot;`.\r\n\r\n**Only When Your Module Is Imported by Another**\r\n\r\n8. (**instead**) If your module is not the main program but was imported by another one, then `__name__` will be `&quot;foo&quot;`, not `&quot;__main__&quot;`, and it&#39;ll skip the body of the `if` statement.\r\n\r\n**Always**\r\n\r\n9. It will print the string `&quot;after __name__ guard&quot;` in both situations.\r\n\r\n***Summary***\r\n\r\nIn summary, here&#39;s what&#39;d be printed in the two cases:\r\n\r\n&lt;!-- language: lang-none --&gt;\r\n\r\n    # What gets printed if foo is the main program\r\n    before import\r\n    before functionA\r\n    before functionB\r\n    before __name__ guard\r\n    Function A\r\n    Function B 10.0\r\n    after __name__ guard\r\n\r\n&lt;!-- language: lang-none --&gt;\r\n\r\n    # What gets printed if foo is imported as a regular module\r\n    before import\r\n    before functionA\r\n    before functionB\r\n    before __name__ guard\r\n    after __name__ guard\r\n\r\nWhy Does It Work This Way?\r\n==========================\r\n\r\nYou might naturally wonder why anybody would want this.  Well, sometimes you want to write a `.py` file that can be both used by other programs and/or modules as a module, and can also be run as the main program itself.  Examples:\r\n\r\n * Your module is a library, but you want to have a script mode where it runs some unit tests or a demo.\r\n \r\n * Your module is only used as a main program, but it has some unit tests, and the testing framework works by importing `.py` files like your script and running special test functions. You don&#39;t want it to try running the script just because it&#39;s importing the module.\r\n \r\n * Your module is mostly used as a main program, but it also provides a programmer-friendly API for advanced users.\r\n\r\nBeyond those examples, it&#39;s elegant that running a script in Python is just setting up a few magic variables and importing the script. &quot;Running&quot; the script is a side effect of importing the script&#39;s module.\r\n\r\nFood for Thought\r\n================\r\n\r\n * Question: Can I have multiple `__name__` checking blocks?  Answer: it&#39;s strange to do so, but the language won&#39;t stop you.\r\n \r\n * Suppose the following is in `foo2.py`.  What happens if you say `python foo2.py` on the command-line? Why?\r\n \r\n&lt;!-- language: python --&gt;\r\n\r\n    # Suppose this is foo2.py.\r\n    import os, sys; sys.path.insert(0, os.path.dirname(__file__)) # needed for some interpreters\r\n\r\n    def functionA():\r\n        print(&quot;a1&quot;)\r\n        from foo2 import functionB\r\n        print(&quot;a2&quot;)\r\n        functionB()\r\n        print(&quot;a3&quot;)\r\n\r\n    def functionB():\r\n        print(&quot;b&quot;)\r\n\r\n    print(&quot;t1&quot;)\r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;m1&quot;)\r\n        functionA()\r\n        print(&quot;m2&quot;)\r\n    print(&quot;t2&quot;)\r\n          \r\n * Now, figure out what will happen if you remove the `__name__` check in `foo3.py`:\r\n \r\n&lt;!-- language: python --&gt;\r\n\r\n    # Suppose this is foo3.py.\r\n    import os, sys; sys.path.insert(0, os.path.dirname(__file__)) # needed for some interpreters\r\n    \r\n    def functionA():\r\n        print(&quot;a1&quot;)\r\n        from foo3 import functionB\r\n        print(&quot;a2&quot;)\r\n        functionB()\r\n        print(&quot;a3&quot;)\r\n\r\n    def functionB():\r\n        print(&quot;b&quot;)\r\n\r\n    print(&quot;t1&quot;)\r\n    print(&quot;m1&quot;)\r\n    functionA()\r\n    print(&quot;m2&quot;)\r\n    print(&quot;t2&quot;)\r\n\r\n * What will this do when used as a script?  When imported as a module?\r\n\r\n&lt;!-- language: python --&gt;\r\n\r\n    # Suppose this is in foo4.py\r\n    __name__ = &quot;__main__&quot;\r\n    \r\n    def bar():\r\n        print(&quot;bar&quot;)\r\n        \r\n    print(&quot;before __name__ guard&quot;)\r\n    if __name__ == &quot;__main__&quot;:\r\n        bar()\r\n    print(&quot;after __name__ guard&quot;)\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 127,
                "display_name" "DrWongKC"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1600333051,
              "post_id" 419189,
              "comment_id" 113056383,
              "body_markdown" "This clarifies the 1st answer! Thank you :)",
            }
          ],
          "owner" {
            "reputation" 351613,
            "display_name" "Adam Rosenfield"
          },
          "is_accepted" false,
          "score" 1896,
          "last_activity_date" 1517405296,
          "answer_id" 419189,
          "body_markdown" "When your script is run by passing it as a command to the Python interpreter,\r\n\r\n    python myscript.py\r\n\r\nall of the code that is at indentation level 0 gets executed.  Functions and classes that are defined are, well, defined, but none of their code gets run.  Unlike other languages, there&#39;s no `main()` function that gets run automatically - the `main()` function is implicitly all the code at the top level.\r\n\r\nIn this case, the top-level code is an `if` block.  `__name__` is a built-in variable which evaluates to the name of the current module.  However, if a module is being run directly (as in `myscript.py` above), then `__name__` instead is set to the string `&quot;__main__&quot;`.  Thus, you can test whether your script is being run directly or being imported by something else by testing\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        ...\r\n\r\nIf your script is being imported into another module, its various function and class definitions will be imported and its top-level code will be executed, but the code in the then-body of the `if` clause above won&#39;t get run as the condition is not met. As a basic example, consider the following two scripts:\r\n\r\n    # file one.py\r\n    def func():\r\n        print(&quot;func() in one.py&quot;)\r\n    \r\n    print(&quot;top-level in one.py&quot;)\r\n    \r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;one.py is being run directly&quot;)\r\n    else:\r\n        print(&quot;one.py is being imported into another module&quot;)\r\n\r\n&lt;!-- --&gt;\r\n\r\n    # file two.py\r\n    import one\r\n    \r\n    print(&quot;top-level in two.py&quot;)\r\n    one.func()\r\n    \r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;two.py is being run directly&quot;)\r\n    else:\r\n        print(&quot;two.py is being imported into another module&quot;)\r\n\r\nNow, if you invoke the interpreter as\r\n\r\n    python one.py\r\n\r\nThe output will be\r\n\r\n    top-level in one.py\r\n    one.py is being run directly\r\n\r\nIf you run `two.py` instead:\r\n\r\n    python two.py\r\n\r\nYou get\r\n\r\n    top-level in one.py\r\n    one.py is being imported into another module\r\n    top-level in two.py\r\n    func() in one.py\r\n    two.py is being run directly\r\n\r\nThus, when module `one` gets loaded, its `__name__` equals `&quot;one&quot;` instead of `&quot;__main__&quot;`.\r\n",
        },
        {
          "owner" {
            "reputation" 18647,
            "display_name" "pi."
          },
          "is_accepted" false,
          "score" 749,
          "last_activity_date" 1545125119,
          "answer_id" 419986,
          "body_markdown" "The simplest explanation for the `__name__` variable (imho) is the following:\r\n\r\nCreate the following files.\r\n\r\n    # a.py\r\n    import b\r\n\r\nand\r\n\r\n    # b.py\r\n    print &quot;Hello World from %s!&quot; % __name__\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n        print &quot;Hello World again from %s!&quot; % __name__\r\n\r\n\r\nRunning them will get you this output:\r\n\r\n    $ python a.py\r\n    Hello World from b!\r\n\r\nAs you can see, when a module is imported, Python sets `globals()[&#39;__name__&#39;]` in this module to the module&#39;s name. Also, upon import all the code in the module is being run. As the `if` statement evaluates to `False` this part is not executed.\r\n\r\n    $ python b.py\r\n    Hello World from __main__!\r\n    Hello World again from __main__!\r\n\r\nAs you can see, when a file is executed, Python sets `globals()[&#39;__name__&#39;]` in this file to `&quot;__main__&quot;`. This time, the `if` statement evaluates to `True` and is being run.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 22017,
                "display_name" "jpmc26"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1388081246,
              "post_id" 14502904,
              "comment_id" 31163627,
              "body_markdown" "I would consider this bad form as you&#39;re 1) relying on side effects and 2) abusing `and`. `and` is used for checking if two boolean statements are both true. Since you&#39;re not interested in the result of the `and`, an `if` statement more clearly communicates your intentions.",
            },
            {
              "owner" {
                "reputation" 103215,
                "display_name" "Mark Amery"
              },
              "score" 8,
              "post_type" "answer",
              "creation_date" 1436542386,
              "post_id" 14502904,
              "comment_id" 50673315,
              "body_markdown" "Leaving aside the question of whether exploiting the short-circuit behaviour of boolean operators as a flow control mechanism is bad style or not, the bigger problem is that this *doesn&#39;t answer the question at all*.",
            },
            {
              "owner" {
                "reputation" 21771,
                "display_name" "Prof. Falken"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1575992044,
              "post_id" 14502904,
              "comment_id" 104748457,
              "body_markdown" "@MarkAmery haha, sheesh, now it does. &#128522;",
            }
          ],
          "owner" {
            "reputation" 21771,
            "display_name" "Prof. Falken"
          },
          "is_accepted" false,
          "score" 24,
          "last_activity_date" 1535958311,
          "answer_id" 14502904,
          "body_markdown" "It is a special for when a Python file is called from the command line. This is typically used to call a &quot;main()&quot; function or execute other appropriate startup code, like commandline arguments handling for instance.\r\n\r\nIt could be written in several ways. Another is:\r\n\r\n    def some_function_for_instance_main():\r\n        dosomething()\r\n\r\n\r\n    __name__ == &#39;__main__&#39; and some_function_for_instance_main()\r\n\r\nI am not saying you should use this in production code, but it serves to illustrate that there is nothing &quot;magical&quot; about `if __name__ == &#39;__main__&#39;`. It is a good convention for invoking a main function in Python files.\r\n",
        },
        {
          "owner" {
            "reputation" 13419,
            "display_name" "Nabeel Ahmed"
          },
          "is_accepted" false,
          "score" 54,
          "last_activity_date" 1527113249,
          "answer_id" 15789709,
          "body_markdown" "When there are certain statements in our module (`M.py`) we want to be executed when it&#39;ll be running as main (not imported), we can place those statements (test-cases, print statements) under this `if` block.\r\n\r\nAs by default (when module running as main, not imported) the `__name__` variable is set to `&quot;__main__&quot;`, and when it&#39;ll be imported the `__name__` variable will get a different value, most probably the name of the module (`&#39;M&#39;`).\r\nThis is helpful in running different variants of a modules together, and separating their specific input &amp; output statements and also if there are any test-cases.\r\n\r\n**In short**, use this &#39;` if __name__ == &quot;main&quot;` &#39; block to prevent (certain) code from being run when the module is imported.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 270870,
            "display_name" "Aaron Hall"
          },
          "is_accepted" false,
          "score" 526,
          "last_activity_date" 1522117667,
          "answer_id" 20158605,
          "body_markdown" "&gt; ## What does the `if __name__ == &quot;__main__&quot;:` do?\r\n\r\nTo outline the basics:\r\n\r\n- The global variable, `__name__`, in the module that is the entry point to your program, is `&#39;__main__&#39;`. Otherwise, it&#39;s the name you import the module by.\r\n\r\n- So, code under the `if` block will only run if the module is the entry point to your program.\r\n\r\n- It allows the code in the module to be importable by other modules, without executing the code block beneath on import.\r\n\r\n-------\r\n\r\nWhy do we need this?\r\n\r\n## Developing and Testing Your Code\r\n\r\nSay you&#39;re writing a Python script designed to be used as a module:\r\n\r\n    def do_important():\r\n        &quot;&quot;&quot;This function does something very important&quot;&quot;&quot;\r\n\r\nYou *could* test the module by adding this call of the function to the bottom:\r\n\r\n    do_important()\r\n\r\nand running it (on a command prompt) with something like:\r\n  \r\n    ~$ python important.py\r\n\r\n## The Problem\r\n\r\nHowever, if you want to import the module to another script:\r\n\r\n    import important\r\n\r\nOn import, the `do_important` function would be called, so you&#39;d probably comment out your function call, `do_important()`, at the bottom. \r\n\r\n    # do_important() # I must remember to uncomment to execute this!\r\n\r\nAnd then you&#39;ll have to remember whether or not you&#39;ve commented out your test function call. And this extra complexity would mean you&#39;re likely to forget, making your development process more troublesome.\r\n\r\n## A Better Way\r\n\r\nThe ``__name__`` variable points to the namespace wherever the Python interpreter happens to be at the moment. \r\n\r\nInside an imported module, it&#39;s the name of that module. \r\n\r\nBut inside the primary module (or an interactive Python session, i.e. the interpreter&#39;s Read, Eval, Print Loop, or REPL) you are running everything from its ``&quot;__main__&quot;``.\r\n\r\nSo if you check before executing:\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        do_important()\r\n\r\nWith the above, your code will only execute when you&#39;re running it as the primary module (or intentionally call it from another script). \r\n\r\n## An Even Better Way\r\n\r\nThere&#39;s a Pythonic way to improve on this, though. \r\n\r\nWhat if we want to run this business process from outside the module?\r\n\r\nIf we put the code we want to exercise as we develop and test in a function like this and then do our check for `&#39;__main__&#39;` immediately after:\r\n\r\n    def main():\r\n        &quot;&quot;&quot;business logic for when running this module as the primary one!&quot;&quot;&quot;\r\n        setup()\r\n        foo = do_important()\r\n        bar = do_even_more_important(foo)\r\n        for baz in bar:\r\n            do_super_important(baz)\r\n        teardown()\r\n    \r\n    # Here&#39;s our payoff idiom!\r\n    if __name__ == &#39;__main__&#39;:\r\n        main()\r\n\r\nWe now have a final function for the end of our module that will run if we run the module as the primary module. \r\n\r\nIt will allow the module and its functions and classes to be imported into other scripts without running the ``main`` function, and will also allow the module (and its functions and classes) to be called when running from a different `&#39;__main__&#39;` module, i.e.\r\n\r\n    import important\r\n    important.main()\r\n\r\n[This idiom can also be found in the Python documentation in an explanation of the `__main__` module.][1] That text states:\r\n\r\n&gt; This module represents the (otherwise anonymous) scope in which the\r\n&gt; interpreter’s main program executes — commands read either from\r\n&gt; standard input, from a script file, or from an interactive prompt. It\r\n&gt; is this environment in which the idiomatic “conditional script” stanza\r\n&gt; causes a script to run:\r\n\r\n&gt;     if __name__ == &#39;__main__&#39;:\r\n&gt;         main()\r\n\r\n\r\n  [1]: https://docs.python.org/2/library/__main__.html",
        },
        {
          "owner" {
            "reputation" 1106,
            "display_name" "Zain"
          },
          "is_accepted" false,
          "score" 37,
          "last_activity_date" 1386761033,
          "answer_id" 20517795,
          "body_markdown" "When you run Python interactively the local `__name__` variable is assigned a value of `__main__`. Likewise, when you execute a Python module from the command line, rather than importing it into another module, its `__name__` attribute is assigned a value of `__main__`, rather than the actual name of the module. In this way, modules can look at their own `__name__` value to determine for themselves how they are being used, whether as support for another program or as the main application executed from the command line. Thus, the following idiom is quite common in Python modules:\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n        # Do something appropriate here, like calling a\r\n        # main() function defined elsewhere in this module.\r\n        main()\r\n    else:\r\n        # Do nothing. This module has been imported by another\r\n        # module that wants to make use of the functions,\r\n        # classes and other useful bits it has defined.",
        },
        {
          "owner" {
            "reputation" 270870,
            "display_name" "Aaron Hall"
          },
          "is_accepted" false,
          "score" 85,
          "last_activity_date" 1484069704,
          "answer_id" 26369628,
          "body_markdown" "&gt;# What does `if __name__ == &quot;__main__&quot;:` do?\r\n\r\n`__name__` is a global variable (in Python, global actually means on the [module level][1]) that exists in all namespaces. It is typically the module&#39;s name (as a `str` type).\r\n\r\nAs the only special case, however, in whatever Python process you run, as in mycode.py:\r\n\r\n    python mycode.py\r\n\r\nthe otherwise anonymous global namespace is assigned the value of `&#39;__main__&#39;` to its `__name__`. \r\n\r\nThus, including [the final lines][2]\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n        main()\r\n\r\n* at the end of your mycode.py script,\r\n* when it is the primary, entry-point module that is run by a Python process, \r\n\r\nwill cause your script&#39;s uniquely defined `main` function to run. \r\n\r\nAnother benefit of using this construct: you can also import your code as a module in another script and then run the main function if and when your program decides:\r\n\r\n    import mycode\r\n    # ... any amount of other code\r\n    mycode.main()\r\n\r\n\r\n  [1]: https://docs.python.org/tutorial/modules.html#modules\r\n  [2]: https://docs.python.org/library/__main__.html",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 115,
                "display_name" "fpsdkfsdkmsdfsdfm"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1588776597,
              "post_id" 28051929,
              "comment_id" 109030156,
              "body_markdown" "I wasn&#39;t able to edit the post (minimum 6 characters if change required). Line 14 has &#39;x.y&#39; rather than &#39;x.py&#39;.",
            }
          ],
          "owner" {
            "reputation" 2214,
            "display_name" "Alisa"
          },
          "is_accepted" false,
          "score" 43,
          "last_activity_date" 1590576624,
          "answer_id" 28051929,
          "body_markdown" "Let&#39;s look at the answer in a more abstract way:\r\n\r\nSuppose we have this code in `x.py`:\r\n\r\n    ...\r\n    &lt;Block A&gt;\r\n    if __name__ == &#39;__main__&#39;:\r\n        &lt;Block B&gt;\r\n    ...\r\n\r\nBlocks A and B are run when we are running `x.py`.\r\n\r\nBut just block A (and not B) is run when we are running another module, `y.py` for example, in which `x.py` is imported and the code is run from there (like when a function in `x.py` is called from `y.py`).",
        },
        {
          "owner" {
            "reputation" 286,
            "display_name" "codewizard"
          },
          "is_accepted" false,
          "score" 20,
          "last_activity_date" 1469007024,
          "answer_id" 33916552,
          "body_markdown" "There are a number of variables that the system (Python interpreter) provides for source files (modules).  You can get their values anytime you want, so, let us focus on the **__name__** variable/attribute:\r\n\r\nWhen Python loads a source code file, it executes all of the code found in it. (Note that it doesn&#39;t call all of the methods and functions defined in the file, but it does define them.)\r\n\r\nBefore the interpreter executes the source code file though, it defines a few special variables for that file; **__name__** is one of those special variables that Python automatically defines for each source code file.\r\n\r\nIf Python is loading this source code file as the main program (i.e. the file you run), then it sets the special **__name__** variable for this file to have a value **&quot;__main__&quot;**.\r\n\r\nIf this is being imported from another module, **__name__** will be set to that module&#39;s name.\r\n\r\nSo, in your example in part:\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n       lock = thread.allocate_lock()\r\n       thread.start_new_thread(myfunction, (&quot;Thread #: 1&quot;, 2, lock))\r\n       thread.start_new_thread(myfunction, (&quot;Thread #: 2&quot;, 2, lock))\r\n\r\n means that the code block:\r\n\r\n    lock = thread.allocate_lock()\r\n    thread.start_new_thread(myfunction, (&quot;Thread #: 1&quot;, 2, lock))\r\n    thread.start_new_thread(myfunction, (&quot;Thread #: 2&quot;, 2, lock))\r\n\r\nwill be executed only when you run the module directly; the code block will not execute if another module is calling/importing it because the value of **__name__** will not equal to &quot;__main__&quot; in that particular instance.\r\n\r\nHope this helps out.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 994,
            "display_name" "The Gr8 Adakron"
          },
          "is_accepted" false,
          "score" 19,
          "last_activity_date" 1527113647,
          "answer_id" 36820845,
          "body_markdown" "`if __name__ == &quot;__main__&quot;:` is basically the top-level script environment, and it specifies the interpreter that (&#39;I have the highest priority to be executed first&#39;).\r\n\r\n`&#39;__main__&#39;` is the name of the scope in which top-level code executes. A module’s `__name__` is set equal to `&#39;__main__&#39;` when read from standard input, a script, or from an interactive prompt.\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        # Execute only if run as a script\r\n        main()\r\n",
        },
        {
          "owner" {
            "reputation" 987,
            "display_name" "Janarthanan Ramu"
          },
          "is_accepted" false,
          "score" 16,
          "last_activity_date" 1549494966,
          "answer_id" 37965772,
          "body_markdown" "Consider:\r\n\r\n    print __name__\r\n\r\nThe output for the above is `__main__`.\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n      print &quot;direct method&quot;\r\n\r\nThe above statement is true and prints *&quot;direct method&quot;*. Suppose if they imported this class in another class it doesn&#39;t print *&quot;direct method&quot;* because, while importing, it will set `__name__ equal to &quot;first model name&quot;`.\r\n",
        },
        {
          "owner" {
            "reputation" 1029,
            "display_name" "joechoj"
          },
          "is_accepted" false,
          "score" 74,
          "last_activity_date" 1527114572,
          "answer_id" 39761460,
          "body_markdown" "There are lots of different takes here on the mechanics of the code in question, the &quot;How&quot;, but for me none of it made sense until I understood the &quot;Why&quot;. This should be especially helpful for new programmers.\r\n\r\nTake file &quot;ab.py&quot;:\r\n\r\n    def a():\r\n        print(&#39;A function in ab file&#39;);\r\n    a()\r\n\r\nAnd a second file &quot;xy.py&quot;:\r\n\r\n    import ab\r\n    def main():\r\n        print(&#39;main function: this is where the action is&#39;)\r\n    def x():\r\n        print (&#39;peripheral task: might be useful in other projects&#39;)\r\n    x()\r\n    if __name__ == &quot;__main__&quot;:\r\n        main()\r\n\r\n&gt; What is this code actually doing?\r\n\r\nWhen you execute `xy.py`, you `import ab`. The import statement runs the module immediately on import, so `ab`&#39;s operations get executed before the remainder of `xy`&#39;s. Once finished with `ab`, it continues with `xy`.\r\n\r\nThe interpreter keeps track of which scripts are running with `__name__`. When you run a script - no matter what you&#39;ve named it - the interpreter calls it `&quot;__main__&quot;`, making it the master or &#39;home&#39; script that gets returned to after running an external script.\r\n\r\nAny other script that&#39;s called from this `&quot;__main__&quot;` script is assigned its filename as its `__name__` (e.g., `__name__ == &quot;ab.py&quot;`). Hence, the line `if __name__ == &quot;__main__&quot;:` is the interpreter&#39;s test to determine if it&#39;s interpreting/parsing the &#39;home&#39; script that was initially executed, or if it&#39;s temporarily peeking into another (external) script. This gives the programmer flexibility to have the script behave differently if it&#39;s executed directly vs. called externally.\r\n\r\nLet&#39;s step through the above code to understand what&#39;s happening, focusing first on the unindented lines and the order they appear in the scripts. Remember that function - or `def` - blocks don&#39;t do anything by themselves until they&#39;re called. What the interpreter might say if mumbled to itself:\r\n\r\n- Open xy.py as the &#39;home&#39; file; call it `&quot;__main__&quot;` in the `__name__` variable.\r\n- Import and open file with the `__name__ == &quot;ab.py&quot;`.\r\n- Oh, a function. I&#39;ll remember that.\r\n- Ok, function `a()`; I just learned that. Printing &#39;*A function in ab file*&#39;.\r\n- End of file; back to `&quot;__main__&quot;`!\r\n- Oh, a function. I&#39;ll remember that.\r\n- Another one.\r\n- Function `x()`; ok, printing &#39;*peripheral task: might be useful in other projects*&#39;.\r\n- What&#39;s this? An `if` statement. Well, the condition has been met (the variable `__name__` has been set to `&quot;__main__&quot;`), so I&#39;ll enter the `main()` function and print &#39;*main function: this is where the action is*&#39;.\r\n\r\nThe bottom two lines mean: &quot;If this is the `&quot;__main__&quot;` or &#39;home&#39; script, execute the function called `main()`&quot;. That&#39;s why you&#39;ll see a `def main():` block up top, which contains the main flow of the script&#39;s functionality.\r\n\r\n&gt; Why implement this?\r\n\r\nRemember what I said earlier about import statements? When you import a module it doesn&#39;t just &#39;recognize&#39; it and wait for further instructions - it actually runs all the executable operations contained within the script. So, putting the meat of your script into the `main()` function effectively quarantines it, putting it in isolation so that it won&#39;t immediately run when imported by another script.\r\n\r\nAgain, there will be exceptions, but common practice is that `main()` doesn&#39;t usually get called externally. So you may be wondering one more thing: if we&#39;re not calling `main()`, why are we calling the script at all? It&#39;s because many people structure their scripts with standalone functions that are built to be run independent of the rest of the code in the file. They&#39;re then later called somewhere else in the body of the script. Which brings me to this:\r\n\r\n&gt; But the code works without it\r\n\r\nYes, that&#39;s right. These separate functions **can** be called from an in-line script that&#39;s not contained inside a `main()` function. If you&#39;re accustomed (as I am, in my early learning stages of programming) to building in-line scripts that do exactly what you need, and you&#39;ll try to figure it out again if you ever need that operation again ... well, you&#39;re not used to this kind of internal structure to your code, because it&#39;s more complicated to build and it&#39;s not as intuitive to read.\r\n\r\nBut that&#39;s a script that probably can&#39;t have its functions called externally, because if it did it would immediately start calculating and assigning variables. And chances are if you&#39;re trying to re-use a function, your new script is related closely enough to the old one that there will be conflicting variables.\r\n\r\nIn splitting out independent functions, you gain the ability to re-use your previous work by calling them into another script. For example, &quot;example.py&quot; might import &quot;xy.py&quot; and call `x()`, making use of the &#39;x&#39; function from &quot;xy.py&quot;. (Maybe it&#39;s capitalizing the third word of a given text string; creating a NumPy array from a list of numbers and squaring them; or detrending a 3D surface. The possibilities are limitless.)\r\n\r\n(As an aside, [this question](https://stackoverflow.com/questions/23000075/purpose-of-if-name-main) contains an answer by @kindall that finally helped me to understand - the why, not the how. Unfortunately it&#39;s been marked as a duplicate of [this one](https://stackoverflow.com/questions/419163/what-does-if-name-main-do), which I think is a mistake.)\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 7646,
                "display_name" "Wolf"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1515675593,
              "post_id" 40057173,
              "comment_id" 83396232,
              "body_markdown" "The C/C++ illustration is wrong: 3 times the same unit name (*file1*).",
            }
          ],
          "owner" {
            "reputation" 1733,
            "display_name" "redbandit"
          },
          "is_accepted" false,
          "score" 48,
          "last_activity_date" 1527114499,
          "answer_id" 40057173,
          "body_markdown" "Put simply, `__name__` is a variable defined for each script that defines whether the script is being run as the main module or it is being run as an imported module.\r\n\r\nSo if we have two scripts;\r\n\r\n    #script1.py\r\n    print &quot;Script 1&#39;s name: {}&quot;.format(__name__)\r\n\r\nand\r\n\r\n    #script2.py\r\n    import script1\r\n    print &quot;Script 2&#39;s name: {}&quot;.format(__name__)\r\n\r\nThe output from executing script1 is\r\n\r\n    Script 1&#39;s name: __main__\r\n\r\nAnd the output from executing script2 is:\r\n\r\n    Script1&#39;s name is script1\r\n    Script 2&#39;s name: __main__\r\n\r\nAs you can see, `__name__` tells us which code is the &#39;main&#39; module.\r\nThis is great, because you can just write code and not have to worry about structural issues like in C/C++, where, if a file does not implement a &#39;main&#39; function then it cannot be compiled as an executable and if it does, it cannot then be used as a library.\r\n\r\nSay you write a Python script that does something great and you implement a boatload of functions that are useful for other purposes. If I want to use them I can just import your script and use them without executing your program (given that your code only executes within the  `if __name__ == &quot;__main__&quot;:` context). Whereas in C/C++ you would have to portion out those pieces into a separate module that then includes the file. Picture the situation below;\r\n\r\n[![Complicated importing in C][1]][1]\r\n\r\nThe arrows are import links. For three modules each trying to include the previous modules code there are six files (nine, counting the implementation files) and five links. This makes it difficult to include other code into a C project unless it is compiled specifically as a library. Now picture it for Python:\r\n\r\n[![Elegant importing in Python][2]][2]\r\n\r\nYou write a module, and if someone wants to use your code they just import it and the `__name__` variable can help to separate the executable portion of the program from the library part.\r\n\r\n  [1]: https://i.stack.imgur.com/hWLqr.png\r\n  [2]: https://i.stack.imgur.com/Eql0u.png\r\n",
        },
        {
          "owner" {
            "reputation" 4728,
            "display_name" "Inconnu"
          },
          "is_accepted" false,
          "score" 27,
          "last_activity_date" 1527114636,
          "answer_id" 40881975,
          "body_markdown" "I think it&#39;s best to break the answer in depth and in simple words:\r\n\r\n`__name__`: Every module in Python has a special attribute called `__name__`.\r\nIt is a built-in variable that returns the name of the module.\r\n\r\n`__main__`: Like other programming languages, Python too has an execution entry point, i.e., main. `&#39;__main__&#39;` *is the name of the scope in which top-level code executes*. Basically you have two ways of using a Python module: Run it directly as a script, or import it. When a module is run as a script, its `__name__` is set to `__main__`.\r\n\r\nThus, the value of the `__name__` attribute is set to `__main__` when the module is run as the main program. Otherwise the value of `__name__`  is set to contain the name of the module.\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 9121,
            "display_name" "kgf3JfUtW"
          },
          "is_accepted" false,
          "score" 16,
          "last_activity_date" 1489441466,
          "answer_id" 42773985,
          "body_markdown" "&gt; You can make the file usable as a **script** as well as an **importable module**.\r\n\r\n**fibo.py (a module named `fibo`)**\r\n\r\n    # Other modules can IMPORT this MODULE to use the function fib\r\n    def fib(n):    # write Fibonacci series up to n\r\n        a, b = 0, 1\r\n        while b &lt; n:\r\n            print(b, end=&#39; &#39;)\r\n            a, b = b, a+b\r\n        print()\r\n\r\n    # This allows the file to be used as a SCRIPT\r\n    if __name__ == &quot;__main__&quot;:\r\n        import sys\r\n        fib(int(sys.argv[1]))\r\n\r\nReference: https://docs.python.org/3.5/tutorial/modules.html",
        },
        {
          "owner" {
            "reputation" 982,
            "display_name" "Larry"
          },
          "is_accepted" false,
          "score" 34,
          "last_activity_date" 1527114702,
          "answer_id" 45824951,
          "body_markdown" "Consider:\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        main()\r\n\r\nIt checks if the `__name__` attribute of the Python script is `&quot;__main__&quot;`. In other words, if the program itself is executed, the attribute will be `__main__`, so the program will be executed (in this case the `main()` function).\r\n\r\nHowever, if your Python script is used by a module, any code outside of the `if` statement will be executed, so `if \\__name__ == &quot;\\__main__&quot;` is used just to check if the program is used as a module or not, and therefore decides whether to run the code.\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 7646,
                "display_name" "Wolf"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1515675998,
              "post_id" 46371154,
              "comment_id" 83396488,
              "body_markdown" "Good to learn about *import lock*. Could you please explain *sign on to a methodology that [...]* part a little bit more?",
            },
            {
              "owner" {
                "reputation" 2516,
                "display_name" "personal_cloud"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1523665580,
              "post_id" 46371154,
              "comment_id" 86669044,
              "body_markdown" "@Wolf: Sure. I&#39;ve added a few sentences about the multiple entry points methodology.",
            }
          ],
          "owner" {
            "reputation" 2516,
            "display_name" "personal_cloud"
          },
          "is_accepted" false,
          "score" 15,
          "last_activity_date" 1524085559,
          "answer_id" 46371154,
          "body_markdown" "The reason for\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        main()\r\n\r\nis primarily to avoid the [import lock](https://docs.python.org/2/library/threading.html#importing-in-threaded-code) problems that would arise from [having code directly imported](https://stackoverflow.com/questions/46326059/). You want `main()` to run if your file was directly invoked (that&#39;s the `__name__ == &quot;__main__&quot;` case), but if your code was imported then the importer has to enter your code from the true main module to avoid import lock problems.\r\n\r\nA side-effect is that you automatically sign on to a methodology that supports multiple entry points. You can run your program using `main()` as the entry point, _but you don&#39;t have to_. While `setup.py` expects `main()`, other tools use alternate entry points. For example, to run your file as a `gunicorn` process, you define an `app()` function instead of a `main()`. Just as with `setup.py`, `gunicorn` imports your code so you don&#39;t want it do do anything while it&#39;s being imported (because of the import lock issue).",
        },
        {
          "owner" {
            "reputation" 717,
            "display_name" "DARK_C0D3R"
          },
          "is_accepted" false,
          "score" 3,
          "last_activity_date" 1527114854,
          "answer_id" 48153234,
          "body_markdown" "Create a file, **a.py**:\r\n\r\n    print(__name__) # It will print out __main__\r\n\r\n`__name__` is always equal to `__main__` whenever that file is **run directly** showing that this is the main file.\r\n\r\nCreate another file, **b.py**, in the same directory:\r\n\r\n    import a  # Prints a\r\n\r\nRun it. It will print **a**, i.e., the name of the file which **is imported**.\r\n\r\nSo, to show **two different behavior of the same file**, this is a commonly used trick:\r\n\r\n    # Code to be run when imported into another python file\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n        # Code to be run only when run directly\r\n",
        },
        {
          "owner" {
            "reputation" 8285,
            "display_name" "Simon"
          },
          "is_accepted" false,
          "score" 29,
          "last_activity_date" 1527115149,
          "answer_id" 49637838,
          "body_markdown" "*Before explaining anything about `if __name__ == &#39;__main__&#39;` it is important to understand what `__name__` is and what it does.*\r\n\r\n&gt;**What is `__name__`?**\r\n\r\n`__name__` is a [DunderAlias][5] - can be thought of as a global variable (accessible from modules) and works in a similar way to [`global`][1].\r\n\r\nIt is a string (global as mentioned above) as indicated by `type(__name__)` (yielding `&lt;class &#39;str&#39;&gt;`), and is an inbuilt standard for both [Python 3][2] and [Python 2][3] versions.\r\n\r\n&gt;**Where:**\r\n\r\nIt can not only be used in scripts but can also be found in both the interpreter and modules/packages.  \r\n\r\n**Interpreter:**\r\n\r\n    &gt;&gt;&gt; print(__name__)\r\n    __main__\r\n    &gt;&gt;&gt;\r\n\r\n**Script:**\r\n\r\n*test_file.py*:\r\n\r\n    print(__name__)\r\n\r\nResulting in `__main__`\r\n\r\n**Module or package:**\r\n \r\n*somefile.py:*\r\n\r\n    def somefunction():\r\n        print(__name__)\r\n\r\n*test_file.py:*\r\n\r\n    import somefile\r\n    somefile.somefunction()\r\n\r\nResulting in `somefile`\r\n\r\nNotice that when used in a package or module, `__name__` takes the name of the file.  The path of the actual module or package path is not given, but has its own DunderAlias `__file__`, that allows for this.\r\n\r\nYou should see that, where `__name__`, where it is the main file (or program) will *always* return `__main__`, and if it is a module/package, or anything that is running off some other Python script, will return the name of the file where it has originated from.\r\n\r\n&gt;**Practice:**\r\n\r\nBeing a variable means that it&#39;s value *can* be overwritten (&quot;can&quot; does not mean &quot;should&quot;), overwriting the value of `__name__` will result in a lack of readability.  So do not do it, for any reason.  If you need a variable define a new variable.\r\n\r\nIt is always assumed that the value of `__name__` to be `__main__` or the name of the file.  Once again changing this default value will cause more confusion that it will do good, causing problems further down the line.\r\n\r\n*example:*\r\n\r\n    &gt;&gt;&gt; __name__ = &#39;Horrify&#39; # Change default from __main__\r\n    &gt;&gt;&gt; if __name__ == &#39;Horrify&#39;: print(__name__)\r\n    ...\r\n    &gt;&gt;&gt; else: print(&#39;Not Horrify&#39;)\r\n    ...\r\n    Horrify\r\n    &gt;&gt;&gt;\r\n\r\nIt is considered good practice in general to include the `if __name__ == &#39;__main__&#39;` in scripts.\r\n\r\n&gt;**Now to answer `if __name__ == &#39;__main__&#39;`:**\r\n\r\n*Now we know the behaviour of `__name__` things become clearer:*\r\n\r\nAn [`if`][4] is a flow control statement that contains the block of code will execute if the value given is true. We have seen that `__name__` can take either \r\n`__main__` or the file name it has been imported from.  \r\n\r\nThis means that if `__name__` is equal to `__main__` then the file must be the main file and must actually be running (or it is the interpreter), not a module or package imported into the script.\r\n\r\nIf indeed `__name__` does take the value of `__main__` then whatever is in that block of code will execute.\r\n\r\nThis tells us that if the file running is the main file (or you are running from the interpreter directly) then that condition must execute.  If it is a package then it should not, and the value will not be `__main__`.\r\n\r\n&gt;**Modules:**\r\n\r\n`__name__` can also be used in modules to define the name of a module\r\n\r\n&gt;**Variants:**  \r\n\r\nIt is also possible to do other, less common but useful things with `__name__`, some I will show here:\r\n\r\n**Executing only if the file is a module or package:**\r\n\r\n    if __name__ != &#39;__main__&#39;:\r\n        # Do some useful things \r\n\r\n**Running one condition if the file is the main one and another if it is not:**\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n        # Execute something\r\n    else:\r\n        # Do some useful things\r\n\r\nYou can also use it to provide runnable help functions/utilities on packages and modules without the elaborate use of libraries.\r\n\r\nIt also allows modules to be run from the command line as main scripts, which can be also very useful.\r\n\r\n\r\n\r\n[1]:https://stackoverflow.com/questions/13881395/in-python-what-is-a-global-statement\r\n[2]:https://docs.python.org/3/library/__main__.html\r\n[3]:https://docs.python.org/2/library/__main__.html\r\n[4]:https://docs.python.org/3/tutorial/controlflow.html#if-statements\r\n[5]:https://wiki.python.org/moin/DunderAlias",
        },
        {
          "owner" {
            "reputation" 1548,
            "display_name" "Ali Hallaji"
          },
          "is_accepted" false,
          "score" 4,
          "last_activity_date" 1527115011,
          "answer_id" 49653760,
          "body_markdown" "# if __name__ == &#39;__main__&#39;:\r\n\r\nWe see if `__name__ == &#39;__main__&#39;:` quite often.\r\n\r\nIt checks if a module is being imported or not.\r\n\r\nIn other words, the code within the `if` block will be executed only when the code runs directly. Here `directly` means `not imported`.\r\n\r\nLet&#39;s see what it does using a simple code that prints the name of the module:\r\n\r\n    # test.py\r\n    def test():\r\n       print(&#39;test module name=%s&#39; %(__name__))\r\n\r\n    if __name__ == &#39;__main__&#39;:\r\n       print(&#39;call test()&#39;)\r\n       test()\r\n\r\nIf we run the code directly via `python test.py`, the module name is `__main__`:\r\n\r\n    call test()\r\n    test module name=__main__\r\n",
        },
        {
          "owner" {
            "reputation" 141,
            "display_name" "preetika mondal"
          },
          "is_accepted" false,
          "score" 4,
          "last_activity_date" 1527115136,
          "answer_id" 50170459,
          "body_markdown" "All the answers have pretty much explained the functionality. But I will provide one example of its usage which might help clearing out the concept further.\r\n\r\nAssume that you have two Python files, a.py and b.py. Now, a.py imports b.py. We run the a.py file, where the &quot;import b.py&quot; code is executed first. Before the rest of the a.py code runs, the code in the file b.py must run completely.\r\n\r\nIn the b.py code there is some code that is exclusive to that file b.py and we don&#39;t want any other file (other than b.py file), that has imported the b.py file, to run it.\r\n\r\nSo that is what this line of code checks. If it is the main file (i.e., b.py) running the code, which in this case it is not (a.py is the main file running), then only the code gets executed.\r\n",
        },
        {
          "owner" {
            "reputation" 657,
            "display_name" "pah8J"
          },
          "is_accepted" false,
          "score" 6,
          "last_activity_date" 1529408669,
          "answer_id" 50927616,
          "body_markdown" "If this .py file are imported by other .py files, the code under &quot;the if statement&quot; will not be executed.\r\n\r\nIf this .py are run by `python this_py.py` under shell, or double clicked in Windows. the code under &quot;the if statement&quot; will be executed.\r\n\r\nIt is usually written for testing.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1643,
                "display_name" "Eureka"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1553462178,
              "post_id" 51011507,
              "comment_id" 97383066,
              "body_markdown" "Yes, point 1 is vital to understand. From that, the need for this mechanism become clear.",
            },
            {
              "owner" {
                "reputation" 647,
                "display_name" "allenwang"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1592611829,
              "post_id" 51011507,
              "comment_id" 110497007,
              "body_markdown" "For me, point 1 and point 4 clear up my confusion.",
            }
          ],
          "owner" {
            "reputation" 632,
            "display_name" "jack"
          },
          "is_accepted" false,
          "score" 28,
          "last_activity_date" 1597011592,
          "answer_id" 51011507,
          "body_markdown" "I&#39;ve been reading so much throughout the answers on this page. I would say, if you know the thing, for sure you will understand those answers, otherwise, you are still confused.\r\n\r\nTo be short, you need to know several points:\r\n\r\n1. `import a` action actually runs all that can be ran in `a.py`, meaning each line in `a.py`\r\n\r\n2. Because of point 1, you may not want everything to be run in `a.py` when importing it\r\n\r\n3. To solve the problem in point 2, python allows you to put a condition check\r\n\r\n4. `__name__` is an implicit variable in all `.py` modules:\r\n- when `a.py` is `import`ed, the value of `__name__` of `a.py` module is set to its file name &quot;`a`&quot;\r\n- when `a.py` is run directly using &quot;`python a.py`&quot;, the value of `__name__` is set to a string `__main__`\r\n\r\n5. Based on the mechanism how python sets the variable `__name__` for each module, do you know how to achieve point 3? The answer is fairly easy, right? Put a if condition: `if __name__ == &quot;__main__&quot;: // do A`\r\n- then `python a.py` will run the part `// do A`\r\n- and `import a` will skip the part `// do A`\r\n6. You can even put if `__name__ == &quot;a&quot;` depending on your functional need, but rarely do\r\n\r\nThe important thing that python is special at is point 4! The rest is just basic logic.",
        },
        {
          "owner" {
            "reputation" 806,
            "display_name" "Raja"
          },
          "is_accepted" false,
          "score" 10,
          "last_activity_date" 1539832172,
          "answer_id" 52685565,
          "body_markdown" "This answer is for Java programmers learning Python.\r\nEvery Java file typically contains one public class. You can use that class in two ways: \r\n\r\n1. Call the class from other files. You just have to import it in the calling program.\r\n\r\n2. Run the class stand alone, for testing purposes. \r\n\r\nFor the latter case, the class should contain a public static void main() method. In Python this purpose is served by the globally defined label `&#39;__main__&#39;`.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 402,
                "display_name" "arredond"
              },
              "score" 11,
              "post_type" "answer",
              "creation_date" 1550839477,
              "post_id" 54506767,
              "comment_id" 96429485,
              "body_markdown" "This answer makes the assumption that the OP (or any user with a similar question) is both familiar with _C_ *and* knows what an entry point is.",
            },
            {
              "owner" {
                "reputation" 519,
                "display_name" "Charlie Harding"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1554845739,
              "post_id" 54506767,
              "comment_id" 97899150,
              "body_markdown" "This answer also assumes that no code (other than definitions without side effects) take place before the `if __name__ == &quot;__main__&quot;` block. Technically the top of the script executed is the entry point of the program.",
            }
          ],
          "owner" {
            "reputation" 727,
            "display_name" "Mohammed Awney"
          },
          "is_accepted" false,
          "score" 0,
          "last_activity_date" 1581077037,
          "answer_id" 54506767,
          "body_markdown" "Simply, it is the entry point to run the file, like the `main` function in the *C* programming language.",
        },
        {
          "owner" {
            "reputation" 2237,
            "display_name" "Rishi Bansal"
          },
          "is_accepted" false,
          "score" 11,
          "last_activity_date" 1577144632,
          "answer_id" 56558865,
          "body_markdown" "Every module in python has a attribute called `__name__`. The value of `__name__`  attribute is  `__main__`  when the module is run directly, like `python my_module.py`. Otherwise (like when you say `import my_module`) the value of `__name__`  is the name of the module.\r\n\r\nSmall example to explain in short.\r\n\r\n    #Script test.py\r\n    \r\n    apple = 42\r\n     \r\n    def hello_world():\r\n        print(&quot;I am inside hello_world&quot;)\r\n     \r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;Value of __name__ is: &quot;, __name__)\r\n\t    print(&quot;Going to call hello_world&quot;)\r\n        hello_world()\r\n\t\r\nWe can execute this directly as\r\n\r\n    python test.py\t\r\n\t\r\nOutput\r\n\r\n    Value of __name__ is: __main__\r\n    Going to call hello_world\r\n    I am inside hello_world\r\n\r\nNow suppose we call above script from other script\r\n\r\n    #script external_calling.py\r\n    \r\n    import test\r\n    print(test.apple)\r\n    test.hello_world()\r\n    \r\n    print(test.__name__)\r\nWhen you execute this\r\n\r\n    python external_calling.py\r\n\r\nOutput\r\n\r\n    42\r\n    I am inside hello_world\r\n    test\r\n\r\nSo, above is self explanatory that when you call test from other script, if loop `__name__` in `test.py` will not execute.",
        },
        {
          "owner" {
            "reputation" 93,
            "display_name" "Nikil Munireddy"
          },
          "is_accepted" false,
          "score" 5,
          "last_activity_date" 1564503735,
          "answer_id" 57276038,
          "body_markdown" "If the python interpreter is running&#160;a particular module then `__name__` global  variable will have value `&quot;__main__&quot;`\n\n      def a():\n          print(&quot;a&quot;)\n      def b():\n          print(&quot;b&quot;)\n\n      if __name__ == &quot;__main__&quot;: \n    \n    &#160;&#160;      &#160;&#160;print (&quot;you can see me&quot; )\n              a()\n      else: \n    \n    &#160;&#160;&#160;      &#160;print (&quot;You can&#39;t see me&quot;)\n              b()\n\nWhen you run this script prints **you can see me** \n\n**a**\n\nIf you import this file say A to file B  and execute the file B then `if __name__ == &quot;__main__&quot;` in file A becomes false, so it prints  **You can&#39;t see me**\n\n**b**\n",
        },
        {
          "owner" {
            "reputation" 928,
            "display_name" "Igor Micev"
          },
          "is_accepted" false,
          "score" 0,
          "last_activity_date" 1580493208,
          "answer_id" 60008967,
          "body_markdown" "You can checkup for the special variable `__name__` with this simple example:\r\n\r\ncreate file1.py\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;file1 is being run directly&quot;)\r\n    else:\r\n        print(&quot;file1 is being imported&quot;)\r\n\r\ncreate file2.py\r\n\r\n    import file1 as f1\r\n    \r\n    print(&quot;__name__ from file1: {}&quot;.format(f1.__name__))\r\n    print(&quot;__name__ from file2: {}&quot;.format(__name__))\r\n    \r\n    if __name__ == &quot;__main__&quot;:\r\n        print(&quot;file2 is being run directly&quot;)\r\n    else:\r\n        print(&quot;file2 is being imported&quot;)\r\n\r\nExecute file2.py\r\n\r\n*output*:\r\n\r\n    file1 is being imported\r\n    __name__ from file1: file1\r\n    __name__ from file2: __main__\r\n    file2 is being run directly\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 20645,
            "display_name" "Giorgos Myrianthous"
          },
          "is_accepted" false,
          "score" 17,
          "last_activity_date" 1607000770,
          "answer_id" 60017299,
          "body_markdown" "The code under `if __name__ == &#39;__main__&#39;:` **will be executed only if the module is invoked as a script**. \r\n\r\nAs an example consider the following module `my_test_module.py`: \r\n\r\n\r\n    # my_test_module.py\r\n\r\n    print(&#39;This is going to be printed out, no matter what&#39;)\r\n    \r\n    if __name__ == &#39;__main__&#39;:\r\n        print(&#39;This is going to be printed out, only if user invokes the module as a script&#39;)\r\n\r\n\r\n----------\r\n\r\n\r\n**1st possibility: Import `my_test_module.py` in another module**\r\n\r\n    # main.py\r\n    \r\n    import my_test_module\r\n    \r\n    if __name__ == &#39;__main__&#39;:\r\n        print(&#39;Hello from main.py&#39;)\r\n\r\nNow if you invoke `main.py`: \r\n\r\n    python main.py \r\n    \r\n    &gt;&gt; &#39;This is going to be printed out, no matter what&#39;\r\n    &gt;&gt; &#39;Hello from main.py&#39;\r\n\r\n\r\nNote that only the top-level `print()` statement in `my_test_module` is executed. \r\n\r\n\r\n----------\r\n\r\n\r\n**2nd possibility: Invoke `my_test_module.py` as a script**\r\n\r\n\r\nNow if you run `my_test_module.py` as a Python script, both `print()` statements will be exectued: \r\n\r\n    python my_test_module.py\r\n\r\n    &gt;&gt;&gt; &#39;This is going to be printed out, no matter what&#39;\r\n    &gt;&gt;&gt; &#39;This is going to be printed out, only if user invokes the module as a script&#39;\r\n\r\n\r\n----------\r\nFor a more comprehensive explanation you can [read this blog post][1]. \r\n\r\n\r\n  [1]: https://towardsdatascience.com/what-does-if-name-main-do-e357dd61be1a",
        },
        {
          "owner" {
            "reputation" 589,
            "display_name" "mamal"
          },
          "is_accepted" false,
          "score" 1,
          "last_activity_date" 1587469733,
          "answer_id" 61342829,
          "body_markdown" "**PYTHON MAIN FUNCTION** is a starting point of any program. When the program is run, the python interpreter runs the code sequentially. Main function is executed only when it is run as a Python program ... \r\n\r\n    def main():\r\n         print (&quot;i am in the function&quot;)\r\n    print (&quot;i am out of function&quot;)\r\nwhen you run script show : \r\n\r\n    i am out of function\r\n\r\nand not the code &quot;i am in the function&quot;\r\nIt is because we did not declare the call function &quot;if__name__== &quot;__main__&quot;.\r\nif you use from it :\r\n\r\n    def main():\r\n         print (&quot;i am in the function&quot;)\r\n    \r\n    \r\n    if __name__ == &quot;__main__&quot;:\r\n        main()\r\n    \r\n    print (&quot;i am out of function&quot;)\r\n\r\nThe output is equal to\r\n\r\n    i am in the function\r\n    i am out of function\r\n\r\nIn Python &quot;if__name__== &quot;__main__&quot; allows you to run the Python files either as reusable modules or standalone programs.\r\n\r\nWhen Python interpreter reads a source file, it will execute all the code found in it.\r\nWhen Python runs the &quot;source file&quot; as the main program, it sets the special variable (__name__) to have a value (&quot;__main__&quot;).\r\n\r\nWhen you execute the main function, it will then read the &quot;if&quot; statement and checks whether __name__ does equal to __main__.\r\n\r\nIn Python &quot;if__name__== &quot;__main__&quot; allows you to run the Python files either as reusable modules or standalone programs.",
        },
        {
          "owner" {
            "reputation" 416,
            "display_name" "Reinstate C0MMUNiSM"
          },
          "is_accepted" false,
          "score" 3,
          "last_activity_date" 1589298956,
          "answer_id" 61458664,
          "body_markdown" "Every module in Python has a special attribute called __name__. The value of __name__  attribute is set to &#39;__main__&#39;  when the module is executed as  the main program (e.g. running `python foo.py`). Otherwise, the value of __name__  is set to the name of the module that it was called from.",
        },
        {
          "owner" {
            "reputation" 97,
            "display_name" "Mustapha Babatunde"
          },
          "is_accepted" false,
          "score" 1,
          "last_activity_date" 1603389660,
          "answer_id" 64488013,
          "body_markdown" "In  simple words:\r\n\r\nThe code you see under  `if __name__ == &quot;__main__&quot;:` will only get called upon when your python file is executed as &quot;python example1.py&quot;.\r\n\r\nHowever, if you wish to import your python file &#39;example1.py&#39; as a module to work with another python file say &#39;example2.py&#39;, the code under `if __name__ == &quot;__main__&quot;:` will not run or take any effect.\r\n\r\n",
        }
      ],
      "owner" {
        "reputation" 86963,
        "display_name" "Devoted"
      },
      "view_count" 3253147,
      "score" 6513,
      "last_activity_date" 1607000770,
      "question_id" 419163,
      "body_markdown" "Given the following code, what does the `if __name__ == &quot;__main__&quot;:` do?\r\n\r\n    # Threading example\r\n    import time, thread\r\n    \r\n    def myfunction(string, sleeptime, lock, *args):\r\n        while True:\r\n            lock.acquire()\r\n            time.sleep(sleeptime)\r\n            lock.release()\r\n            time.sleep(sleeptime)\r\n\r\n    if __name__ == &quot;__main__&quot;:\r\n        lock = thread.allocate_lock()\r\n        thread.start_new_thread(myfunction, (&quot;Thread #: 1&quot;, 2, lock))\r\n        thread.start_new_thread(myfunction, (&quot;Thread #: 2&quot;, 2, lock))\r\n",
      "title" "What does if __name__ == &quot;__main__&quot;: do?"
    },
    {
      "tags" [
        "python",
        "operators",
        "ternary-operator",
        "conditional-operator"
      ],
      "comments" [
        {
          "owner" {
            "reputation" 1174,
            "display_name" "ジョージ"
          },
          "score" 4,
          "post_type" "question",
          "creation_date" 1306370886,
          "post_id" 394809,
          "comment_id" 33335746,
          "body_markdown" "Though Pythons older than 2.5 are slowly drifting to history, here is a list of old pre-2.5 ternary operator tricks: [&quot;Python Idioms&quot;, search for the text &#39;Conditional expression&#39;](http://c2.com/cgi/wiki?PythonIdioms) .\n[Wikipedia](http://en.wikipedia.org/wiki/Ternary_operation#Python) is also quite helpful Ж:-)",
        },
        {
          "owner" {
            "reputation" 38029,
            "display_name" "Brent Bradburn"
          },
          "score" 155,
          "post_type" "question",
          "creation_date" 1357797450,
          "post_id" 394809,
          "comment_id" 19777451,
          "body_markdown" "In the Python 3.0 official documentation referenced in a comment above, this is referred to as &quot;conditional_expressions&quot; and is very cryptically defined.  That documentation doesn&#39;t even include the term &quot;ternary&quot;, so you would be hard-pressed to find it via Google unless you knew exactly what to look for.  The [version 2 documentation](http://docs.python.org/2/reference/expressions.html#conditional-expressions) is somewhat more helpful and includes a link to [&quot;PEP 308&quot;](http://www.python.org/dev/peps/pep-0308/), which includes a lot of interesting historical context related to this question.",
        },
        {
          "owner" {
            "reputation" 261,
            "display_name" "user313114"
          },
          "score" 29,
          "post_type" "question",
          "creation_date" 1418678064,
          "post_id" 394809,
          "comment_id" 43418120,
          "body_markdown" "&quot;ternary&quot; (having three inputs) is a consequential property of this impelmentation, not a defining property of the concept. eg:  SQL has `case [...] { when ... then ...} [ else ... ] end` for a similar effect but not at all ternary.",
        },
        {
          "owner" {
            "reputation" 261,
            "display_name" "user313114"
          },
          "score" 10,
          "post_type" "question",
          "creation_date" 1418678422,
          "post_id" 394809,
          "comment_id" 43418299,
          "body_markdown" "also ISO/IEC 9899 (the C programming language standard) section 6.5.15 calls it the &quot;the condtitional operator&quot;",
        },
        {
          "owner" {
            "reputation" 2577,
            "display_name" "HelloGoodbye"
          },
          "score" 9,
          "post_type" "question",
          "creation_date" 1465459863,
          "post_id" 394809,
          "comment_id" 62912948,
          "body_markdown" "Wikipedia covers this thoroughly in the article &quot;[?:](https://en.wikipedia.org/wiki/%3F:#Python)&quot;.",
        },
        {
          "owner" {
            "reputation" 284,
            "display_name" "sdaffa23fdsf"
          },
          "score" 1,
          "post_type" "question",
          "creation_date" 1521001211,
          "post_id" 394809,
          "comment_id" 85542543,
          "body_markdown" "It is mentioned here https://docs.python.org/3/faq/programming.html#is-there-an-equivalent-of-c-s-ternary-operator, but not mentioned in Python Standard Library",
        },
        {
          "owner" {
            "reputation" 730,
            "display_name" "Scott Martin"
          },
          "score" 10,
          "post_type" "question",
          "creation_date" 1534339507,
          "post_id" 394809,
          "comment_id" 90671337,
          "body_markdown" "In the years since nobar&#39;s comment the [conditional expression documentation](https://docs.python.org/3/reference/expressions.html#conditional-expressions) has been updated to say _Conditional expressions (sometimes called a “ternary operator”)..._",
        },
        {
          "owner" {
            "reputation" 407,
            "display_name" "Kaan E."
          },
          "score" 1,
          "post_type" "question",
          "creation_date" 1598065087,
          "post_id" 394809,
          "comment_id" 112344289,
          "body_markdown" "I sometimes wonder how it is possible that a yes or no question gets 26 answers",
        },
        {
          "owner" {
            "reputation" 4311,
            "display_name" "mins"
          },
          "score" 0,
          "post_type" "question",
          "creation_date" 1603644830,
          "post_id" 394809,
          "comment_id" 114095743,
          "body_markdown" "@KaanE. and most of all being copy-paste of each other with different comments. So the value is in the comments... Anyway as of 2020 the comprehensive answer is in the [&#39;Ternary Operators&#39;](https://book.pythontips.com/en/latest/ternary_operators.html) documentation: &quot;*Ternary operators are more commonly known as conditional expressions [...] they became a part of Python in version 2.4.*&quot;",
        }
      ],
      "answers" [
        {
          "comments" [
            {
              "owner" {
                "reputation" 1372,
                "display_name" "yota"
              },
              "score" 290,
              "post_type" "answer",
              "creation_date" 1453734428,
              "post_id" 394814,
              "comment_id" 57719599,
              "body_markdown" "The order may seems strange for coders however `f(x) = |x| = x if x &gt; 0 else -x` sounds very natural to mathematicians. You may also understand it as do A in most case, except when C then you should do B instead...",
            },
            {
              "owner" {
                "reputation" 1064,
                "display_name" "Kal Zekdor"
              },
              "score" 134,
              "post_type" "answer",
              "creation_date" 1457256191,
              "post_id" 394814,
              "comment_id" 59317179,
              "body_markdown" "Be careful with order of operations when using this. For example, the line `z = 3 + x if x &lt; y else y`. If `x=2` and `y=1`, you might expect that to yield 4, but it would actually yield 1. `z = 3 + (x if x &gt; y else y)` is the correct usage.",
            },
            {
              "owner" {
                "reputation" 1064,
                "display_name" "Kal Zekdor"
              },
              "score" 13,
              "post_type" "answer",
              "creation_date" 1460680565,
              "post_id" 394814,
              "comment_id" 60867668,
              "body_markdown" "The point was if you want to perform additional evaluations *after* the conditional is evaluated, like adding a value to the result, you&#39;ll either need to add the additional expression to both sides (`z = 3 + x if x &lt; y else 3 + y`), or group the conditional (`z = 3 + (x if x &lt; y else y)` or `z = (x if x &lt; y else y) + 3`)",
            },
            {
              "owner" {
                "reputation" 18685,
                "display_name" "MrGeek"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1495812704,
              "post_id" 394814,
              "comment_id" 75423088,
              "body_markdown" "what if there are multiple conditions ?",
            },
            {
              "owner" {
                "reputation" 647,
                "display_name" "Dimesio"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1502258913,
              "post_id" 394814,
              "comment_id" 78124651,
              "body_markdown" "@MrGeek, you could group the boolean expressions.\n&quot;foo&quot; if (bool or bool &amp;&amp; bool or etc) else &quot;bar&quot;",
            },
            {
              "owner" {
                "reputation" 18685,
                "display_name" "MrGeek"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1502270275,
              "post_id" 394814,
              "comment_id" 78131735,
              "body_markdown" "@Dimesio I meant something like if (c1) a1 elif (c2) a2 elif ... else a(n).",
            },
            {
              "owner" {
                "reputation" 647,
                "display_name" "Dimesio"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1502409872,
              "post_id" 394814,
              "comment_id" 78209851,
              "body_markdown" "@MrGeek, I see what you mean, so you would basically be nesting the operations:\n` &quot;foo&quot; if Bool else (&quot;bar&quot; if Bool else &quot;foobar&quot;) `",
            },
            {
              "owner" {
                "reputation" 668,
                "display_name" "Albert van der Horst"
              },
              "score" 4,
              "post_type" "answer",
              "creation_date" 1529239852,
              "post_id" 394814,
              "comment_id" 88793385,
              "body_markdown" "Programmers need precise correct formulation even more than mathematician, because in mathematics there is always a resort to underlying concepts. A convincing argument  is the % operator, mimicking the way &quot;mod&quot; is used in math would have been a disaster.  So no, I don&#39;t accept your argument. It is like adhering to imperial units. Groetjes Albert",
            },
            {
              "owner" {
                "reputation" 466,
                "display_name" "BeMyGuestPlease"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1567954938,
              "post_id" 394814,
              "comment_id" 102113536,
              "body_markdown" "For the expression: &#39;x and y&#39; and &#39;x or y&#39; and their return values I recommend checking https://docs.python.org/3/reference/expressions.html#boolean-operations and also the other post: https://stackoverflow.com/questions/3181901/python-boolean-expression-and-or/3181946#3181946",
            },
            {
              "owner" {
                "reputation" 3697,
                "display_name" "Baldrickk"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1570715106,
              "post_id" 394814,
              "comment_id" 103007398,
              "body_markdown" "@KalZekdor I read that and expected 1... It took me a few seconds to see how you had to read it to make 4 a possibility.",
            },
            {
              "owner" {
                "reputation" 1003,
                "display_name" "QtRoS"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1580383085,
              "post_id" 394814,
              "comment_id" 106083253,
              "body_markdown" "Go doesn&#39;t have ternary operator, bro.",
            },
            {
              "owner" {
                "reputation" 3083,
                "display_name" "Gerard ONeill"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1583796939,
              "post_id" 394814,
              "comment_id" 107229324,
              "body_markdown" "Nice explanation, but short circuiting doesn&#39;t mean what you think it means - it means that the rest of the *condition* isn&#39;t evaluated if the &#39;truthiness&#39; of the entire condition can be decided early. Statement &#39;b&#39; is never evaluated even in languages without short circuiting.  Ex: if (arr != null &amp;&amp; arr[0] = 1)...",
            }
          ],
          "owner" {
            "reputation" 239309,
            "display_name" "Vinko Vrsalovic"
          },
          "is_accepted" true,
          "score" 7416,
          "last_activity_date" 1596671534,
          "answer_id" 394814,
          "body_markdown" "Yes, it was [added][1] in version 2.5. The expression syntax is:\r\n```python\r\na if condition else b\r\n```\r\nFirst `condition` is evaluated, then exactly one of either `a` or `b` is evaluated and returned based on the [Boolean][4] value of `condition`. If `condition` evaluates to `True`, then `a` is evaluated and returned but `b` is ignored, or else when `b` is evaluated and returned but `a` is ignored.\r\n\r\nThis allows short-circuiting because when `condition` is true only `a` is evaluated and `b` is not evaluated at all, but when `condition` is false only `b` is evaluated and `a` is not evaluated at all.\r\n\r\nFor example:\r\n```python\r\n&gt;&gt;&gt; &#39;true&#39; if True else &#39;false&#39;\r\n&#39;true&#39;\r\n&gt;&gt;&gt; &#39;true&#39; if False else &#39;false&#39;\r\n&#39;false&#39;\r\n```\r\nNote that conditionals are an _expression_, not a _statement_. This means you can&#39;t use assignment statements or `pass` or other **statements** within a conditional **expression**:\r\n```python\r\n&gt;&gt;&gt; pass if False else x = 3\r\n  File &quot;&lt;stdin&gt;&quot;, line 1\r\n    pass if False else x = 3\r\n          ^\r\nSyntaxError: invalid syntax\r\n```\r\nYou can, however, use conditional expressions to assign a variable like so:\r\n```python\r\nx = a if True else b\r\n```\r\nThink of the conditional expression as switching between two values. It is very useful when you&#39;re in a &#39;one value or another&#39; situation, it but doesn&#39;t do much else.\r\n\r\nIf you need to use statements, you have to use a normal `if` **statement** instead of a conditional **expression**.\r\n\r\n----------\r\n\r\n\r\nKeep in mind that it&#39;s frowned upon by some Pythonistas for several reasons:\r\n\r\n- The order of the arguments is different from those of the classic `condition ? a : b` ternary operator from many other languages (such as C, C++, Go, Perl, Ruby, Java, Javascript, etc.), which may lead to bugs when people unfamiliar with Python&#39;s &quot;surprising&quot; behaviour use it (they may reverse the argument order).\r\n- Some find it &quot;unwieldy&quot;, since it goes contrary to the normal flow of thought (thinking of the condition first and then the effects).\r\n- Stylistic reasons. (Although the &#39;inline `if`&#39; can be *really* useful, and make your script more concise, it really does complicate your code)\r\n\r\nIf you&#39;re having trouble remembering the order, then remember that when read aloud, you (almost) say what you mean. For example, `x = 4 if b &gt; 8 else 9` is read aloud as `x will be 4 if b is greater than 8 otherwise 9`.\r\n\r\nOfficial documentation:     \r\n\r\n- [Conditional expressions][2]\r\n- [Is there an equivalent of C’s ”?:” ternary operator?][3]\r\n\r\n\r\n  [1]: https://mail.python.org/pipermail/python-dev/2005-September/056846.html &quot;[Python-Dev] Conditional Expression Resolution&quot;\r\n  [2]: https://docs.python.org/3/reference/expressions.html#conditional-expressions &quot;Conditional expressions&quot;\r\n  [3]: https://docs.python.org/3/faq/programming.html#is-there-an-equivalent-of-c-s-ternary-operator &quot;Is there an equivalent of C’s ”?:” ternary operator?&quot;\r\n  [4]: https://en.wikipedia.org/wiki/Boolean_data_type &quot;Boolean data type&quot;",
        },
        {
          "owner" {
            "reputation" 307294,
            "display_name" "Michael Burr"
          },
          "is_accepted" false,
          "score" 168,
          "last_activity_date" 1445067833,
          "answer_id" 394815,
          "body_markdown" "From [the documentation]:\r\n\r\n&gt; Conditional expressions (sometimes called a “ternary operator”) have the lowest priority of all Python operations.\r\n&gt; \r\n&gt; The expression `x if C else y` first evaluates the condition, *C* (*not x*); if *C* is true, *x* is evaluated and its value is returned; otherwise, *y* is evaluated and its value is returned.\r\n&gt; \r\n&gt; See [PEP 308] for more details about conditional expressions.\r\n\r\nNew since version 2.5.\r\n\r\n  [the documentation]: https://docs.python.org/3/reference/expressions.html#conditional-expressions &quot;Conditional expressions&quot;\r\n  [PEP 308]: https://www.python.org/dev/peps/pep-0308/ &quot;PEP 308 -- Conditional Expressions&quot;",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 18350,
                "display_name" "ThomasH"
              },
              "score" 70,
              "post_type" "answer",
              "creation_date" 1256139239,
              "post_id" 394887,
              "comment_id" 1466794,
              "body_markdown" "The remedy is to use (test and [true_value] or [false_value])[0], which avoids this trap.",
            },
            {
              "owner" {
                "reputation" 3254,
                "display_name" "volcano"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1389599528,
              "post_id" 394887,
              "comment_id" 31716349,
              "body_markdown" "Ternary operator usually executes faster(sometimes by 10-25%).",
            },
            {
              "owner" {
                "reputation" 9535,
                "display_name" "OrangeTux"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1407241833,
              "post_id" 394887,
              "comment_id" 39130413,
              "body_markdown" "@volcano Do you have source for me?",
            },
            {
              "owner" {
                "reputation" 2909,
                "display_name" "mbomb007"
              },
              "score" 4,
              "post_type" "answer",
              "creation_date" 1521493194,
              "post_id" 394887,
              "comment_id" 85745604,
              "body_markdown" "@OrangeTux [Here&#39;s the disassembled code](https://tio.run/##bcoxCoAwEETRPqeY0kCwsPQ2kmR1QZOwWQtPH4OClQO/GV65dMtpao2PkkURuBoTIoEG76AOZGeDPol6SoKCCR5xrxH0yvVfeiwpdJ/lgVzH3kDWFOGk37Ha1m4). Using the method ThomasH suggested would be even slower.",
            }
          ],
          "owner" {
            "reputation" 21176,
            "display_name" "James Brady"
          },
          "is_accepted" false,
          "score" 362,
          "last_activity_date" 1389597408,
          "answer_id" 394887,
          "body_markdown" "For versions prior to 2.5, there&#39;s the trick:\r\n\r\n    [expression] and [on_true] or [on_false]\r\n\r\nIt can give wrong results when `on_true` \r\n has a false boolean value.&lt;sup&gt;1&lt;/sup&gt;  \r\nAlthough it does have the benefit of evaluating expressions left to right, which is clearer in my opinion.\r\n\r\n&lt;sub&gt;1. [Is there an equivalent of C’s ”?:” ternary operator?][1]&lt;/sub&gt;\r\n\r\n[1]: http://docs.python.org/3.3/faq/programming.html#is-there-an-equivalent-of-c-s-ternary-operator",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1994,
                "display_name" "SilverbackNet"
              },
              "score" 617,
              "post_type" "answer",
              "creation_date" 1296786324,
              "post_id" 470376,
              "comment_id" 5445970,
              "body_markdown" "Note that this one always evaluates everything, whereas the if/else construct only evaluates the winning expression.",
            },
            {
              "owner" {
                "reputation" 19582,
                "display_name" "Dustin Getz"
              },
              "score" 126,
              "post_type" "answer",
              "creation_date" 1331235108,
              "post_id" 470376,
              "comment_id" 12212868,
              "body_markdown" "`(lambda: print(&quot;a&quot;), lambda: print(&quot;b&quot;))[test==true]()`",
            },
            {
              "owner" {
                "reputation" 93454,
                "display_name" "martineau"
              },
              "score" 17,
              "post_type" "answer",
              "creation_date" 1338488415,
              "post_id" 470376,
              "comment_id" 14113335,
              "body_markdown" "It should be noted that what&#39;s within the `[]`s can be an arbitrary expression. Also, for safety you can explicitly test for truthiness by writing `[bool(&lt;expression&gt;)]`. The `bool()` function has been around since v2.2.1.",
            },
            {
              "owner" {
                "reputation" 2205,
                "display_name" "jskulski"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1432846634,
              "post_id" 470376,
              "comment_id" 49108946,
              "body_markdown" "Is this idiomatic in python? Seems confusing but maybe its convention",
            },
            {
              "owner" {
                "reputation" 2052,
                "display_name" "Dr. Drew"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1455868190,
              "post_id" 470376,
              "comment_id" 58691508,
              "body_markdown" "Ugly is in the eye of the beholder, and I don&#39;t find this ugly at all.  It concisely make elegant use of the fact that bool is a subclass of int and that Python indexes are 0-based.  Admittedly, it&#39;s probably not the most efficient (as @SilverBackNet mentioned, both options are eval&#39;d).  However, this works perfectly for deciding between 1 of 2 strings as @Claudiu said - I use it for this all the time.  For example: `&#39;%d item%s to process!&#39;%(num_items,(&#39;&#39;,&#39;s&#39;)[num_items &gt; 1])` or `&#39;Null hypothesis %s be rejected (p-val = %0.4f)&#39;%((&quot;can&#39;t&quot;,&#39;must&#39;)[pval&lt;alpha],pval)`.",
            },
            {
              "owner" {
                "reputation" 1557,
                "display_name" "JDM"
              },
              "score" 12,
              "post_type" "answer",
              "creation_date" 1456857817,
              "post_id" 470376,
              "comment_id" 59136440,
              "body_markdown" "I&#39;ve done a similar trick -- only once or twice, but done it -- by indexing into a dictionary with `True` and `False` as the keys:  `{True:trueValue, False:falseValue}[test]`  I don&#39;t know whether this is any less efficient, but it does at least avoid the whole &quot;elegant&quot; vs. &quot;ugly&quot; debate.  There&#39;s no ambiguity that you&#39;re dealing with a boolean rather than an int.",
            },
            {
              "owner" {
                "reputation" 2026,
                "display_name" "Natecat"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1462230656,
              "post_id" 470376,
              "comment_id" 61540187,
              "body_markdown" "[comparisons to singletons should always use is/is not instead of ==](https://www.python.org/dev/peps/pep-0008/#programming-recommendations)",
            },
            {
              "owner" {
                "reputation" 423,
                "display_name" "Breezer"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1511963973,
              "post_id" 470376,
              "comment_id" 82065817,
              "body_markdown" "This trick may help avoid timing based attacks on algorithms if it always evaluates both possible results and avoids skipping code (an &#39;if&#39; skips).",
            },
            {
              "owner" {
                "reputation" 3697,
                "display_name" "Baldrickk"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1570715325,
              "post_id" 470376,
              "comment_id" 103007535,
              "body_markdown" "@jskulski only in old python, before 2.5.  Even then though, `if cond: [expression_1] else: [expression_2]` was more common",
            },
            {
              "owner" {
                "reputation" 1342,
                "display_name" "problemofficer"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1600856209,
              "post_id" 470376,
              "comment_id" 113217979,
              "body_markdown" "It&#39;s a &quot;cool&quot; and interesting idea, no doubt. But real-life code should be easy to read and least error-prone. If one of my developers used this I would ask him to change it. Since IMHO this should not be used in production code, I have down voted the answer.",
            },
            {
              "owner" {
                "reputation" 263749,
                "display_name" "Mark Ransom"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1603144932,
              "post_id" 470376,
              "comment_id" 113938181,
              "body_markdown" "@JDM there&#39;s no ambiguity since `bool` is actually a subclass of `int`.  And I&#39;d argue that this answer and your variant of it are *both* ugly.",
            }
          ],
          "owner" {
            "reputation" 60176,
            "display_name" "Landon Kuhn"
          },
          "is_accepted" false,
          "score" 840,
          "last_activity_date" 1445067310,
          "answer_id" 470376,
          "body_markdown" "You can index into a tuple:\r\n\r\n    (falseValue, trueValue)[test]\r\n\r\n`test` needs to return _True_ or _False_.  \r\nIt might be safer to always implement it as:\r\n\r\n    (falseValue, trueValue)[test == True]\r\n\r\nor you can use the built-in [`bool()`][2] to assure a [Boolean][1] value:\r\n\r\n    (falseValue, trueValue)[bool(&lt;expression&gt;)]\r\n\r\n[1]: https://en.wikipedia.org/wiki/Boolean_data_type &quot;Boolean data type&quot;\r\n[2]: https://docs.python.org/3.3/library/functions.html#bool &quot;bool&quot;",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1931,
                "display_name" "Perkins"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1539279291,
              "post_id" 1855173,
              "comment_id" 92454573,
              "body_markdown" "While the tuple of lambdas trick works, it takes roughly 3x as long as the ternary operator.  It&#39;s only likely to be a reasonable idea if it can replace a long chain of `if else if`.",
            }
          ],
          "owner" {
            "reputation" 1262,
            "display_name" "gorsky"
          },
          "is_accepted" false,
          "score" 101,
          "last_activity_date" 1557395133,
          "answer_id" 1855173,
          "body_markdown" "Unfortunately, the\r\n\r\n    (falseValue, trueValue)[test]\r\n\r\nsolution doesn&#39;t have short-circuit behaviour; thus both `falseValue` and `trueValue` are evaluated regardless of the condition. This could be suboptimal or even buggy (i.e. both `trueValue` and `falseValue` could be methods and have side-effects).\r\n\r\nOne solution to this would be\r\n\r\n    (lambda: falseValue, lambda: trueValue)[test]()\r\n\r\n(execution delayed until the winner is known ;)), but it introduces inconsistency between callable and non-callable objects. In addition, it doesn&#39;t solve the case when using properties.\r\n\r\nAnd so the story goes - choosing between 3 mentioned solutions is a trade-off between having the short-circuit feature, using at least Зython 2.5 (IMHO not a problem anymore) and not being prone to &quot;`trueValue`-evaluates-to-false&quot; errors.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 9869,
                "display_name" "Roy Tinker"
              },
              "score" 87,
              "post_type" "answer",
              "creation_date" 1286226861,
              "post_id" 2919360,
              "comment_id" 4103349,
              "body_markdown" "This one emphasizes the primary intent of the ternary operator: value selection. It also shows that more than one ternary can be chained together into a single expression.",
            },
            {
              "owner" {
                "reputation" 1576,
                "display_name" "Jon Coombs"
              },
              "score" 6,
              "post_type" "answer",
              "creation_date" 1417469451,
              "post_id" 2919360,
              "comment_id" 42951997,
              "body_markdown" "@Craig , I agree, but it&#39;s also helpful to know what will happen when there are no parentheses. In real code, I too would tend to insert explicit parens.",
            },
            {
              "owner" {
                "reputation" 4311,
                "display_name" "mins"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1603644061,
              "post_id" 2919360,
              "comment_id" 114095475,
              "body_markdown" "Use: `return 3 if t &gt; 10 else t/2`",
            }
          ],
          "owner" {
            "reputation" 3368,
            "display_name" "Simon Zimmermann"
          },
          "is_accepted" false,
          "score" 288,
          "last_activity_date" 1557932621,
          "answer_id" 2919360,
          "body_markdown" "&lt;code&gt; &lt;i&gt;&amp;lt;expression 1&amp;gt;&lt;/i&gt; &lt;b&gt;if&lt;/b&gt; &lt;i&gt;&amp;lt;condition&amp;gt;&lt;/i&gt; &lt;b&gt;else&lt;/b&gt; &lt;i&gt;&amp;lt;expression 2&amp;gt;&lt;/i&gt; &lt;/code&gt;\r\n\r\n```python\r\na = 1\r\nb = 2\r\n\r\n1 if a &gt; b else -1 \r\n# Output is -1\r\n\r\n1 if a &gt; b else -1 if a &lt; b else 0\r\n# Output is -1\r\n```",
        },
        {
          "comments" [
            {
              "owner" {
                "display_name" "user3317"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1348650569,
              "post_id" 10314837,
              "comment_id" 16979616,
              "body_markdown" "The behaviour is not identical - `q(&quot;blob&quot;, on_true, on_false)` returns `on_false`, whereas `on_true if cond else on_false` returns `on_true`. A workaround is to replace `cond` with `cond is not None` in these cases, although that is not a perfect solution.",
            },
            {
              "owner" {
                "reputation" 7001,
                "display_name" "Jonas K&#246;lker"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1384186266,
              "post_id" 10314837,
              "comment_id" 29624617,
              "body_markdown" "Why not `bool(cond)` instead of `cond is True`?  The former checks the truthiness of `cond`, the latter checks for pointer-equality with the `True` object.  As highlighted by @AndrewCecil, `&quot;blob&quot;` is truthy but it `is not True`.",
            },
            {
              "owner" {
                "reputation" 4754,
                "display_name" "Arseny"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1393242668,
              "post_id" 10314837,
              "comment_id" 33319520,
              "body_markdown" "Wow, that looks really hacky! :)\nTechnically, you can even write `[on_false, on_True][cond is True]` so the expression becomes shorter.",
            },
            {
              "owner" {
                "reputation" 561,
                "display_name" "Hucker"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1553782100,
              "post_id" 10314837,
              "comment_id" 97519631,
              "body_markdown" "There is no short circuit in this answer.  If on_true and on_false are expensive to call this is a bad answer.",
            }
          ],
          "owner" {
            "reputation" 15383,
            "display_name" "Paolo"
          },
          "is_accepted" false,
          "score" 65,
          "last_activity_date" 1335355326,
          "answer_id" 10314837,
          "body_markdown" "For Python 2.5 and newer there is a specific syntax:\r\n\r\n    [on_true] if [cond] else [on_false]\r\n\r\nIn older Pythons a ternary operator is not implemented but it&#39;s possible to simulate it.\r\n\r\n    cond and on_true or on_false\r\n\r\nThough, there is a potential problem, which if `cond` evaluates to `True` and `on_true` evaluates to `False` then `on_false` is returned instead of `on_true`. If you want this behavior the method is OK, otherwise use this:\r\n\r\n    {True: on_true, False: on_false}[cond is True] # is True, not == True\r\n\r\nwhich can be wrapped by:\r\n\r\n    def q(cond, on_true, on_false)\r\n        return {True: on_true, False: on_false}[cond is True]\r\n\r\nand used this way:\r\n\r\n    q(cond, on_true, on_false)\r\n\r\nIt is compatible with all Python versions.\r\n\r\n  [1]: http://docs.python.org/faq/programming.html#is-there-an-equivalent-of-c-s-ternary-operator",
        },
        {
          "owner" {
            "reputation" 21,
            "display_name" "Benoit Bertholon"
          },
          "is_accepted" false,
          "score" 46,
          "last_activity_date" 1358178969,
          "answer_id" 14321907,
          "body_markdown" "You might often find\r\n\r\n    cond and on_true or on_false\r\n\r\nbut this lead to problem when on_true == 0\r\n\r\n    &gt;&gt;&gt; x = 0\r\n    &gt;&gt;&gt; print x == 0 and 0 or 1 \r\n    1\r\n    &gt;&gt;&gt; x = 1\r\n    &gt;&gt;&gt; print x == 0 and 0 or 1 \r\n    1\r\n\r\n\r\nwhere you would expect for a  normal ternary operator this result\r\n\r\n    &gt;&gt;&gt; x = 0\r\n    &gt;&gt;&gt; print 0 if x == 0 else 1 \r\n    0\r\n    &gt;&gt;&gt; x = 1\r\n    &gt;&gt;&gt; print 0 if x == 0 else 1 \r\n    1\r\n\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 51810,
                "display_name" "Grijesh Chauhan"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1388123406,
              "post_id" 20093702,
              "comment_id" 31175220,
              "body_markdown" "Why not simply `result = (y, x)[a &lt; b]` Why do you uses `lambda` function **?**",
            },
            {
              "owner" {
                "reputation" 79620,
                "display_name" "glglgl"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1392279273,
              "post_id" 20093702,
              "comment_id" 32895313,
              "body_markdown" "@GrijeshChauhan Because on &quot;compliated&quot; expressions, e. g. involving a function call etc., this would be executed in both cases. This might not be wanted.",
            },
            {
              "owner" {
                "reputation" 476,
                "display_name" "jocerfranquiz"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1607920188,
              "post_id" 20093702,
              "comment_id" 115414421,
              "body_markdown" "The use of `lambda` functions is an overkill for this question",
            }
          ],
          "owner" {
            "reputation" 1319,
            "display_name" "Sasikiran Vaddi"
          },
          "is_accepted" false,
          "score" 23,
          "last_activity_date" 1384944252,
          "answer_id" 20093702,
          "body_markdown" "Simulating the python ternary operator.\r\n\r\nFor example\r\n\r\n    a, b, x, y = 1, 2, &#39;a greather than b&#39;, &#39;b greater than a&#39;\r\n    result = (lambda:y, lambda:x)[a &gt; b]()\r\n\r\noutput:\r\n\r\n    &#39;b greater than a&#39;\r\n\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 10404,
                "display_name" "Walter Tross"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1549735660,
              "post_id" 30052371,
              "comment_id" 96013109,
              "body_markdown" "`result = {1: x, 0: y}[a &gt; b]` is another possible variant (`True` and `False` are actually integers with values `1` and `0`)",
            }
          ],
          "owner" {
            "reputation" 110267,
            "display_name" "kenorb"
          },
          "is_accepted" false,
          "score" 130,
          "last_activity_date" 1502115752,
          "answer_id" 30052371,
          "body_markdown" "An operator for a conditional expression in Python was added in 2006 as part of [Python Enhancement Proposal 308][1]. Its form differ from common `?:` operator and it&#39;s:\r\n\r\n    &lt;expression1&gt; if &lt;condition&gt; else &lt;expression2&gt;\r\n\r\nwhich is equivalent to:\r\n\r\n    if &lt;condition&gt;: &lt;expression1&gt; else: &lt;expression2&gt;\r\n\r\nHere is an example:\r\n\r\n    result = x if a &gt; b else y\r\n\r\nAnother syntax which can be used (compatible with versions before 2.5):\r\n\r\n    result = (lambda:y, lambda:x)[a &gt; b]()\r\n\r\nwhere operands are [lazily evaluated][2].\r\n\r\nAnother way is by indexing a tuple (which isn&#39;t consistent with the conditional operator of most other languages):\r\n\r\n    result = (y, x)[a &gt; b]\r\n\r\nor explicitly constructed dictionary:\r\n\r\n    result = {True: x, False: y}[a &gt; b]\r\n\r\nAnother (less reliable), but simpler method is to use `and` and `or` operators:\r\n\r\n    result = (a &gt; b) and x or y\r\n\r\nhowever this won&#39;t work if `x` would be `False`.\r\n\r\nA possible workaround is to make `x` and `y` lists or tuples as in the following:\r\n\r\n    result = ((a &gt; b) and [x] or [y])[0]\r\n\r\nor:\r\n\r\n    result = ((a &gt; b) and (x,) or (y,))[0]\r\n\r\nIf you&#39;re working with dictionaries, instead of using a ternary conditional, you can take advantage of [`get(key, default)`][3], for example:\r\n\r\n    shell = os.environ.get(&#39;SHELL&#39;, &quot;/bin/sh&quot;)\r\n\r\n&lt;sup&gt;Source: [?: in Python at Wikipedia][4]&lt;/sup&gt;\r\n\r\n\r\n  [1]: https://www.python.org/dev/peps/pep-0308/\r\n  [2]: https://en.wikipedia.org/wiki/Lazy_evaluation\r\n  [3]: https://docs.python.org/3/library/stdtypes.html#dict.get\r\n  [4]: https://en.wikipedia.org/wiki/%3F:#Python",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 3629,
                "display_name" "JSDBroughton"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1455800759,
              "post_id" 33765206,
              "comment_id" 58658010,
              "body_markdown" "`expression1 or expression2` being similar and with the same drawbacks/positives as `expression1 || expression2` in javascript",
            },
            {
              "owner" {
                "reputation" 270870,
                "display_name" "Aaron Hall"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1464323859,
              "post_id" 33765206,
              "comment_id" 62448054,
              "body_markdown" "Thanks, @selurvedu - it can be confusing until you get it straight. I learned the hard way, so your way might not be as hard. ;) Using if without the else, at the end of a generator expression or list comprehension will filter the iterable. In the front, it&#39;s a ternary conditional operation, and requires the else. Cheers!!",
            },
            {
              "owner" {
                "reputation" 74053,
                "display_name" "tchrist"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1548511967,
              "post_id" 33765206,
              "comment_id" 95571762,
              "body_markdown" "@AaronHall Although your use of metasyntactic `expressionN` for all instances is consistent, it might be easier to understand with naming that distinguished the conditional test expression from the two result expressions; eg, `result1 if condition else result2`. This is especially evident when nesting (aka chaining): `result1 if condition1 else result2 if condition2 else result3`. See how much better that reads this way?",
            },
            {
              "owner" {
                "reputation" 270870,
                "display_name" "Aaron Hall"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1548527061,
              "post_id" 33765206,
              "comment_id" 95575895,
              "body_markdown" "@tchrist thanks for the review - if you look at the revision history, this post currently has two revisions. Most of my other answers, especially the top ones, have been revisited again and again. This answer never gets my attention because the community wiki status gives me no credit for the content, and so I never see any votes on it. As I don&#39;t really have time for an edit on this right now, frog knows when it will come to my attention again in the future. I can see you&#39;ve edited the top answer, so feel free to borrow/quote my material from this post in that one (and cite me if apropos!)",
            }
          ],
          "owner" {
            "reputation" 270870,
            "display_name" "Aaron Hall"
          },
          "is_accepted" false,
          "score" 41,
          "last_activity_date" 1470682591,
          "answer_id" 33765206,
          "body_markdown" "&gt; # Does Python have a ternary conditional operator?\r\n\r\n\r\nYes. From the [grammar file][1]:\r\n\r\n    test: or_test [&#39;if&#39; or_test &#39;else&#39; test] | lambdef\r\n\r\nThe part of interest is:\r\n\r\n    or_test [&#39;if&#39; or_test &#39;else&#39; test]\r\n\r\nSo, a ternary conditional operation is of the form:\r\n\r\n    expression1 if expression2 else expression3\r\n\r\n`expression3` will be lazily evaluated (that is, evaluated only if `expression2` is false in a boolean context). And because of the recursive definition, you can chain them indefinitely (though it may considered bad style.)\r\n\r\n    expression1 if expression2 else expression3 if expression4 else expression5 # and so on\r\n\r\n### A note on usage:\r\n\r\nNote that every `if` must be followed with an `else`. People learning list comprehensions and generator expressions may find this to be a difficult lesson to learn - the following will not work, as Python expects a third expression for an else:\r\n\r\n    [expression1 if expression2 for element in iterable]\r\n    #                          ^-- need an else here\r\n\r\nwhich raises a `SyntaxError: invalid syntax`.\r\nSo the above is either an incomplete piece of logic (perhaps the user expects a no-op in the false condition) or what may be intended is to use expression2 as a filter - notes that the following is legal Python:\r\n\r\n    [expression1 for element in iterable if expression2]\r\n\r\n`expression2` works as a filter for the list comprehension, and is *not* a ternary conditional operator.\r\n\r\n### Alternative syntax for a more narrow case:\r\n\r\nYou may find it somewhat painful to write the following:\r\n\r\n    expression1 if expression1 else expression2\r\n\r\n`expression1` will have to be evaluated twice with the above usage. It can limit redundancy if it is simply a local variable. However, a common and performant Pythonic idiom for this use-case is to use `or`&#39;s shortcutting behavior:\r\n\r\n    expression1 or expression2\r\n\r\nwhich is equivalent in semantics. Note that some style-guides may limit this usage on the grounds of clarity - it does pack a lot of meaning into very little syntax.\r\n\r\n  [1]: https://docs.python.org/reference/grammar.html",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 935,
                "display_name" "frederick99"
              },
              "score" 6,
              "post_type" "answer",
              "creation_date" 1503209237,
              "post_id" 37155553,
              "comment_id" 78515516,
              "body_markdown" "I prefer `print( &#39;yes&#39; if conditionX else &#39;nah&#39; )` over your answer. :-)",
            },
            {
              "owner" {
                "reputation" 13592,
                "display_name" "Todor Minakov"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1509018017,
              "post_id" 37155553,
              "comment_id" 80853618,
              "body_markdown" "That is if you want to `print()` in both cases - and it looks a bit more pythonic, I have to admit :) But what if the expressions/functions are not the same - like `print(&#39;yes&#39;) if conditionX else True` - to get the `print()` only in truthy `conditionX `",
            },
            {
              "owner" {
                "reputation" 20113,
                "display_name" "Thierry Lathuille"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1540158689,
              "post_id" 37155553,
              "comment_id" 92749861,
              "body_markdown" "To add to Frederick99&#39;s remark, another reason to avoid `print(&#39;yes&#39;) if conditionX else print(&#39;nah&#39;)` is that it gives a SyntaxError in Python2.",
            },
            {
              "owner" {
                "reputation" 13592,
                "display_name" "Todor Minakov"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1540181340,
              "post_id" 37155553,
              "comment_id" 92753803,
              "body_markdown" "The only reason it gives a syntax error is because in Python 2 print is a statement - `print &quot;yes&quot;`, while in Python 3 it is a function - `print(&quot;yes&quot;)`. That can be resolved by either using it as a statement, or better - `from future import print_function`.",
            }
          ],
          "owner" {
            "reputation" 13592,
            "display_name" "Todor Minakov"
          },
          "is_accepted" false,
          "score" 19,
          "last_activity_date" 1464030986,
          "answer_id" 37155553,
          "body_markdown" "More a tip than an answer (don&#39;t need to repeat the obvious for the hundreth time), but I sometimes use it as a oneliner shortcut in such constructs:\r\n\r\n    if conditionX:\r\n        print(&#39;yes&#39;)\r\n    else:\r\n        print(&#39;nah&#39;)\r\n\r\n, becomes:\r\n\r\n    print(&#39;yes&#39;) if conditionX else print(&#39;nah&#39;)\r\n\r\nSome (many :) may frown upon it as unpythonic (even, ruby-ish :), but I personally find it more natural - i.e. how you&#39;d express it normally, plus a bit more visually appealing in large blocks of code.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 3185,
                "display_name" "JamesThomasMoon1979"
              },
              "score" 15,
              "post_type" "answer",
              "creation_date" 1487200130,
              "post_id" 39067220,
              "comment_id" 71681244,
              "body_markdown" "This [blogger found python&#39;s ternary operator to be unnecessarily different than most other languages](https://archive.is/yqwSh).",
            },
            {
              "owner" {
                "reputation" 1695,
                "display_name" "fralau"
              },
              "score" 6,
              "post_type" "answer",
              "creation_date" 1515604348,
              "post_id" 39067220,
              "comment_id" 83365814,
              "body_markdown" "It may sound opinionated; but what it essentially says is that it the Python syntax is likely to be understood by a person who never saw a ternary operator, while very few people will understand the more usual syntax unless they have been told first what it means.",
            },
            {
              "owner" {
                "reputation" 668,
                "display_name" "Albert van der Horst"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1529240129,
              "post_id" 39067220,
              "comment_id" 88793454,
              "body_markdown" "Algol68:  a=.if. .true. .then. 1 .else. 0 .fi. This may be expressed also a=(.true.|1|0)  As usual Algol68 is an improvement over its successors.",
            },
            {
              "owner" {
                "reputation" 1718,
                "display_name" "Varun Garg"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1604565861,
              "post_id" 39067220,
              "comment_id" 114386996,
              "body_markdown" "something simple as `print a || &#39;&lt;alt text&gt;&#39;` in ruby is pita in python `print a if a is not None else &#39;alt text&#39;`",
            },
            {
              "owner" {
                "reputation" 4426,
                "display_name" "lenz"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1605471844,
              "post_id" 39067220,
              "comment_id" 114654346,
              "body_markdown" "@VarunGarg But of course you can say `print(a or &#39;alt text&#39;)` in Python.",
            }
          ],
          "owner" {
            "reputation" 1893,
            "display_name" "Simplans"
          },
          "is_accepted" false,
          "score" 78,
          "last_activity_date" 1548511518,
          "answer_id" 39067220,
          "body_markdown" "**Ternary Operator in different programming Languages**\r\n-------------------------------------------------------\r\n\r\nHere I just try to show some important difference in `ternary operator` between a couple of programming languages.\r\n\r\n&gt;*Ternary Operator in Javascript*\r\n\r\n    var a = true ? 1 : 0;\r\n    # 1\r\n    var b = false ? 1 : 0;\r\n    # 0\r\n\r\n&gt;*Ternary Operator in Ruby*\r\n\r\n    a = true ? 1 : 0\r\n    # 1\r\n    b = false ? 1 : 0\r\n    # 0\r\n\r\n&gt;*Ternary operator in Scala*\r\n\r\n    val a = true ? 1 | 0\r\n    # 1\r\n    val b = false ? 1 | 0\r\n    # 0\r\n\r\n&gt;*Ternary operator in R programming*\r\n\r\n    a &lt;- if (TRUE) 1 else 0\r\n    # 1\r\n    b &lt;- if (FALSE) 1 else 0\r\n    # 0\r\n\r\n&gt;*Ternary operator in Python*\r\n\r\n    a = 1 if True else 0\r\n    # 1\r\n    b = 1 if False else 0\r\n    # 0\r\n\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 978,
                "display_name" "moi"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1508481421,
              "post_id" 45779600,
              "comment_id" 80634562,
              "body_markdown" "If you want to use that in the context of `x = [condition] and ([expression_1] or 1) or [expression_2]` and `expression_1` evaluates to false, `x` will be `1`, not `expression_1`. Use the accepted answer.",
            }
          ],
          "owner" {
            "reputation" 6676,
            "display_name" "Natesh bhat"
          },
          "is_accepted" false,
          "score" 21,
          "last_activity_date" 1503215334,
          "answer_id" 45779600,
          "body_markdown" "you can do this :-\n\n### **`[condition] and [expression_1] or [expression_2] ;`** \nExample:-\n### `print(number%2 and &quot;odd&quot; or &quot;even&quot;)`\nThis would print &quot;odd&quot; if the number is odd or &quot;even&quot; if the number is even.\n___\n### **The result :-**  If condition is true exp_1 is executed else exp_2 is executed.\n \n\n**Note :-** 0 , None , False , emptylist , emptyString evaluates as False.\nAnd any data other than 0 evaluates to True.\n\nHere&#39;s how it works:\n----\nif the condition [condition] becomes &quot;True&quot; then , expression_1 will be evaluated but not expression_2 .\nIf we &quot;and&quot; something with 0 (zero) , the result will always to be fasle .So in the below statement ,\n\n    0 and exp\n\nThe expression exp won&#39;t be evaluated at all since &quot;and&quot; with 0 will always evaluate to zero and there is no need to evaluate the expression . This is how the compiler itself works , in all languages.\n\nIn \n\n    1 or exp\n\n \n\nthe expression exp won&#39;t be evaluated at all since &quot;or&quot; with 1 will always be 1. So it won&#39;t bother to evaluate the expression exp since the result will be 1 anyway . (compiler optimization methods). \n\nBut in case of \n\n    True and exp1 or exp2\n\nThe second expression exp2 won&#39;t be evaluated since `True and exp1` would be True when exp1 isn&#39;t false .\n\nSimilarly in \n\n    False and exp1 or exp2\nThe expression exp1 won&#39;t be evaluated since False is equivalent to writing 0 and doing &quot;and&quot; with 0 would be 0 itself but after exp1 since &quot;or&quot; is used, it will evaluate the expression exp2 after &quot;or&quot; .\n\n__________\n\n**Note:-** This kind of branching using &quot;or&quot; and &quot;and&quot; can only be used when the expression_1 doesn&#39;t have a Truth value of False (or 0 or None or emptylist [ ] or emptystring &#39; &#39;.) since if expression_1 becomes False , then the expression_2 will be evaluated because of the presence &quot;or&quot; between exp_1 and exp_2.\n\n**In case you still want to make it work for all the cases regardless of what exp_1 and exp_2 truth values are, do this :-**\n\n### `[condition] and ([expression_1] or 1) or [expression_2] ;`",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1931,
                "display_name" "Perkins"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1539278926,
              "post_id" 49653070,
              "comment_id" 92454366,
              "body_markdown" "Note that the ternary operator is smaller (in memory) and faster than the nested if.  Also, your nested `if-else` isn&#39;t actually a rewrite of the ternary operator, and will produce different output for select values of a and b (specifically if one is a type which implements a weird `__ne__` method).",
            }
          ],
          "owner" {
            "reputation" 1548,
            "display_name" "Ali Hallaji"
          },
          "is_accepted" false,
          "score" 22,
          "last_activity_date" 1522850537,
          "answer_id" 49653070,
          "body_markdown" "Ternary conditional operator simply allows testing a condition in a single line replacing the multiline if-else making the code compact.\r\n\r\n###Syntax : \r\n\r\n&gt; [on_true] if [expression] else [on_false] \r\n\r\n###1- Simple Method to use ternary operator:\r\n\r\n    # Program to demonstrate conditional operator\r\n    a, b = 10, 20\r\n    # Copy value of a in min if a &lt; b else copy b\r\n    min = a if a &lt; b else b\r\n    print(min)  # Output: 10\r\n\r\n###2- Direct Method of using tuples, Dictionary, and lambda:\r\n\r\n    # Python program to demonstrate ternary operator\r\n    a, b = 10, 20\r\n    # Use tuple for selecting an item\r\n    print( (b, a) [a &lt; b] )\r\n    # Use Dictionary for selecting an item\r\n    print({True: a, False: b} [a &lt; b])\r\n    # lamda is more efficient than above two methods\r\n    # because in lambda  we are assure that\r\n    # only one expression will be evaluated unlike in\r\n    # tuple and Dictionary\r\n    print((lambda: b, lambda: a)[a &lt; b]()) # in output you should see three 10\r\n\r\n###3- Ternary operator can be written as nested if-else:\r\n\r\n    # Python program to demonstrate nested ternary operator\r\n    a, b = 10, 20\r\n    print (&quot;Both a and b are equal&quot; if a == b else &quot;a is greater than b&quot;\r\n            if a &gt; b else &quot;b is greater than a&quot;)\r\n\r\nAbove approach can be written as:\r\n\r\n    # Python program to demonstrate nested ternary operator\r\n    a, b = 10, 20\r\n    if a != b:\r\n        if a &gt; b:\r\n            print(&quot;a is greater than b&quot;)\r\n        else:\r\n            print(&quot;b is greater than a&quot;)\r\n    else:\r\n        print(&quot;Both a and b are equal&quot;) \r\n    # Output: b is greater than a",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 1931,
                "display_name" "Perkins"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1539278030,
              "post_id" 50047083,
              "comment_id" 92453871,
              "body_markdown" "While useful for similar problems, this is not a ternary conditional.  It works to replace `x if x else y`, but not `x if z else y`.",
            }
          ],
          "owner" {
            "reputation" 15179,
            "display_name" "ewwink"
          },
          "is_accepted" false,
          "score" -2,
          "last_activity_date" 1524759778,
          "answer_id" 50047083,
          "body_markdown" "if variable is defined and you want to check if it has value you can just `a or b`\r\n\r\n    def test(myvar=None):\r\n        # shorter than: print myvar if myvar else &quot;no Input&quot;\r\n        print myvar or &quot;no Input&quot;\r\n        \r\n    test()\r\n    test([])\r\n    test(False)\r\n    test(&#39;hello&#39;)\r\n    test([&#39;Hello&#39;])\r\n    test(True)\r\n\r\nwill output\r\n\r\n    no Input\r\n    no Input\r\n    no Input\r\n    hello\r\n    [&#39;Hello&#39;]\r\n    True",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 50,
                "display_name" "realmanusharma"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1540154754,
              "post_id" 52919467,
              "comment_id" 92748969,
              "body_markdown" "I have added a one line statement example to check which number is big to elaborate it further",
            },
            {
              "owner" {
                "reputation" 20113,
                "display_name" "Thierry Lathuille"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1540158776,
              "post_id" 52919467,
              "comment_id" 92749878,
              "body_markdown" "`print` is really not a good choice, as this will give a SyntaxError in Python2.",
            },
            {
              "owner" {
                "reputation" 50,
                "display_name" "realmanusharma"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1540158861,
              "post_id" 52919467,
              "comment_id" 92749900,
              "body_markdown" "@Thierry Lathuille here I used print() function not print statement, print function is for Python 3 while print statement is for Python 2",
            },
            {
              "owner" {
                "reputation" 20113,
                "display_name" "Thierry Lathuille"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1540159103,
              "post_id" 52919467,
              "comment_id" 92749954,
              "body_markdown" "The question has already been asked on SO, just try it with Python 2 and you will see by yourself. &#39;print(&#39;hello&#39;) is a perfectly valid syntax in Python 2.7, but the way it is parsed makes your code above throw a SyntaxError.",
            }
          ],
          "owner" {
            "reputation" 50,
            "display_name" "realmanusharma"
          },
          "is_accepted" false,
          "score" 7,
          "last_activity_date" 1540154794,
          "answer_id" 52919467,
          "body_markdown" "YES, python have a ternary operator, here is the syntax and an example code to demonstrate the same :)\n\n    #[On true] if [expression] else[On false]\n    # if the expression evaluates to true then it will pass On true otherwise On false\n\n\n    a= input(&quot;Enter the First Number &quot;)\n    b= input(&quot;Enter the Second Number &quot;)\n    \n    print(&quot;A is Bigger&quot;) if a&gt;b else print(&quot;B is Bigger&quot;)",
        },
        {
          "owner" {
            "reputation" 173,
            "display_name" "shivtej"
          },
          "is_accepted" false,
          "score" 21,
          "last_activity_date" 1544107527,
          "answer_id" 53653902,
          "body_markdown" "    a if condition else b\r\n\r\nJust memorize this pyramid if you have trouble remembering:\r\n\r\n         condition\r\n      if           else\r\n    a                   b ",
        },
        {
          "owner" {
            "reputation" 22770,
            "display_name" "Andy Fedoroff"
          },
          "is_accepted" false,
          "score" 10,
          "last_activity_date" 1546639334,
          "answer_id" 53922638,
          "body_markdown" "Many programming languages derived from `C` usually have the following syntax of ternary conditional operator:\r\n\r\n    &lt;condition&gt; ? &lt;expression1&gt; : &lt;expression2&gt;\r\n\r\n&gt;At first, the `Python` **B**enevolent **D**ictator **F**or **L**ife (I mean Guido van Rossum, of course) rejected it (as non-Pythonic style), since it&#39;s quite hard to understand for people not used to `C` language. Also, the colon sign **`:`** already has many uses in `Python`. After **PEP 308** was approved, `Python` finally received its own shortcut conditional expression (what we use now):\r\n\r\n    &lt;expression1&gt; if &lt;condition&gt; else &lt;expression2&gt;\r\n\r\nSo, firstly it evaluates the condition. If it returns `True`, **expression1** will be evaluated to give the result, otherwise **expression2** will be evaluated. Due to ***Lazy Evaluation*** mechanics – only one expression will be executed.\r\n\r\nHere are some examples (conditions will be evaluated from left to right):\r\n\r\n    pressure = 10\r\n    print(&#39;High&#39; if pressure &lt; 20 else &#39;Critical&#39;)\r\n    \r\n    # Result is &#39;High&#39;\r\n\r\nTernary operators can be chained in series:     \r\n\r\n    pressure = 5\r\n    print(&#39;Normal&#39; if pressure &lt; 10 else &#39;High&#39; if pressure &lt; 20 else &#39;Critical&#39;)\r\n\r\n    # Result is &#39;Normal&#39;\r\n\r\nThe following one is the same as previous one:\r\n\r\n    pressure = 5\r\n\r\n    if pressure &lt; 20:\r\n        if pressure &lt; 10:\r\n            print(&#39;Normal&#39;)\r\n        else:\r\n            print(&#39;High&#39;)\r\n    else:\r\n        print(&#39;Critical&#39;)\r\n\r\n    # Result is &#39;Normal&#39;\r\n\r\nHope this helps.",
        },
        {
          "owner" {
            "reputation" 10404,
            "display_name" "Walter Tross"
          },
          "is_accepted" false,
          "score" 20,
          "last_activity_date" 1582376615,
          "answer_id" 54609267,
          "body_markdown" "One of the alternatives to Python&#39;s [conditional expression][1]\r\n\r\n    &quot;yes&quot; if boolean else &quot;no&quot;\r\n\r\nis the following:\r\n\r\n    {True:&quot;yes&quot;, False:&quot;no&quot;}[boolean]\r\n\r\nwhich has the following nice extension:\r\n\r\n    {True:&quot;yes&quot;, False:&quot;no&quot;, None:&quot;maybe&quot;}[boolean_or_none]\r\n\r\nThe shortest alternative remains:\r\n\r\n    (&quot;no&quot;, &quot;yes&quot;)[boolean]\r\n\r\nbut there is no alternative to\r\n\r\n    yes() if boolean else no()\r\n\r\nif you want to avoid the evaluation of `yes()` _and_ `no()`, because in\r\n\r\n    (no(), yes())[boolean]  # bad\r\n\r\nboth `no()` and `yes()` are evaluated.\r\n\r\n\r\n  [1]: https://mail.python.org/pipermail/python-dev/2005-September/056846.html",
        },
        {
          "owner" {
            "reputation" 3315,
            "display_name" "Yaakov Bressler"
          },
          "is_accepted" false,
          "score" -1,
          "last_activity_date" 1557666228,
          "answer_id" 56099511,
          "body_markdown" "*A neat way to chain multiple operators:*\r\n\r\n```\r\nf = lambda x,y: &#39;greater&#39; if x &gt; y else &#39;less&#39; if y &gt; x else &#39;equal&#39;\r\n\r\narray = [(0,0),(0,1),(1,0),(1,1)]\r\n\r\nfor a in array:\r\n  x, y = a[0], a[1]\r\n  print(f(x,y))\r\n\r\n# Output is:\r\n#   equal,\r\n#   less,\r\n#   greater,\r\n#   equal\r\n",
        },
        {
          "owner" {
            "reputation" 1284,
            "display_name" "Frank"
          },
          "is_accepted" false,
          "score" 12,
          "last_activity_date" 1571215046,
          "answer_id" 58409100,
          "body_markdown" "As already answered, yes there is a ternary operator in python:\r\n\r\n    &lt;expression 1&gt; if &lt;condition&gt; else &lt;expression 2&gt;\r\n\r\nAdditional information:\r\n\r\nIf `&lt;expression 1&gt;` is the condition you can use [Short-cirquit evaluation][1]:\r\n\r\n```python\r\na = True\r\nb = False\r\n\r\n# Instead of this:\r\nx = a if a else b\r\n\r\n# You could use Short-cirquit evaluation:\r\nx = a or b\r\n```\r\nPS: Of course, a Short-cirquit evaluation is not a ternary operator but often the ternary is used in cases where the short circuit would be enough.\r\n\r\n  [1]: https://en.wikipedia.org/wiki/Short-circuit_evaluation",
        },
        {
          "owner" {
            "reputation" 286,
            "display_name" "That&#39;s Enam"
          },
          "is_accepted" false,
          "score" -3,
          "last_activity_date" 1571815378,
          "answer_id" 58517534,
          "body_markdown" "    is_spacial=True if gender = &quot;Female&quot; else (True if age &gt;= 65 else False)\r\n\r\n**\r\n\r\n&gt; it can be nested as your need. best of luck\r\n\r\n**",
        },
        {
          "owner" {
            "reputation" 2316,
            "display_name" "Todd"
          },
          "is_accepted" false,
          "score" 3,
          "last_activity_date" 1584254539,
          "answer_id" 60630600,
          "body_markdown" "Python has a ternary form for assignments; however there may be even a shorter form that people should be aware of.\r\n\r\nIt&#39;s very common to need to assign to a variable one value or another depending on a condition.\r\n\r\n    &gt;&gt;&gt; li1 = None\r\n    &gt;&gt;&gt; li2 = [1, 2, 3]\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; if li1:\r\n    ...     a = li1\r\n    ... else:\r\n    ...     a = li2\r\n    ...     \r\n    &gt;&gt;&gt; a\r\n    [1, 2, 3]\r\n\r\n^ This is the long form for doing such assignments.\r\n\r\nBelow is the ternary form. But this isn&#39;t most succinct way - see last example.\r\n\r\n    &gt;&gt;&gt; a = li1 if li1 else li2\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a\r\n    [1, 2, 3]\r\n    &gt;&gt;&gt; \r\n\r\nWith Python, you can simply use `or` for alternative assignments.\r\n\r\n    &gt;&gt;&gt; a = li1 or li2\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a\r\n    [1, 2, 3]\r\n    &gt;&gt;&gt; \r\n\r\nThe above works since `li1` is `None` and the interp treats that as False in logic expressions. The interp then moves on and evaluates the second expression, which is not `None` and it&#39;s not an empty list - so it gets assigned to a.\r\n\r\nThis also works with empty lists. For instance, if you want to assign `a` whichever list has items.\r\n\r\n    &gt;&gt;&gt; li1 = []\r\n    &gt;&gt;&gt; li2 = [1, 2, 3]\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a = li1 or li2\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a\r\n    [1, 2, 3]\r\n    &gt;&gt;&gt; \r\n\r\nKnowing this, you can simply such assignments whenever you encounter them. This also works with strings and other iterables. You could assign `a` whichever string isn&#39;t empty.\r\n\r\n    &gt;&gt;&gt; s1 = &#39;&#39;\r\n    &gt;&gt;&gt; s2 = &#39;hello world&#39;\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a = s1 or s2\r\n    &gt;&gt;&gt; \r\n    &gt;&gt;&gt; a\r\n    &#39;hello world&#39;\r\n    &gt;&gt;&gt; \r\n\r\nI always liked the C ternary syntax, but Python takes it a step further!\r\n\r\nI understand that some may say this isn&#39;t a good stylistic choice because it relies on mechanics that aren&#39;t immediately apparent to all developers. I personally disagree with that viewpoint. Python is a syntax rich language with lots of idiomatic tricks that aren&#39;t immediately apparent to the dabler. But the more you learn and understand the mechanics of the underlying system, the more you appreciate it.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 414,
                "display_name" "eatsfood"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1588093079,
              "post_id" 60876846,
              "comment_id" 108765520,
              "body_markdown" "This seems to be twice the amount of work, more RAM usage and more obfuscated than the simpler `val = a if cond else b` statement.",
            }
          ],
          "owner" {
            "reputation" 102,
            "display_name" "Baruc Almaguer"
          },
          "is_accepted" false,
          "score" -1,
          "last_activity_date" 1585259983,
          "answer_id" 60876846,
          "body_markdown" "I find cumbersome the default python syntax `val = a if cond else b`, so sometimes I do this:\r\n```\r\niif = lambda (cond, a, b): a if cond else b\r\n# so I can then use it like:\r\nval = iif(cond, a, b)\r\n```\r\nOf course, it has the downside of always evaluating both sides (a and b), but the syntax it&#39;s way clearer to me",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 138,
                "display_name" "ruancomelli"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1603207300,
              "post_id" 61896436,
              "comment_id" 113960535,
              "body_markdown" "A valuable complement, but I disagree: `option_value or 10` is *not* better than `option_value if option_value is not None else 10`. It is shorter, indeed, but looks weird to me and may lead to bugs. What happens if `option_value = 0`, for instance? The first snippet will run `run_algorithm(0)` because `option_value` is not `None`. The second and third snippets, however, will run `run_algorithm(10)` because `0` is a falsy. The two snippets are not equivalent, and hence one is not better than the other. And explicit is better than implicit.",
            },
            {
              "owner" {
                "reputation" 2299,
                "display_name" "user118967"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1603375483,
              "post_id" 61896436,
              "comment_id" 114022487,
              "body_markdown" "@ruancomelli: Good point. I&#39;ve modified the answer to reflect that correction.",
            },
            {
              "owner" {
                "reputation" 2299,
                "display_name" "user118967"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1603375583,
              "post_id" 61896436,
              "comment_id" 114022542,
              "body_markdown" "As for it looking weird, I wonder if it looked weird to you because you noticed the imprecision (that it was not really equivalent). To me it sounds natural because it reminds me saying in English: &quot;Use this or that (if the first option is unavailable)&quot;. But of course that is subjective. It is useful to know it does not look natural to everybody.",
            },
            {
              "owner" {
                "reputation" 138,
                "display_name" "ruancomelli"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1603914260,
              "post_id" 61896436,
              "comment_id" 114190254,
              "body_markdown" "Much better! And thanks for the explanation regarding the &quot;or&quot;-idiom. It looks weird to me because I tend to think of `or` as a function mapping two arguments to a boolean, so I expect it to return either `True` or `False` (this happens in many other programming languages). But &quot;use this or that&quot; is a nice mnemonic and will definitely help me (and hopefully others) to remember this pattern.",
            }
          ],
          "owner" {
            "reputation" 2299,
            "display_name" "user118967"
          },
          "is_accepted" false,
          "score" 3,
          "last_activity_date" 1603375452,
          "answer_id" 61896436,
          "body_markdown" "Other answers correctly talk about the Python ternary operator. I would like to complement by mentioning a scenario for which the ternary operator is often used but for which there is a better idiom. This is the scenario of using a default value.\r\n\r\nSuppose we want to use `option_value` with a default value if it is not set:\r\n\r\n    run_algorithm(option_value if option_value is not None else 10)\r\n\r\nor, if `option_value` is never set to a falsy value (`0`, `&quot;&quot;`, etc), simply\r\n\r\n    run_algorithm(option_value if option_value else 10)\r\n\r\n \r\nHowever, in this case an ever better solution is simply to write\r\n\r\n    run_algorithm(option_value or 10)\r\n",
        },
        {
          "owner" {
            "reputation" 157,
            "display_name" "Samanthika Rajapaksa"
          },
          "is_accepted" false,
          "score" 1,
          "last_activity_date" 1607527061,
          "answer_id" 65219579,
          "body_markdown" "The ternary operator is a way of writing conditional statements in Python. As the name ternary suggests, this Python operator consists of three operands.\r\n\r\nSyntax:\r\nThe three operands in a ternary operator include:\r\n\r\ncondition: A boolean expression that evaluates to either true or false.\r\n\r\ntrue_val: A value to be assigned if the expression is evaluated to true.\r\n\r\nfalse_val: A value to be assigned if the expression is evaluated to false.\r\n\r\n\r\n        var = true_val if condition else false_val\r\n\r\nThe variable var on the left-hand side of the = (assignment) operator will be assigned:\r\n\r\nvalue1 if the booleanExpression evaluates to true.\r\n\r\nvalue2 if the booleanExpression evaluates to false.\r\n\r\nExample:\r\n\r\n        # USING TERNARY OPERATOR\r\n        to_check = 6\r\n        msg = &quot;Even&quot; if to_check%2 == 0 else &quot;Odd&quot;\r\n        print(msg) \r\n\r\n        # USING USUAL IF-ELSE\r\n        msg = &quot;&quot;\r\n        if(to_check%2 == 0):\r\n        msg = &quot;Even&quot;\r\n        else:\r\n        msg = &quot;Odd&quot;\r\n        print(msg)\r\n",
        },
        {
          "owner" {
            "reputation" 37,
            "display_name" "David Chung"
          },
          "is_accepted" false,
          "score" 0,
          "last_activity_date" 1608717719,
          "answer_id" 65422380,
          "body_markdown" "Vinko Vrsalovic&#39;s answer is good enough. There is only one more thing:\r\n&gt; Note that conditionals are an *expression*, not a *statement*. This means you can&#39;t use assignment statements or ```pass``` or other **statements** within a conditional **expression**\r\n\r\n# Walrus operator in Python 3.8 #\r\nAfter that walrus operator was introduced in Python 3.8, there is something changed.\r\n```\r\n(a := 3) if True else (b := 5)\r\n```\r\ngives ```a = 3``` and ```b is not defined```,\r\n```\r\n(a := 3) if False else (b := 5)\r\n```\r\ngives ```a is not defined``` and ```b = 5```, and\r\n```\r\nc = (a := 3) if False else (b := 5)\r\n```\r\ngives ```c = 5```, ```a is not defined``` and ```b = 5```.\r\n\r\nEven if this may be ugly, **assignments** can be done **inside** conditional expressions after Python 3.8. Anyway, it is still better to use normal ```if``` **statement** instead in this case.",
        }
      ],
      "owner" {
        "reputation" 86963,
        "display_name" "Devoted"
      },
      "view_count" 2041369,
      "score" 6404,
      "last_activity_date" 1608717719,
      "question_id" 394809,
      "body_markdown" "If Python does not have a ternary conditional operator, is it possible to simulate one using other language constructs?",
      "title" "Does Python have a ternary conditional operator?"
    },
    {
      "tags" [
        "python",
        "oop",
        "metaclass",
        "python-datamodel"
      ],
      "answers" [
        {
          "owner" {
            "reputation" 37760,
            "display_name" "Jerub"
          },
          "is_accepted" false,
          "score" 425,
          "last_activity_date" 1573027043,
          "answer_id" 100037,
          "body_markdown" "*Note, this answer is for Python 2.x as it was written in 2008, metaclasses are slightly different in 3.x.*\r\n\r\nMetaclasses are the secret sauce that make &#39;class&#39; work. The default metaclass for a new style object is called &#39;type&#39;.\r\n\r\n&lt;!-- language: lang-none --&gt;\r\n\r\n    class type(object)\r\n      |  type(object) -&gt; the object&#39;s type\r\n      |  type(name, bases, dict) -&gt; a new type\r\n\r\nMetaclasses take 3 args. &#39;**name**&#39;, &#39;**bases**&#39; and &#39;**dict**&#39;\r\n\r\nHere is where the secret starts. Look for where name, bases and the dict come from in this example class definition.\r\n\r\n    class ThisIsTheName(Bases, Are, Here):\r\n        All_the_code_here\r\n        def doesIs(create, a):\r\n            dict\r\n\r\nLets define a metaclass that will demonstrate how &#39;**class:**&#39; calls it.\r\n\r\n    def test_metaclass(name, bases, dict):\r\n        print &#39;The Class Name is&#39;, name\r\n        print &#39;The Class Bases are&#39;, bases\r\n        print &#39;The dict has&#39;, len(dict), &#39;elems, the keys are&#39;, dict.keys()\r\n    \r\n        return &quot;yellow&quot;\r\n    \r\n    class TestName(object, None, int, 1):\r\n        __metaclass__ = test_metaclass\r\n        foo = 1\r\n        def baz(self, arr):\r\n            pass\r\n    \r\n    print &#39;TestName = &#39;, repr(TestName)\r\n\r\n    # output =&gt; \r\n    The Class Name is TestName\r\n    The Class Bases are (&lt;type &#39;object&#39;&gt;, None, &lt;type &#39;int&#39;&gt;, 1)\r\n    The dict has 4 elems, the keys are [&#39;baz&#39;, &#39;__module__&#39;, &#39;foo&#39;, &#39;__metaclass__&#39;]\r\n    TestName =  &#39;yellow&#39;\r\n\r\n\r\nAnd now, an example that actually means something, this will automatically make the variables in the list &quot;attributes&quot; set on the class, and set to None.\r\n\r\n    def init_attributes(name, bases, dict):\r\n        if &#39;attributes&#39; in dict:\r\n            for attr in dict[&#39;attributes&#39;]:\r\n                dict[attr] = None\r\n    \r\n        return type(name, bases, dict)\r\n    \r\n    class Initialised(object):\r\n        __metaclass__ = init_attributes\r\n        attributes = [&#39;foo&#39;, &#39;bar&#39;, &#39;baz&#39;]\r\n    \r\n    print &#39;foo =&gt;&#39;, Initialised.foo\r\n    # output=&gt;\r\n    foo =&gt; None\r\n\r\n**Note that the magic behaviour that `Initialised` gains by having the metaclass `init_attributes` is not passed onto a subclass of `Initialised`.**\r\n\r\nHere is an even more concrete example, showing how you can subclass &#39;type&#39; to make a metaclass that performs an action when the class is created. This is quite tricky:\r\n\r\n\tclass MetaSingleton(type):\r\n\t    instance = None\r\n\t    def __call__(cls, *args, **kw):\r\n\t        if cls.instance is None:\r\n\t            cls.instance = super(MetaSingleton, cls).__call__(*args, **kw)\r\n\t        return cls.instance\r\n\r\n\tclass Foo(object):\r\n\t    __metaclass__ = MetaSingleton\r\n\r\n\ta = Foo()\r\n\tb = Foo()\r\n\tassert a is b\r\n",
        },
        {
          "owner" {
            "reputation" 2850,
            "display_name" "Matthias Kestenholz"
          },
          "is_accepted" false,
          "score" 128,
          "last_activity_date" 1534135995,
          "answer_id" 100059,
          "body_markdown" "I think the ONLamp introduction to metaclass programming is well written and gives a really good introduction to the topic despite being several years old already.\r\n\r\n[http://www.onlamp.com/pub/a/python/2003/04/17/metaclasses.html][1] (archived at https://web.archive.org/web/20080206005253/http://www.onlamp.com/pub/a/python/2003/04/17/metaclasses.html)\r\n\r\nIn short: A class is a blueprint for the creation of an instance, a metaclass is a blueprint for the creation of a class. It can be easily seen that in Python classes need to be first-class objects too to enable this behavior.\r\n\r\nI&#39;ve never written one myself, but I think one of the nicest uses of metaclasses can be seen in the [Django framework][2]. The model classes use a metaclass approach to enable a declarative style of writing new models or form classes. While the metaclass is creating the class, all members get the possibility to customize the class itself.\r\n\r\n  - [Creating a new model][3]\r\n  - [The metaclass enabling this][4]\r\n\r\nThe thing that&#39;s left to say is: If you don&#39;t know what metaclasses are, the probability that you **will not need them** is 99%.\r\n\r\n\r\n  [1]: http://www.onlamp.com/pub/a/python/2003/04/17/metaclasses.html\r\n  [2]: http://www.djangoproject.com/\r\n  [3]: http://docs.djangoproject.com/en/dev/intro/tutorial01/#id3\r\n  [4]: http://code.djangoproject.com/browser/django/trunk/django/db/models/base.py#L25",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 10957,
                "display_name" "trixn"
              },
              "score" 7,
              "post_type" "answer",
              "creation_date" 1485559482,
              "post_id" 100091,
              "comment_id" 70991701,
              "body_markdown" "Isn&#39;t the use of meta classes adding new properties and methods to a **class** and not an instance? As far as i understood it the meta class alters the class itself and as a result the instances can be constructed differently by the altered class. Could be a bit misleading to people who try to get the nature of a meta class. Having useful methods on instances can be achieved by normal inherence. The reference to Django code as an example is good, though.",
            }
          ],
          "owner" {
            "reputation" 8718,
            "display_name" "Antti Rasinen"
          },
          "is_accepted" false,
          "score" 167,
          "last_activity_date" 1221806740,
          "answer_id" 100091,
          "body_markdown" "One use for metaclasses is adding new properties and methods to an instance automatically.\r\n\r\nFor example, if you look at [Django models][1], their definition looks a bit confusing. It looks as if you are only defining class properties:\r\n\r\n    class Person(models.Model):\r\n        first_name = models.CharField(max_length=30)\r\n        last_name = models.CharField(max_length=30)\r\n\r\nHowever, at runtime the Person objects are filled with all sorts of useful methods. See the [source][2] for some amazing metaclassery.\r\n\r\n\r\n  [1]: http://docs.djangoproject.com/en/dev/topics/db/models/\r\n  [2]: http://code.djangoproject.com/browser/django/trunk/django/db/models/base.py",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 3356,
                "display_name" "pppery"
              },
              "score" 14,
              "post_type" "answer",
              "creation_date" 1501770869,
              "post_id" 100146,
              "comment_id" 77935564,
              "body_markdown" "`class A(type):pass&lt;NEWLINE&gt;class B(type,metaclass=A):pass&lt;NEWLINE&gt;b.__class__ = b`",
            },
            {
              "owner" {
                "reputation" 131,
                "display_name" "Holle van"
              },
              "score" 25,
              "post_type" "answer",
              "creation_date" 1537313050,
              "post_id" 100146,
              "comment_id" 91736655,
              "body_markdown" "ppperry he obviously meant you can&#39;t recreate type without using type itself as a metaclass. Which is fair enough to say.",
            },
            {
              "owner" {
                "reputation" 5884,
                "display_name" "Ciasto piekarz"
              },
              "score" 4,
              "post_type" "answer",
              "creation_date" 1543453155,
              "post_id" 100146,
              "comment_id" 93929134,
              "body_markdown" "Shouldn&#39;t unregister()  be called by instance of Example class ?",
            },
            {
              "owner" {
                "reputation" 1894,
                "display_name" "BlackShift"
              },
              "score" 8,
              "post_type" "answer",
              "creation_date" 1556699760,
              "post_id" 100146,
              "comment_id" 98519860,
              "body_markdown" "Note that `__metaclass__` is not supported in Python 3. In Python 3 use `class MyObject(metaclass=MyType)`, see https://www.python.org/dev/peps/pep-3115/ and the answer below.",
            },
            {
              "owner" {
                "reputation" 544,
                "display_name" "kapad"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1567061789,
              "post_id" 100146,
              "comment_id" 101851909,
              "body_markdown" "`The metaclass is determined by looking at the baseclasses of the class-to-be (metaclasses are inherited), at the __metaclass__ attribute of the class-to-be (if any) or the __metaclass__ global variable.`; Is this ordering correct? Or will python first look at `__metaclass__` in the class-to-be and then it&#39;s baseclasses and then the global `__metaclass__`?",
            },
            {
              "owner" {
                "reputation" 100325,
                "display_name" "George Mauer"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1568902933,
              "post_id" 100146,
              "comment_id" 102430482,
              "body_markdown" "Which version of python does this code example assume and does it matter?",
            },
            {
              "owner" {
                "reputation" 544,
                "display_name" "kapad"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1573023886,
              "post_id" 100146,
              "comment_id" 103741093,
              "body_markdown" "@thomas-wouters Is this line `metaclasses are inherited` correct? [Jerub&#39;s](https://stackoverflow.com/questions/100003/what-is-a-metaclass-in-python/100037#100037) answer says that metaclasses *are not* inherited. Also, what happens if the class subclasses multiple classes, and they each have their own `__metaclass__` (or two or more parent classes define a `__metaclass__`).",
            },
            {
              "owner" {
                "reputation" 369837,
                "display_name" "chepner"
              },
              "score" 3,
              "post_type" "answer",
              "creation_date" 1578604462,
              "post_id" 100146,
              "comment_id" 105503046,
              "body_markdown" "The documentation describes [how the metaclass is chosen](https://docs.python.org/3/reference/datamodel.html#determining-the-appropriate-metaclass). The metaclass isn&#39;t inherited so much as it is derived. If you specify a metaclass, it has to be a subtype of each base class metaclass; otherwise, you&#39;ll use the a base class metaclass that is a subtype of each other base class metaclass. Note that it is possible that *no* valid metaclass can be found, and the definition will fail.",
            },
            {
              "owner" {
                "reputation" 486,
                "display_name" "Vishesh Mangla"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1594138793,
              "post_id" 100146,
              "comment_id" 111019130,
              "body_markdown" "Well can someone also explain what does register tag do? I &#39;m using class X(ABC): from lot of time to create interfaces but what exactly register will enable me to do? The docs literally says one line about register.",
            },
            {
              "owner" {
                "reputation" 7616,
                "display_name" "Reinderien"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1608230181,
              "post_id" 100146,
              "comment_id" 115526811,
              "body_markdown" "What @BlackShift says. This could use a big, bold warning at the top that the entire answer is deprecated, as it relies on Python 2 which is itself deprecated.",
            }
          ],
          "owner" {
            "reputation" 113757,
            "display_name" "Thomas Wouters"
          },
          "is_accepted" true,
          "score" 3075,
          "last_activity_date" 1551735259,
          "answer_id" 100146,
          "body_markdown" "A metaclass is the class of a class. A class defines how an instance of the class (i.e. an object) behaves while a metaclass defines how a class behaves. A class is an instance of a metaclass.\r\n\r\nWhile in Python you can use arbitrary callables for metaclasses (like [Jerub][1] shows), the better approach is to make it an actual class itself. `type` is the usual metaclass in Python. `type` is itself a class, and it is its own type. You won&#39;t be able to recreate something like `type` purely in Python, but Python cheats a little. To create your own metaclass in Python you really just want to subclass `type`.\r\n\r\nA metaclass is most commonly used as a class-factory. When you create an object by calling the class, Python creates a new class (when it executes the &#39;class&#39; statement) by calling the metaclass. Combined with the normal `__init__` and `__new__` methods, metaclasses therefore allow you to do &#39;extra things&#39; when creating a class, like registering the new class with some registry or replace the class with something else entirely.\r\n\r\nWhen the `class` statement is executed, Python first executes the body of the `class` statement as a normal block of code. The resulting namespace (a dict) holds the attributes of the class-to-be. The metaclass is determined by looking at the baseclasses of the class-to-be (metaclasses are inherited), at the `__metaclass__` attribute of the class-to-be (if any) or the `__metaclass__` global variable. The metaclass is then called with the name, bases and attributes of the class to instantiate it.\r\n\r\nHowever, metaclasses actually define the *type* of a class, not just a factory for it, so you can do much more with them. You can, for instance, define normal methods on the metaclass. These metaclass-methods are like classmethods in that they can be called on the class without an instance, but they are also not like classmethods in that they cannot be called on an instance of the class. `type.__subclasses__()` is an example of a method on the `type` metaclass. You can also define the normal &#39;magic&#39; methods, like `__add__`, `__iter__` and `__getattr__`, to implement or change how the class behaves.\r\n\r\nHere&#39;s an aggregated example of the bits and pieces:\r\n\r\n    def make_hook(f):\r\n        &quot;&quot;&quot;Decorator to turn &#39;foo&#39; method into &#39;__foo__&#39;&quot;&quot;&quot;\r\n        f.is_hook = 1\r\n        return f\r\n    \r\n    class MyType(type):\r\n        def __new__(mcls, name, bases, attrs):\r\n    \r\n            if name.startswith(&#39;None&#39;):\r\n                return None\r\n    \r\n            # Go over attributes and see if they should be renamed.\r\n            newattrs = {}\r\n            for attrname, attrvalue in attrs.iteritems():\r\n                if getattr(attrvalue, &#39;is_hook&#39;, 0):\r\n                    newattrs[&#39;__%s__&#39; % attrname] = attrvalue\r\n                else:\r\n                    newattrs[attrname] = attrvalue\r\n    \r\n            return super(MyType, mcls).__new__(mcls, name, bases, newattrs)\r\n    \r\n        def __init__(self, name, bases, attrs):\r\n            super(MyType, self).__init__(name, bases, attrs)\r\n    \r\n            # classregistry.register(self, self.interfaces)\r\n            print &quot;Would register class %s now.&quot; % self\r\n    \r\n        def __add__(self, other):\r\n            class AutoClass(self, other):\r\n                pass\r\n            return AutoClass\r\n            # Alternatively, to autogenerate the classname as well as the class:\r\n            # return type(self.__name__ + other.__name__, (self, other), {})\r\n    \r\n        def unregister(self):\r\n            # classregistry.unregister(self)\r\n            print &quot;Would unregister class %s now.&quot; % self\r\n    \r\n    class MyObject:\r\n        __metaclass__ = MyType\r\n    \r\n    \r\n    class NoneSample(MyObject):\r\n        pass\r\n    \r\n    # Will print &quot;NoneType None&quot;\r\n    print type(NoneSample), repr(NoneSample)\r\n    \r\n    class Example(MyObject):\r\n        def __init__(self, value):\r\n            self.value = value\r\n        @make_hook\r\n        def add(self, other):\r\n            return self.__class__(self.value + other.value)\r\n    \r\n    # Will unregister the class\r\n    Example.unregister()\r\n    \r\n    inst = Example(10)\r\n    # Will fail with an AttributeError\r\n    #inst.unregister()\r\n    \r\n    print inst + inst\r\n    class Sibling(MyObject):\r\n        pass\r\n    \r\n    ExampleSibling = Example + Sibling\r\n    # ExampleSibling is now a subclass of both Example and Sibling (with no\r\n    # content of its own) although it will believe it&#39;s called &#39;AutoClass&#39;\r\n    print ExampleSibling\r\n    print ExampleSibling.__mro__\r\n\r\n  [1]: https://stackoverflow.com/questions/100003/what-is-a-metaclass-in-python/100037#100037",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 91312,
                "display_name" "Michael Gundlach"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1555437574,
              "post_id" 6428779,
              "comment_id" 98108709,
              "body_markdown" "Thanks for the example.  Why did you find this easier than inheriting from MyBase, whose `__init__(self)` says `type(self)._order = MyBase.counter; MyBase.counter += 1` ?",
            },
            {
              "owner" {
                "reputation" 153234,
                "display_name" "kindall"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1555438179,
              "post_id" 6428779,
              "comment_id" 98108978,
              "body_markdown" "I wanted the classes themselves, not their instances, to be numbered.",
            },
            {
              "owner" {
                "reputation" 91312,
                "display_name" "Michael Gundlach"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1555538280,
              "post_id" 6428779,
              "comment_id" 98151100,
              "body_markdown" "Right, duh. Thanks. My code would reset MyType&#39;s attribute on every instantiation, and would never set the attribute if an instance of MyType was never created.  Oops.  (And a class property could also work, but unlike the metaclass it offers no obvious place to store the counter.)",
            },
            {
              "owner" {
                "reputation" 9227,
                "display_name" "mike rodent"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1571702983,
              "post_id" 6428779,
              "comment_id" 103320847,
              "body_markdown" "This is a jolly interesting example, not least because one can genuinely see why a metaclass could be neeeded with this, to supply a solution to a specific difficulty. OTOH I struggle to be convinced that anyone would really need to instantiate objects in the order in which their classes were defined: I guess we just have to take your word for that :).",
            }
          ],
          "owner" {
            "reputation" 153234,
            "display_name" "kindall"
          },
          "is_accepted" false,
          "score" 179,
          "last_activity_date" 1480356281,
          "answer_id" 6428779,
          "body_markdown" "Others have explained how metaclasses work and how they fit into the Python type system. Here&#39;s an example of what they can be used for. In a testing framework I wrote, I wanted to keep track of the order in which classes were defined, so that I could later instantiate them in this order. I found it easiest to do this using a metaclass.\r\n\r\n    class MyMeta(type):\r\n    \r\n        counter = 0\r\n    \r\n        def __init__(cls, name, bases, dic):\r\n            type.__init__(cls, name, bases, dic)\r\n            cls._order = MyMeta.counter\r\n            MyMeta.counter += 1\r\n    \r\n    class MyType(object):              # Python 2\r\n        __metaclass__ = MyMeta\r\n\r\n    class MyType(metaclass=MyMeta):    # Python 3\r\n        pass\r\n\r\nAnything that&#39;s a subclass of `MyType` then gets a class attribute `_order` that records the order in which the classes were defined.",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 353,
                "display_name" "Max Goodridge"
              },
              "score" 35,
              "post_type" "answer",
              "creation_date" 1492003139,
              "post_id" 6581949,
              "comment_id" 73803183,
              "body_markdown" "It appears that in Django `models.Model` it does not use `__metaclass__` but rather `class Model(metaclass=ModelBase):` to reference a `ModelBase` class which then does the aforementioned metaclass magic. Great post! Here&#39;s the Django source: https://github.com/django/django/blob/master/django/db/models/base.py#L382",
            },
            {
              "owner" {
                "reputation" 149,
                "display_name" "Spybdai"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1492658648,
              "post_id" 6581949,
              "comment_id" 74073568,
              "body_markdown" "what if indicate different __metaclass__ in both base and derived class?",
            },
            {
              "owner" {
                "reputation" 1383,
                "display_name" "petrux"
              },
              "score" 18,
              "post_type" "answer",
              "creation_date" 1493155964,
              "post_id" 6581949,
              "comment_id" 74291770,
              "body_markdown" "&lt;&lt;Be careful here that the `__metaclass__` attribute will not be inherited, the metaclass of the parent (`Bar.__class__`) will be. If `Bar` used a `__metaclass__` attribute that created `Bar` with `type()` (and not `type.__new__()`), the subclasses will not inherit that behavior.&gt;&gt; -- Could you/someone please explain a bit deeper this passage?",
            },
            {
              "owner" {
                "reputation" 1237,
                "display_name" "TBBle"
              },
              "score" 19,
              "post_type" "answer",
              "creation_date" 1497360147,
              "post_id" 6581949,
              "comment_id" 76037462,
              "body_markdown" "@MaxGoodridge That&#39;s the Python 3 syntax for metaclasses. See [Python 3.6 Data model](https://docs.python.org/3.6/reference/datamodel.html#metaclasses) VS [Python 2.7 Data model](https://docs.python.org/2.7/reference/datamodel.html?#customizing-class-creation)",
            },
            {
              "owner" {
                "reputation" 721,
                "display_name" "Deep"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1498401834,
              "post_id" 6581949,
              "comment_id" 76475706,
              "body_markdown" "echoing @petrux question. I was lost at the statement:\n\n\n    `Be careful here that the __metaclass__ attribute will not be inherited, the metaclass of the parent (Bar.__class__) will be. If Bar used a __metaclass__ attribute that created Bar with type() (and not type.__new__()), the subclasses will not inherit that behavior.`\n\n\n\nCan someone please explain this a little bit deeper? Would really appreciate some help here.",
            },
            {
              "owner" {
                "reputation" 3356,
                "display_name" "pppery"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1501771357,
              "post_id" 6581949,
              "comment_id" 77936026,
              "body_markdown" "&quot;type is actually its own metaclass. This is not something you could reproduce in pure Python, and is done by cheating a little bit at the implementation level.&quot; is not true. `__class__` is writable in Python, so one can create a subclass of type with a custom metaclass, and then set its class to itself.",
            },
            {
              "owner" {
                "reputation" 468,
                "display_name" "Philip Stark"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1502875059,
              "post_id" 6581949,
              "comment_id" 78377186,
              "body_markdown" "@Deep: The _actual_ metaclass of a class is specified in `.__class__`, whereas `.__metaclass__` specifies which callable should be used to alter the class during creation. If for example `.__metaclass__` contains a function `foo_bar()` that uses `type(x,y,z)` to alter the class, then you will have .`__metaclass__ = foo_bar` which will not be inherited but `.__class__`  will be `type`, because that&#39;s what was used to create the new altered class. \nRead this a few times. I am 99% sure I haven&#39;t made a mistake ;)",
            },
            {
              "owner" {
                "reputation" 26015,
                "display_name" "Mr_and_Mrs_D"
              },
              "score" 5,
              "post_type" "answer",
              "creation_date" 1506682047,
              "post_id" 6581949,
              "comment_id" 79928729,
              "body_markdown" "`Now you wonder why the heck is it written in lowercase, and not Type?` - well because it&#39;s implemented in C - it&#39;s the same reason defaultdict is lowercase while OrderedDict (in python 2) is normal CamelCase",
            },
            {
              "owner" {
                "reputation" 3149,
                "display_name" "Brōtsyorfuzthrāx"
              },
              "score" 19,
              "post_type" "answer",
              "creation_date" 1510131571,
              "post_id" 6581949,
              "comment_id" 81299800,
              "body_markdown" "It&#39;s a community wiki answer (so, those who commented with corrections/improvements might consider editing their comments into the answer, if they&#39;re sure they are correct).",
            },
            {
              "owner" {
                "reputation" 34736,
                "display_name" "Ren&#233; Nyffenegger"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1522759238,
              "post_id" 6581949,
              "comment_id" 86267443,
              "body_markdown" "If a metaclass is an object, does that object not also have a metaclass?",
            },
            {
              "owner" {
                "reputation" 2334,
                "display_name" "Nearoo"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1536070208,
              "post_id" 6581949,
              "comment_id" 91286491,
              "body_markdown" "@Ren&#233;Nyffenegger Yes, you can have a metaclass of a metaclass of a metaclass",
            },
            {
              "owner" {
                "reputation" 1925,
                "display_name" "polarise"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1555597926,
              "post_id" 6581949,
              "comment_id" 98173392,
              "body_markdown" "Dynamically creating methods is a bit more tricky than the text illustrates. While it works here it fails to account for cases where we refer to the `self` object. For example the method `def foo(self, *args, **kwargs): print(self, args, kwargs)` will do different things if created dynamically or as part of the class definition. The right way to create it dynamically is to execute the body in a namespace then attach it (see *Python Essential Reference 4th Edition, David Beazly p.138*).",
            },
            {
              "owner" {
                "reputation" 778,
                "display_name" "skytree"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1559129518,
              "post_id" 6581949,
              "comment_id" 99322592,
              "body_markdown" "Why `type(...)` is not really OOP compared with `type.__new__(...)` ?",
            },
            {
              "owner" {
                "reputation" 1492,
                "display_name" "Karuhanga"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1562746448,
              "post_id" 6581949,
              "comment_id" 100469454,
              "body_markdown" "`Subclasses of a class will be instances of its metaclass if you specified a metaclass-class, but not with a metaclass-function.`  Why is this the case?",
            },
            {
              "owner" {
                "reputation" 354,
                "display_name" "artu-hnrq"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1588235948,
              "post_id" 6581949,
              "comment_id" 108823005,
              "body_markdown" "Karuhanga, `metaclass-function` was referring any method that manipulates a class declaration, calling `type.__new__(x, y, z)` directly in way that no specific `__metaclass__` is set to the just created class (i.e. its metaclass will be `type`). Thus this class&#39; subclasses will also have by default its same mataclass (that is, `type`)",
            },
            {
              "owner" {
                "reputation" 377,
                "display_name" "3rdi"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1590052496,
              "post_id" 6581949,
              "comment_id" 109536175,
              "body_markdown" "May i know, if everything is an object in Python... type is also an object. So type is also created using some parent class. If we try to understand this where exactly this parent tree ends? What is its root? and how is this all mapped. In short i am asking for how to build something like python from scratch probably.",
            },
            {
              "owner" {
                "reputation" 369837,
                "display_name" "chepner"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1591897971,
              "post_id" 6581949,
              "comment_id" 110237486,
              "body_markdown" "@3rdi It&#39;s not really a proper tree, as there is no true root. There is no type that has no metaclass; the metaclass of `type` is `type`. It&#39;s this loop in the graph that prevents it from being a tree, and the prevents `type` from being defined in Python itself. It has to be provided by the implementation.",
            },
            {
              "owner" {
                "reputation" 143,
                "display_name" "Miszo97"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1593169163,
              "post_id" 6581949,
              "comment_id" 110692105,
              "body_markdown" "Such a great response. Superbly and clearly told.",
            }
          ],
          "owner" {
            "reputation" 500965,
            "display_name" "e-satis"
          },
          "is_accepted" false,
          "score" 7094,
          "last_activity_date" 1600437653,
          "answer_id" 6581949,
          "body_markdown" "Classes as objects\r\n==================\r\n\r\nBefore understanding metaclasses, you need to master classes in Python. And Python has a very peculiar idea of what classes are, borrowed from the Smalltalk language.\r\n\r\nIn most languages, classes are just pieces of code that describe how to produce an object. That&#39;s kinda true in Python too:\r\n\r\n    &gt;&gt;&gt; class ObjectCreator(object):\r\n    ...       pass\r\n    ...\r\n\r\n    &gt;&gt;&gt; my_object = ObjectCreator()\r\n    &gt;&gt;&gt; print(my_object)\r\n    &lt;__main__.ObjectCreator object at 0x8974f2c&gt;\r\n\r\nBut classes are more than that in Python. Classes are objects too.\r\n\r\nYes, objects.\r\n\r\nAs soon as you use the keyword `class`, Python executes it and creates\r\nan OBJECT. The instruction\r\n\r\n    &gt;&gt;&gt; class ObjectCreator(object):\r\n    ...       pass\r\n    ...\r\n\r\ncreates in memory an object with the name &quot;ObjectCreator&quot;.\r\n\r\n**This object (the class) is itself capable of creating objects (the instances),\r\nand this is why it&#39;s a class**.\r\n\r\nBut still, it&#39;s an object, and therefore:\r\n\r\n- you can assign it to a variable\r\n- you can copy it\r\n- you can add attributes to it\r\n- you can pass it as a function parameter\r\n\r\ne.g.:\r\n\r\n    &gt;&gt;&gt; print(ObjectCreator) # you can print a class because it&#39;s an object\r\n    &lt;class &#39;__main__.ObjectCreator&#39;&gt;\r\n    &gt;&gt;&gt; def echo(o):\r\n    ...       print(o)\r\n    ...\r\n    &gt;&gt;&gt; echo(ObjectCreator) # you can pass a class as a parameter\r\n    &lt;class &#39;__main__.ObjectCreator&#39;&gt;\r\n    &gt;&gt;&gt; print(hasattr(ObjectCreator, &#39;new_attribute&#39;))\r\n    False\r\n    &gt;&gt;&gt; ObjectCreator.new_attribute = &#39;foo&#39; # you can add attributes to a class\r\n    &gt;&gt;&gt; print(hasattr(ObjectCreator, &#39;new_attribute&#39;))\r\n    True\r\n    &gt;&gt;&gt; print(ObjectCreator.new_attribute)\r\n    foo\r\n    &gt;&gt;&gt; ObjectCreatorMirror = ObjectCreator # you can assign a class to a variable\r\n    &gt;&gt;&gt; print(ObjectCreatorMirror.new_attribute)\r\n    foo\r\n    &gt;&gt;&gt; print(ObjectCreatorMirror())\r\n    &lt;__main__.ObjectCreator object at 0x8997b4c&gt;\r\n\r\n\r\nCreating classes dynamically\r\n============================\r\n\r\nSince classes are objects, you can create them on the fly, like any object.\r\n\r\nFirst, you can create a class in a function using `class`:\r\n\r\n    &gt;&gt;&gt; def choose_class(name):\r\n    ...     if name == &#39;foo&#39;:\r\n    ...         class Foo(object):\r\n    ...             pass\r\n    ...         return Foo # return the class, not an instance\r\n    ...     else:\r\n    ...         class Bar(object):\r\n    ...             pass\r\n    ...         return Bar\r\n    ...\r\n    &gt;&gt;&gt; MyClass = choose_class(&#39;foo&#39;)\r\n    &gt;&gt;&gt; print(MyClass) # the function returns a class, not an instance\r\n    &lt;class &#39;__main__.Foo&#39;&gt;\r\n    &gt;&gt;&gt; print(MyClass()) # you can create an object from this class\r\n    &lt;__main__.Foo object at 0x89c6d4c&gt;\r\n\r\n\r\nBut it&#39;s not so dynamic, since you still have to write the whole class yourself.\r\n\r\nSince classes are objects, they must be generated by something.\r\n\r\nWhen you use the `class` keyword, Python creates this object automatically. But as\r\nwith most things in Python, it gives you a way to do it manually.\r\n\r\nRemember the function `type`? The good old function that lets you know what\r\ntype an object is:\r\n\r\n    &gt;&gt;&gt; print(type(1))\r\n    &lt;type &#39;int&#39;&gt;\r\n    &gt;&gt;&gt; print(type(&quot;1&quot;))\r\n    &lt;type &#39;str&#39;&gt;\r\n    &gt;&gt;&gt; print(type(ObjectCreator))\r\n    &lt;type &#39;type&#39;&gt;\r\n    &gt;&gt;&gt; print(type(ObjectCreator()))\r\n    &lt;class &#39;__main__.ObjectCreator&#39;&gt;\r\n\r\nWell, [`type`][1] has a completely different ability, it can also create classes on the fly. `type` can take the description of a class as parameters,\r\nand return a class.\r\n\r\n(I  know, it&#39;s silly that the same function can have two completely different uses according to the parameters you pass to it. It&#39;s an issue due to backward\r\ncompatibility in Python)\r\n\r\n`type` works this way:\r\n\r\n    type(name, bases, attrs)\r\n\r\nWhere:\r\n\r\n- **`name`**: name of the class\r\n- **`bases`**: tuple of the parent class (for inheritance, can be empty)\r\n- **`attrs`**: dictionary containing attributes names and values\r\n\r\n\r\ne.g.:\r\n\r\n    &gt;&gt;&gt; class MyShinyClass(object):\r\n    ...       pass\r\n\r\ncan be created manually this way:\r\n\r\n    &gt;&gt;&gt; MyShinyClass = type(&#39;MyShinyClass&#39;, (), {}) # returns a class object\r\n    &gt;&gt;&gt; print(MyShinyClass)\r\n    &lt;class &#39;__main__.MyShinyClass&#39;&gt;\r\n    &gt;&gt;&gt; print(MyShinyClass()) # create an instance with the class\r\n    &lt;__main__.MyShinyClass object at 0x8997cec&gt;\r\n\r\nYou&#39;ll notice that we use &quot;MyShinyClass&quot; as the name of the class\r\nand as the variable to hold the class reference. They can be different,\r\nbut there is no reason to complicate things.\r\n\r\n`type` accepts a dictionary to define the attributes of the class. So:\r\n\r\n    &gt;&gt;&gt; class Foo(object):\r\n    ...       bar = True\r\n\r\nCan be translated to:\r\n\r\n    &gt;&gt;&gt; Foo = type(&#39;Foo&#39;, (), {&#39;bar&#39;:True})\r\n\r\nAnd used as a normal class:\r\n\r\n    &gt;&gt;&gt; print(Foo)\r\n    &lt;class &#39;__main__.Foo&#39;&gt;\r\n    &gt;&gt;&gt; print(Foo.bar)\r\n    True\r\n    &gt;&gt;&gt; f = Foo()\r\n    &gt;&gt;&gt; print(f)\r\n    &lt;__main__.Foo object at 0x8a9b84c&gt;\r\n    &gt;&gt;&gt; print(f.bar)\r\n    True\r\n\r\nAnd of course, you can inherit from it, so:\r\n\r\n    &gt;&gt;&gt;   class FooChild(Foo):\r\n    ...         pass\r\n\r\nwould be:\r\n\r\n    &gt;&gt;&gt; FooChild = type(&#39;FooChild&#39;, (Foo,), {})\r\n    &gt;&gt;&gt; print(FooChild)\r\n    &lt;class &#39;__main__.FooChild&#39;&gt;\r\n    &gt;&gt;&gt; print(FooChild.bar) # bar is inherited from Foo\r\n    True\r\n\r\nEventually, you&#39;ll want to add methods to your class. Just define a function\r\nwith the proper signature and assign it as an attribute.\r\n\r\n    &gt;&gt;&gt; def echo_bar(self):\r\n    ...       print(self.bar)\r\n    ...\r\n    &gt;&gt;&gt; FooChild = type(&#39;FooChild&#39;, (Foo,), {&#39;echo_bar&#39;: echo_bar})\r\n    &gt;&gt;&gt; hasattr(Foo, &#39;echo_bar&#39;)\r\n    False\r\n    &gt;&gt;&gt; hasattr(FooChild, &#39;echo_bar&#39;)\r\n    True\r\n    &gt;&gt;&gt; my_foo = FooChild()\r\n    &gt;&gt;&gt; my_foo.echo_bar()\r\n    True\r\n\r\nAnd you can add even more methods after you dynamically create the class, just like adding methods to a normally created class object.\r\n\r\n    &gt;&gt;&gt; def echo_bar_more(self):\r\n    ...       print(&#39;yet another method&#39;)\r\n    ...\r\n    &gt;&gt;&gt; FooChild.echo_bar_more = echo_bar_more\r\n    &gt;&gt;&gt; hasattr(FooChild, &#39;echo_bar_more&#39;)\r\n    True\r\n\r\nYou see where we are going: in Python, classes are objects, and you can create a class on the fly, dynamically.\r\n\r\nThis is what Python does when you use the keyword `class`, and it does so by using a metaclass.\r\n\r\nWhat are metaclasses (finally)\r\n==============================\r\n\r\nMetaclasses are the &#39;stuff&#39; that creates classes.\r\n\r\nYou define classes in order to create objects, right?\r\n\r\nBut we learned that Python classes are objects.\r\n\r\nWell, metaclasses are what create these objects. They are the classes&#39; classes,\r\nyou can picture them this way:\r\n\r\n    MyClass = MetaClass()\r\n    my_object = MyClass()\r\n\r\nYou&#39;ve seen that `type` lets you do something like this:\r\n\r\n    MyClass = type(&#39;MyClass&#39;, (), {})\r\n\r\nIt&#39;s because the function `type` is in fact a metaclass. `type` is the\r\nmetaclass Python uses to create all classes behind the scenes.\r\n\r\nNow you wonder why the heck is it written in lowercase, and not `Type`?\r\n\r\nWell, I guess it&#39;s a matter of consistency with `str`, the class that creates\r\nstrings objects, and `int` the class that creates integer objects. `type` is\r\njust the class that creates class objects.\r\n\r\nYou see that by checking the `__class__` attribute.\r\n\r\nEverything, and I mean everything, is an object in Python. That includes ints,\r\nstrings, functions and classes. All of them are objects. And all of them have\r\nbeen created from a class:\r\n\r\n    &gt;&gt;&gt; age = 35\r\n    &gt;&gt;&gt; age.__class__\r\n    &lt;type &#39;int&#39;&gt;\r\n    &gt;&gt;&gt; name = &#39;bob&#39;\r\n    &gt;&gt;&gt; name.__class__\r\n    &lt;type &#39;str&#39;&gt;\r\n    &gt;&gt;&gt; def foo(): pass\r\n    &gt;&gt;&gt; foo.__class__\r\n    &lt;type &#39;function&#39;&gt;\r\n    &gt;&gt;&gt; class Bar(object): pass\r\n    &gt;&gt;&gt; b = Bar()\r\n    &gt;&gt;&gt; b.__class__\r\n    &lt;class &#39;__main__.Bar&#39;&gt;\r\n\r\nNow, what is the `__class__` of any `__class__` ?\r\n\r\n    &gt;&gt;&gt; age.__class__.__class__\r\n    &lt;type &#39;type&#39;&gt;\r\n    &gt;&gt;&gt; name.__class__.__class__\r\n    &lt;type &#39;type&#39;&gt;\r\n    &gt;&gt;&gt; foo.__class__.__class__\r\n    &lt;type &#39;type&#39;&gt;\r\n    &gt;&gt;&gt; b.__class__.__class__\r\n    &lt;type &#39;type&#39;&gt;\r\n\r\nSo, a metaclass is just the stuff that creates class objects.\r\n\r\nYou can call it a &#39;class factory&#39; if you wish.\r\n\r\n`type` is the built-in metaclass Python uses, but of course, you can create your\r\nown metaclass.\r\n\r\nThe [`__metaclass__`][2] attribute\r\n==================================\r\n\r\nIn Python 2, you can add a `__metaclass__` attribute when you write a class (see next section for the Python 3 syntax):\r\n\r\n    class Foo(object):\r\n        __metaclass__ = something...\r\n        [...]\r\n\r\nIf you do so, Python will use the metaclass to create the class `Foo`.\r\n\r\nCareful, it&#39;s tricky.\r\n\r\nYou write `class Foo(object)` first, but the class object `Foo` is not created\r\nin memory yet.\r\n\r\nPython will look for `__metaclass__` in the class definition. If it finds it,\r\nit will use it to create the object class `Foo`. If it doesn&#39;t, it will use\r\n`type` to create the class.\r\n\r\nRead that several times.\r\n\r\nWhen you do:\r\n\r\n    class Foo(Bar):\r\n        pass\r\n\r\nPython does the following:\r\n\r\nIs there a `__metaclass__` attribute in `Foo`?\r\n\r\nIf yes, create in-memory a class object (I said a class object, stay with me here), with the name `Foo` by using what is in `__metaclass__`.\r\n\r\nIf Python can&#39;t find `__metaclass__`, it will look for a `__metaclass__` at the MODULE level, and try to do the same (but only for classes that don&#39;t inherit anything, basically old-style classes).\r\n\r\nThen if it can&#39;t find any `__metaclass__` at all, it will use the `Bar`&#39;s (the first parent) own metaclass (which might be the default `type`) to create the class object.\r\n\r\nBe careful here that the `__metaclass__` attribute will not be inherited, the metaclass of the parent (`Bar.__class__`) will be. If `Bar` used a `__metaclass__` attribute that created `Bar` with `type()` (and not `type.__new__()`), the subclasses will not inherit that behavior.\r\n\r\nNow the big question is, what can you put in `__metaclass__`?\r\n\r\nThe answer is something that can create a class.\r\n\r\nAnd what can create a class? `type`, or anything that subclasses or uses it.\r\n\r\nMetaclasses in Python 3\r\n=======================\r\n\r\nThe syntax to set the metaclass has been changed in Python 3:\r\n\r\n    class Foo(object, metaclass=something):\r\n        ...\r\n\r\ni.e. the `__metaclass__` attribute is no longer used, in favor of a keyword argument in the list of base classes.\r\n\r\nThe behavior of metaclasses however stays [largely the same](https://www.python.org/dev/peps/pep-3115/).\r\n\r\nOne thing added to metaclasses in Python 3 is that you can also pass attributes as keyword-arguments into a metaclass, like so:\r\n\r\n    class Foo(object, metaclass=something, kwarg1=value1, kwarg2=value2):\r\n        ...\r\n\r\nRead the section below for how python handles this.\r\n\r\nCustom metaclasses\r\n==================\r\n\r\nThe main purpose of a metaclass is to change the class automatically,\r\nwhen it&#39;s created.\r\n\r\nYou usually do this for APIs, where you want to create classes matching the\r\ncurrent context.\r\n\r\nImagine a stupid example, where you decide that all classes in your module\r\nshould have their attributes written in uppercase. There are several ways to\r\ndo this, but one way is to set `__metaclass__` at the module level.\r\n\r\nThis way, all classes of this module will be created using this metaclass,\r\nand we just have to tell the metaclass to turn all attributes to uppercase.\r\n\r\nLuckily, `__metaclass__` can actually be any callable, it doesn&#39;t need to be a\r\nformal class (I know, something with &#39;class&#39; in its name doesn&#39;t need to be\r\na class, go figure... but it&#39;s helpful).\r\n\r\nSo we will start with a simple example, by using a function.\r\n\r\n    # the metaclass will automatically get passed the same argument\r\n    # that you usually pass to `type`\r\n    def upper_attr(future_class_name, future_class_parents, future_class_attrs):\r\n        &quot;&quot;&quot;\r\n          Return a class object, with the list of its attribute turned\r\n          into uppercase.\r\n        &quot;&quot;&quot;\r\n        # pick up any attribute that doesn&#39;t start with &#39;__&#39; and uppercase it\r\n        uppercase_attrs = {\r\n            attr if attr.startswith(&quot;__&quot;) else attr.upper(): v\r\n            for attr, v in future_class_attrs.items()\r\n        }\r\n\r\n        # let `type` do the class creation\r\n        return type(future_class_name, future_class_parents, uppercase_attrs)\r\n\r\n    __metaclass__ = upper_attr # this will affect all classes in the module\r\n\r\n    class Foo(): # global __metaclass__ won&#39;t work with &quot;object&quot; though\r\n        # but we can define __metaclass__ here instead to affect only this class\r\n        # and this will work with &quot;object&quot; children\r\n        bar = &#39;bip&#39;\r\n\r\nLet&#39;s check:\r\n\r\n    &gt;&gt;&gt; hasattr(Foo, &#39;bar&#39;)\r\n    False\r\n    &gt;&gt;&gt; hasattr(Foo, &#39;BAR&#39;)\r\n    True\r\n    &gt;&gt;&gt; Foo.BAR\r\n    &#39;bip&#39;\r\n\r\nNow, let&#39;s do exactly the same, but using a real class for a metaclass:\r\n\r\n    # remember that `type` is actually a class like `str` and `int`\r\n    # so you can inherit from it\r\n    class UpperAttrMetaclass(type):\r\n        # __new__ is the method called before __init__\r\n        # it&#39;s the method that creates the object and returns it\r\n        # while __init__ just initializes the object passed as parameter\r\n        # you rarely use __new__, except when you want to control how the object\r\n        # is created.\r\n        # here the created object is the class, and we want to customize it\r\n        # so we override __new__\r\n        # you can do some stuff in __init__ too if you wish\r\n        # some advanced use involves overriding __call__ as well, but we won&#39;t\r\n        # see this\r\n        def __new__(upperattr_metaclass, future_class_name,\r\n                    future_class_parents, future_class_attrs):\r\n            uppercase_attrs = {\r\n                attr if attr.startswith(&quot;__&quot;) else attr.upper(): v\r\n                for attr, v in future_class_attrs.items()\r\n            }\r\n            return type(future_class_name, future_class_parents, uppercase_attrs)\r\n\r\nLet&#39;s rewrite the above, but with shorter and more realistic variable names now that we know what they mean:\r\n\r\n    class UpperAttrMetaclass(type):\r\n        def __new__(cls, clsname, bases, attrs):\r\n            uppercase_attrs = {\r\n                attr if attr.startswith(&quot;__&quot;) else attr.upper(): v\r\n                for attr, v in attrs.items()\r\n            }\r\n            return type(clsname, bases, uppercase_attrs)\r\n\r\nYou may have noticed the extra argument `cls`. There is\r\nnothing special about it: `__new__` always receives the class it&#39;s defined in, as the first parameter. Just like you have `self` for ordinary methods which receive the instance as the first parameter, or the defining class for class methods.\r\n\r\nBut this is not proper OOP. We are calling `type` directly and we aren&#39;t overriding or calling the parent&#39;s `__new__`. Let&#39;s do that instead:\r\n\r\n    class UpperAttrMetaclass(type):\r\n        def __new__(cls, clsname, bases, attrs):\r\n            uppercase_attrs = {\r\n                attr if attr.startswith(&quot;__&quot;) else attr.upper(): v\r\n                for attr, v in attrs.items()\r\n            }\r\n            return type.__new__(cls, clsname, bases, uppercase_attrs)\r\n\r\nWe can make it even cleaner by using `super`, which will ease inheritance (because yes, you can have metaclasses, inheriting from metaclasses, inheriting from type):\r\n\r\n    class UpperAttrMetaclass(type):\r\n        def __new__(cls, clsname, bases, attrs):\r\n            uppercase_attrs = {\r\n                attr if attr.startswith(&quot;__&quot;) else attr.upper(): v\r\n                for attr, v in attrs.items()\r\n            }\r\n            return super(UpperAttrMetaclass, cls).__new__(\r\n                cls, clsname, bases, uppercase_attrs)\r\n\r\nOh, and in python 3 if you do this call with keyword arguments, like this:\r\n\r\n    class Foo(object, metaclass=MyMetaclass, kwarg1=value1):\r\n        ...\r\n\r\nIt translates to this in the metaclass to use it:\r\n\r\n    class MyMetaclass(type):\r\n        def __new__(cls, clsname, bases, dct, kwargs1=default):\r\n            ...\r\n\r\nThat&#39;s it. There is really nothing more about metaclasses.\r\n\r\nThe reason behind the complexity of the code using metaclasses is not because\r\nof metaclasses, it&#39;s because you usually use metaclasses to do twisted stuff\r\nrelying on introspection, manipulating inheritance, vars such as `__dict__`, etc.\r\n\r\nIndeed, metaclasses are especially useful to do black magic, and therefore\r\ncomplicated stuff. But by themselves, they are simple:\r\n\r\n- intercept a class creation\r\n- modify the class\r\n- return the modified class\r\n\r\n\r\nWhy would you use metaclasses classes instead of functions?\r\n=============================================================\r\n\r\nSince `__metaclass__` can accept any callable, why would you use a class\r\nsince it&#39;s obviously more complicated?\r\n\r\nThere are several reasons to do so:\r\n\r\n- The intention is clear. When you read `UpperAttrMetaclass(type)`, you know\r\n  what&#39;s going to follow\r\n- You can use OOP. Metaclass can inherit from metaclass, override parent methods. Metaclasses can even use metaclasses.\r\n- Subclasses of a class will be instances of its metaclass if you specified a metaclass-class, but not with a metaclass-function.\r\n- You can structure your code better. You never use metaclasses for something as trivial as the above example. It&#39;s usually for something complicated. Having the ability to make several methods and group them in one class is very useful to make the code easier to read.\r\n- You can hook on `__new__`, `__init__` and `__call__`. Which will allow you to do different stuff, Even if usually you can do it all in `__new__`,\r\n  some people are just more comfortable using `__init__`.\r\n- These are called metaclasses, damn it! It must mean something!\r\n\r\n\r\nWhy would you use metaclasses?\r\n========================================\r\n\r\nNow the big question. Why would you use some obscure error-prone feature?\r\n\r\nWell, usually you don&#39;t:\r\n\r\n&gt; Metaclasses are deeper magic that\r\n&gt; 99% of users should never worry about it.\r\n&gt; If you wonder whether you need them,\r\n&gt; you don&#39;t (the people who actually\r\n&gt; need them to know with certainty that\r\n&gt; they need them and don&#39;t need an\r\n&gt; explanation about why).\r\n\r\n  *Python Guru Tim Peters*\r\n\r\nThe main use case for a metaclass is creating an API. A typical example of this is the Django ORM. It allows you to define something like this:\r\n\r\n    class Person(models.Model):\r\n        name = models.CharField(max_length=30)\r\n        age = models.IntegerField()\r\n\r\nBut if you do this:\r\n\r\n    person = Person(name=&#39;bob&#39;, age=&#39;35&#39;)\r\n    print(person.age)\r\n\r\nIt won&#39;t return an `IntegerField` object. It will return an `int`, and can even take it directly from the database.\r\n\r\nThis is possible because `models.Model` defines `__metaclass__` and\r\nit uses some magic that will turn the `Person` you just defined with simple statements\r\ninto a complex hook to a database field.\r\n\r\nDjango makes something complex look simple by exposing a simple API\r\nand using metaclasses, recreating code from this API to do the real job\r\nbehind the scenes.\r\n\r\nThe last word\r\n====================\r\n\r\nFirst, you know that classes are objects that can create instances.\r\n\r\nWell, in fact, classes are themselves instances. Of metaclasses.\r\n\r\n    &gt;&gt;&gt; class Foo(object): pass\r\n    &gt;&gt;&gt; id(Foo)\r\n    142630324\r\n\r\nEverything is an object in Python, and they are all either instance of classes\r\nor instances of metaclasses.\r\n\r\nExcept for `type`.\r\n\r\n`type` is actually its own metaclass. This is not something you could\r\nreproduce in pure Python, and is done by cheating a little bit at the implementation\r\nlevel.\r\n\r\nSecondly, metaclasses are complicated. You may not want to use them for\r\nvery simple class alterations. You can change classes by using two different techniques:\r\n\r\n- [monkey patching](http://en.wikipedia.org/wiki/Monkey_patch)\r\n- class decorators\r\n\r\n99% of the time you need class alteration, you are better off using these.\r\n\r\nBut 98% of the time, you don&#39;t need class alteration at all.\r\n\r\n\r\n  [1]: http://docs.python.org/2/library/functions.html#type\r\n  [2]: http://docs.python.org/2/reference/datamodel.html?highlight=__metaclass__#__metaclass__",
        },
        {
          "owner" {
            "reputation" 3640,
            "display_name" "Craig"
          },
          "is_accepted" false,
          "score" 60,
          "last_activity_date" 1453752517,
          "answer_id" 21999253,
          "body_markdown" "A metaclass is a class that tells how (some) other class should be created.\r\n\r\nThis is a case where I saw metaclass as a solution to my problem:\r\nI had a really complicated problem, that probably could have been solved differently, but I chose to solve it using a metaclass.  Because of the complexity, it is one of the few modules I have written where the comments in the module surpass the amount of code that has been written.  Here it is...\r\n\r\n    #!/usr/bin/env python\r\n\r\n    # Copyright (C) 2013-2014 Craig Phillips.  All rights reserved.\r\n\r\n    # This requires some explaining.  The point of this metaclass excercise is to\r\n    # create a static abstract class that is in one way or another, dormant until\r\n    # queried.  I experimented with creating a singlton on import, but that did\r\n    # not quite behave how I wanted it to.  See now here, we are creating a class\r\n    # called GsyncOptions, that on import, will do nothing except state that its\r\n    # class creator is GsyncOptionsType.  This means, docopt doesn&#39;t parse any\r\n    # of the help document, nor does it start processing command line options.\r\n    # So importing this module becomes really efficient.  The complicated bit\r\n    # comes from requiring the GsyncOptions class to be static.  By that, I mean\r\n    # any property on it, may or may not exist, since they are not statically\r\n    # defined; so I can&#39;t simply just define the class with a whole bunch of\r\n    # properties that are @property @staticmethods.\r\n    #\r\n    # So here&#39;s how it works:\r\n    #\r\n    # Executing &#39;from libgsync.options import GsyncOptions&#39; does nothing more\r\n    # than load up this module, define the Type and the Class and import them\r\n    # into the callers namespace.  Simple.\r\n    #\r\n    # Invoking &#39;GsyncOptions.debug&#39; for the first time, or any other property\r\n    # causes the __metaclass__ __getattr__ method to be called, since the class\r\n    # is not instantiated as a class instance yet.  The __getattr__ method on\r\n    # the type then initialises the class (GsyncOptions) via the __initialiseClass\r\n    # method.  This is the first and only time the class will actually have its\r\n    # dictionary statically populated.  The docopt module is invoked to parse the\r\n    # usage document and generate command line options from it.  These are then\r\n    # paired with their defaults and what&#39;s in sys.argv.  After all that, we\r\n    # setup some dynamic properties that could not be defined by their name in\r\n    # the usage, before everything is then transplanted onto the actual class\r\n    # object (or static class GsyncOptions).\r\n    #\r\n    # Another piece of magic, is to allow command line options to be set in\r\n    # in their native form and be translated into argparse style properties.\r\n    #\r\n    # Finally, the GsyncListOptions class is actually where the options are\r\n    # stored.  This only acts as a mechanism for storing options as lists, to\r\n    # allow aggregation of duplicate options or options that can be specified\r\n    # multiple times.  The __getattr__ call hides this by default, returning the\r\n    # last item in a property&#39;s list.  However, if the entire list is required,\r\n    # calling the &#39;list()&#39; method on the GsyncOptions class, returns a reference\r\n    # to the GsyncListOptions class, which contains all of the same properties\r\n    # but as lists and without the duplication of having them as both lists and\r\n    # static singlton values.\r\n    #\r\n    # So this actually means that GsyncOptions is actually a static proxy class...\r\n    #\r\n    # ...And all this is neatly hidden within a closure for safe keeping.\r\n    def GetGsyncOptionsType():\r\n        class GsyncListOptions(object):\r\n            __initialised = False\r\n\r\n        class GsyncOptionsType(type):\r\n            def __initialiseClass(cls):\r\n                if GsyncListOptions._GsyncListOptions__initialised: return\r\n\r\n                from docopt import docopt\r\n                from libgsync.options import doc\r\n                from libgsync import __version__\r\n\r\n                options = docopt(\r\n                    doc.__doc__ % __version__,\r\n                    version = __version__,\r\n                    options_first = True\r\n                )\r\n\r\n                paths = options.pop(&#39;&lt;path&gt;&#39;, None)\r\n                setattr(cls, &quot;destination_path&quot;, paths.pop() if paths else None)\r\n                setattr(cls, &quot;source_paths&quot;, paths)\r\n                setattr(cls, &quot;options&quot;, options)\r\n\r\n                for k, v in options.iteritems():\r\n                    setattr(cls, k, v)\r\n\r\n                GsyncListOptions._GsyncListOptions__initialised = True\r\n\r\n            def list(cls):\r\n                return GsyncListOptions\r\n\r\n            def __getattr__(cls, name):\r\n                cls.__initialiseClass()\r\n                return getattr(GsyncListOptions, name)[-1]\r\n\r\n            def __setattr__(cls, name, value):\r\n                # Substitut option names: --an-option-name for an_option_name\r\n                import re\r\n                name = re.sub(r&#39;^__&#39;, &quot;&quot;, re.sub(r&#39;-&#39;, &quot;_&quot;, name))\r\n                listvalue = []\r\n\r\n                # Ensure value is converted to a list type for GsyncListOptions\r\n                if isinstance(value, list):\r\n                    if value:\r\n                        listvalue = [] + value\r\n                    else:\r\n                        listvalue = [ None ]\r\n                else:\r\n                    listvalue = [ value ]\r\n\r\n                type.__setattr__(GsyncListOptions, name, listvalue)\r\n\r\n        # Cleanup this module to prevent tinkering.\r\n        import sys\r\n        module = sys.modules[__name__]\r\n        del module.__dict__[&#39;GetGsyncOptionsType&#39;]\r\n\r\n        return GsyncOptionsType\r\n\r\n    # Our singlton abstract proxy class.\r\n    class GsyncOptions(object):\r\n        __metaclass__ = GetGsyncOptionsType()",
        },
        {
          "owner" {
            "reputation" 270870,
            "display_name" "Aaron Hall"
          },
          "is_accepted" false,
          "score" 119,
          "last_activity_date" 1504063154,
          "answer_id" 31930795,
          "body_markdown" "&gt; ## What are metaclasses? What do you use them for?\r\n\r\nTLDR: A metaclass instantiates and defines behavior for a class just like a class instantiates and defines behavior for an instance. \r\n\r\nPseudocode:\r\n\r\n    &gt;&gt;&gt; Class(...)\r\n    instance\r\n\r\nThe above should look familiar. Well, where does `Class` come from? It&#39;s an instance of a metaclass (also pseudocode):\r\n\r\n    &gt;&gt;&gt; Metaclass(...)\r\n    Class\r\n    \r\nIn real code, we can pass the default metaclass, `type`, everything we need to instantiate a class and we get a class:\r\n\r\n    &gt;&gt;&gt; type(&#39;Foo&#39;, (object,), {}) # requires a name, bases, and a namespace\r\n    &lt;class &#39;__main__.Foo&#39;&gt;\r\n\r\n## Putting it differently\r\n\r\n- A class is to an instance as a metaclass is to a class. \r\n\r\n  When we instantiate an object, we get an instance:\r\n\r\n        &gt;&gt;&gt; object()                          # instantiation of class\r\n        &lt;object object at 0x7f9069b4e0b0&gt;     # instance\r\n\r\n  Likewise, when we define a class explicitly with the default metaclass, `type`, we instantiate it:\r\n\r\n        &gt;&gt;&gt; type(&#39;Object&#39;, (object,), {})     # instantiation of metaclass\r\n        &lt;class &#39;__main__.Object&#39;&gt;             # instance\r\n\r\n\r\n- Put another way, a class is an instance of a metaclass:\r\n\r\n        &gt;&gt;&gt; isinstance(object, type)\r\n        True\r\n\r\n- Put a third way, a metaclass is a class&#39;s class.\r\n\r\n        &gt;&gt;&gt; type(object) == type\r\n        True\r\n        &gt;&gt;&gt; object.__class__\r\n        &lt;class &#39;type&#39;&gt;\r\n\r\n\r\nWhen you write a class definition and Python executes it, it uses a metaclass to instantiate the class object (which will, in turn, be used to instantiate instances of that class).\r\n\r\nJust as we can use class definitions to change how custom object instances behave, we can use a metaclass class definition to change the way a class object behaves.\r\n\r\nWhat can they be used for? From the [docs][1]:\r\n\r\n&gt; The potential uses for metaclasses are boundless. Some ideas that have been explored include logging, interface checking, automatic delegation, automatic property creation, proxies, frameworks, and automatic resource locking/synchronization.\r\n\r\nNevertheless, it is usually encouraged for users to avoid using metaclasses unless absolutely necessary.\r\n\r\n# You use a metaclass every time you create a class:\r\n\r\nWhen you write a class definition, for example, like this,\r\n\r\n    class Foo(object): \r\n        &#39;demo&#39;\r\n\r\nYou instantiate a class object.\r\n\r\n    &gt;&gt;&gt; Foo\r\n    &lt;class &#39;__main__.Foo&#39;&gt;\r\n    &gt;&gt;&gt; isinstance(Foo, type), isinstance(Foo, object)\r\n    (True, True)\r\n\r\nIt is the same as functionally calling `type` with the appropriate arguments and assigning the result to a variable of that name:\r\n\r\n    name = &#39;Foo&#39;\r\n    bases = (object,)\r\n    namespace = {&#39;__doc__&#39;: &#39;demo&#39;}\r\n    Foo = type(name, bases, namespace)\r\n\r\nNote, some things automatically get added to the `__dict__`, i.e., the namespace:\r\n\r\n    &gt;&gt;&gt; Foo.__dict__\r\n    dict_proxy({&#39;__dict__&#39;: &lt;attribute &#39;__dict__&#39; of &#39;Foo&#39; objects&gt;, \r\n    &#39;__module__&#39;: &#39;__main__&#39;, &#39;__weakref__&#39;: &lt;attribute &#39;__weakref__&#39; \r\n    of &#39;Foo&#39; objects&gt;, &#39;__doc__&#39;: &#39;demo&#39;})\r\n\r\nThe *metaclass* of the object we created, in both cases, is `type`. \r\n\r\n(A side-note on the contents of the class `__dict__`: `__module__` is there because classes must know where they are defined, and  `__dict__` and `__weakref__` are there because we don&#39;t define `__slots__` - if we [define `__slots__`][2] we&#39;ll save a bit of space in the instances, as we can disallow `__dict__` and `__weakref__` by excluding them. For example:\r\n\r\n    &gt;&gt;&gt; Baz = type(&#39;Bar&#39;, (object,), {&#39;__doc__&#39;: &#39;demo&#39;, &#39;__slots__&#39;: ()})\r\n    &gt;&gt;&gt; Baz.__dict__\r\n    mappingproxy({&#39;__doc__&#39;: &#39;demo&#39;, &#39;__slots__&#39;: (), &#39;__module__&#39;: &#39;__main__&#39;})\r\n\r\n... but I digress.)\r\n\r\n# We can extend `type` just like any other class definition:\r\n\r\nHere&#39;s the default `__repr__` of classes:\r\n\r\n    &gt;&gt;&gt; Foo\r\n    &lt;class &#39;__main__.Foo&#39;&gt;\r\n\r\nOne of the most valuable things we can do by default in writing a Python object is to provide it with a good `__repr__`. When we call `help(repr)` we learn that there&#39;s a good test for a `__repr__` that also requires a test for equality - `obj == eval(repr(obj))`. The following simple implementation of `__repr__` and `__eq__` for class instances of our type class provides us with a demonstration that may improve on the default `__repr__` of classes:\r\n\r\n    class Type(type):\r\n        def __repr__(cls):\r\n            &quot;&quot;&quot;\r\n            &gt;&gt;&gt; Baz\r\n            Type(&#39;Baz&#39;, (Foo, Bar,), {&#39;__module__&#39;: &#39;__main__&#39;, &#39;__doc__&#39;: None})\r\n            &gt;&gt;&gt; eval(repr(Baz))\r\n            Type(&#39;Baz&#39;, (Foo, Bar,), {&#39;__module__&#39;: &#39;__main__&#39;, &#39;__doc__&#39;: None})\r\n            &quot;&quot;&quot;\r\n            metaname = type(cls).__name__\r\n            name = cls.__name__\r\n            parents = &#39;, &#39;.join(b.__name__ for b in cls.__bases__)\r\n            if parents:\r\n                parents += &#39;,&#39;\r\n            namespace = &#39;, &#39;.join(&#39;: &#39;.join(\r\n              (repr(k), repr(v) if not isinstance(v, type) else v.__name__))\r\n                   for k, v in cls.__dict__.items())\r\n            return &#39;{0}(\\&#39;{1}\\&#39;, ({2}), {{{3}}})&#39;.format(metaname, name, parents, namespace)\r\n        def __eq__(cls, other):\r\n            &quot;&quot;&quot;\r\n            &gt;&gt;&gt; Baz == eval(repr(Baz))\r\n            True            \r\n            &quot;&quot;&quot;\r\n            return (cls.__name__, cls.__bases__, cls.__dict__) == (\r\n                    other.__name__, other.__bases__, other.__dict__)\r\n\r\n\r\nSo now when we create an object with this metaclass, the `__repr__` echoed on the command line provides a much less ugly sight than the default:\r\n\r\n    &gt;&gt;&gt; class Bar(object): pass\r\n    &gt;&gt;&gt; Baz = Type(&#39;Baz&#39;, (Foo, Bar,), {&#39;__module__&#39;: &#39;__main__&#39;, &#39;__doc__&#39;: None})\r\n    &gt;&gt;&gt; Baz\r\n    Type(&#39;Baz&#39;, (Foo, Bar,), {&#39;__module__&#39;: &#39;__main__&#39;, &#39;__doc__&#39;: None})\r\n\r\nWith a nice `__repr__` defined for the class instance, we have a stronger ability to debug our code. However, much further checking with `eval(repr(Class))` is unlikely (as functions would be rather impossible to eval from their default `__repr__`&#39;s).\r\n\r\n# An expected usage: `__prepare__` a namespace\r\n\r\nIf, for example, we want to know in what order a class&#39;s methods are created in, we could provide an ordered dict as the namespace of the class. We would do this with `__prepare__` which [returns the namespace dict for the class if it is implemented in Python 3][3]: \r\n\r\n    from collections import OrderedDict\r\n    \r\n    class OrderedType(Type):\r\n        @classmethod\r\n        def __prepare__(metacls, name, bases, **kwargs):\r\n            return OrderedDict()\r\n        def __new__(cls, name, bases, namespace, **kwargs):\r\n            result = Type.__new__(cls, name, bases, dict(namespace))\r\n            result.members = tuple(namespace)\r\n            return result\r\n\r\nAnd usage:\r\n\r\n    class OrderedMethodsObject(object, metaclass=OrderedType):\r\n        def method1(self): pass\r\n        def method2(self): pass\r\n        def method3(self): pass\r\n        def method4(self): pass\r\n\r\nAnd now we have a record of the order in which these methods (and other class attributes) were created:\r\n\r\n    &gt;&gt;&gt; OrderedMethodsObject.members\r\n    (&#39;__module__&#39;, &#39;__qualname__&#39;, &#39;method1&#39;, &#39;method2&#39;, &#39;method3&#39;, &#39;method4&#39;)\r\n\r\nNote, this example was adapted from the [documentation][1] - the new [enum in the standard library][4] does this.\r\n\r\n\r\nSo what we did was instantiate a metaclass by creating a class. We can also treat the metaclass as we would any other class. It has a method resolution order:\r\n\r\n    &gt;&gt;&gt; inspect.getmro(OrderedType)\r\n    (&lt;class &#39;__main__.OrderedType&#39;&gt;, &lt;class &#39;__main__.Type&#39;&gt;, &lt;class &#39;type&#39;&gt;, &lt;class &#39;object&#39;&gt;)\r\n\r\nAnd it has approximately the correct `repr` (which we can no longer eval unless we can find a way to represent our functions.):\r\n\r\n    &gt;&gt;&gt; OrderedMethodsObject\r\n    OrderedType(&#39;OrderedMethodsObject&#39;, (object,), {&#39;method1&#39;: &lt;function OrderedMethodsObject.method1 at 0x0000000002DB01E0&gt;, &#39;members&#39;: (&#39;__module__&#39;, &#39;__qualname__&#39;, &#39;method1&#39;, &#39;method2&#39;, &#39;method3&#39;, &#39;method4&#39;), &#39;method3&#39;: &lt;function OrderedMet\r\n    hodsObject.method3 at 0x0000000002DB02F0&gt;, &#39;method2&#39;: &lt;function OrderedMethodsObject.method2 at 0x0000000002DB0268&gt;, &#39;__module__&#39;: &#39;__main__&#39;, &#39;__weakref__&#39;: &lt;attribute &#39;__weakref__&#39; of &#39;OrderedMethodsObject&#39; objects&gt;, &#39;__doc__&#39;: None, &#39;__d\r\n    ict__&#39;: &lt;attribute &#39;__dict__&#39; of &#39;OrderedMethodsObject&#39; objects&gt;, &#39;method4&#39;: &lt;function OrderedMethodsObject.method4 at 0x0000000002DB0378&gt;})\r\n\r\n\r\n  [1]: https://docs.python.org/3/reference/datamodel.html#metaclass-example\r\n  [2]: https://stackoverflow.com/q/472000/541136\r\n  [3]: https://docs.python.org/3/reference/datamodel.html#preparing-the-class-namespace\r\n  [4]: https://github.com/python/cpython/blob/master/Lib/enum.py\r\n\r\n\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 887,
                "display_name" "Rich Lysakowski PhD"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1574740446,
              "post_id" 35732111,
              "comment_id" 104328890,
              "body_markdown" "Wow, this is an awesome new feature that I didn&#39;t know existed in Python 3.  Thank you for the example!!",
            },
            {
              "owner" {
                "reputation" 1223,
                "display_name" "Lars"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1583229393,
              "post_id" 35732111,
              "comment_id" 107036974,
              "body_markdown" "Note that since python 3.6, you can use `__set_name__(cls, name)` in the descriptor (`ValidateType`) to set the name in the descriptor (`self.name` and in this case also `self.attr`). This was added to not have to dive into metaclasses for this specific common use case (see PEP 487).",
            }
          ],
          "owner" {
            "reputation" 49076,
            "display_name" "Ethan Furman"
          },
          "is_accepted" false,
          "score" 85,
          "last_activity_date" 1456861714,
          "answer_id" 35732111,
          "body_markdown" "**Python 3 update**\r\n\r\nThere are (at this point) two key methods in a metaclass:\r\n\r\n- `__prepare__`, and\r\n- `__new__`\r\n\r\n`__prepare__` lets you supply a custom mapping (such as an `OrderedDict`) to be used as the namespace while the class is being created.  You must return an instance of whatever namespace you choose.  If you don&#39;t implement `__prepare__` a normal `dict` is used.\r\n\r\n`__new__` is responsible for the actual creation/modification of the final class.\r\n\r\nA bare-bones, do-nothing-extra metaclass would like:\r\n\r\n    class Meta(type):\r\n\r\n        def __prepare__(metaclass, cls, bases):\r\n            return dict()\r\n\r\n        def __new__(metacls, cls, bases, clsdict):\r\n            return super().__new__(metacls, cls, bases, clsdict)\r\n\r\nA simple example:\r\n\r\nSay you want some simple validation code to run on your attributes -- like it must always be an `int` or a `str`.  Without a metaclass, your class would look something like:\r\n\r\n    class Person:\r\n        weight = ValidateType(&#39;weight&#39;, int)\r\n        age = ValidateType(&#39;age&#39;, int)\r\n        name = ValidateType(&#39;name&#39;, str)\r\n\r\nAs you can see, you have to repeat the name of the attribute twice.  This makes typos possible along with irritating bugs.\r\n\r\nA simple metaclass can address that problem:\r\n\r\n    class Person(metaclass=Validator):\r\n        weight = ValidateType(int)\r\n        age = ValidateType(int)\r\n        name = ValidateType(str)\r\n\r\nThis is what the metaclass would look like (not using `__prepare__` since it is not needed):\r\n\r\n    class Validator(type):\r\n        def __new__(metacls, cls, bases, clsdict):\r\n            # search clsdict looking for ValidateType descriptors\r\n            for name, attr in clsdict.items():\r\n                if isinstance(attr, ValidateType):\r\n                    attr.name = name\r\n                    attr.attr = &#39;_&#39; + name\r\n            # create final class and return it\r\n            return super().__new__(metacls, cls, bases, clsdict)\r\n\r\nA sample run of:\r\n\r\n    p = Person()\r\n    p.weight = 9\r\n    print(p.weight)\r\n    p.weight = &#39;9&#39;\r\n\r\nproduces:\r\n\r\n    9\r\n    Traceback (most recent call last):\r\n      File &quot;simple_meta.py&quot;, line 36, in &lt;module&gt;\r\n        p.weight = &#39;9&#39;\r\n      File &quot;simple_meta.py&quot;, line 24, in __set__\r\n        (self.name, self.type, value))\r\n    TypeError: weight must be of type(s) &lt;class &#39;int&#39;&gt; (got &#39;9&#39;)\r\n\r\n---\r\n\r\n**Note**:  This example is simple enough it could have also been accomplished with a class decorator, but presumably an actual metaclass would be doing much more.\r\n\r\nThe &#39;ValidateType&#39; class for reference:\r\n\r\n    class ValidateType:\r\n        def __init__(self, type):\r\n            self.name = None  # will be set by metaclass\r\n            self.attr = None  # will be set by metaclass\r\n            self.type = type\r\n        def __get__(self, inst, cls):\r\n            if inst is None:\r\n                return self\r\n            else:\r\n                return inst.__dict__[self.attr]\r\n        def __set__(self, inst, value):\r\n            if not isinstance(value, self.type):\r\n                raise TypeError(&#39;%s must be of type(s) %s (got %r)&#39; %\r\n                        (self.name, self.type, value))\r\n            else:\r\n                inst.__dict__[self.attr] = value\r\n",
        },
        {
          "owner" {
            "reputation" 2588,
            "display_name" "Mushahid Khan"
          },
          "is_accepted" false,
          "score" 45,
          "last_activity_date" 1503984195,
          "answer_id" 38858285,
          "body_markdown" "`type` is actually a `metaclass` -- a class that creates another classes.\r\nMost `metaclass` are the subclasses of `type`. The `metaclass` receives the `new` class as its first argument and provide access to class object with details as mentioned below:\r\n\r\n    &gt;&gt;&gt; class MetaClass(type):\r\n    ...     def __init__(cls, name, bases, attrs):\r\n    ...         print (&#39;class name: %s&#39; %name )\r\n    ...         print (&#39;Defining class %s&#39; %cls)\r\n    ...         print(&#39;Bases %s: &#39; %bases)\r\n    ...         print(&#39;Attributes&#39;)\r\n    ...         for (name, value) in attrs.items():\r\n    ...             print (&#39;%s :%r&#39; %(name, value))\r\n    ... \r\n\r\n    &gt;&gt;&gt; class NewClass(object, metaclass=MetaClass):\r\n    ...    get_choch=&#39;dairy&#39;\r\n    ... \r\n    class name: NewClass\r\n    Bases &lt;class &#39;object&#39;&gt;: \r\n    Defining class &lt;class &#39;NewClass&#39;&gt;\r\n    get_choch :&#39;dairy&#39;\r\n    __module__ :&#39;builtins&#39;\r\n    __qualname__ :&#39;NewClass&#39;\r\n\r\n`Note: `\r\n\r\nNotice that the class was not instantiated at any time; the simple act of creating the class triggered execution of the `metaclass`.\r\n",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 887,
                "display_name" "Rich Lysakowski PhD"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1574740265,
              "post_id" 40017019,
              "comment_id" 104328842,
              "body_markdown" "This is a good addition to the previously upvoted &quot;accepted answer&quot;.  It provides examples for intermediate coders to chew on.",
            }
          ],
          "owner" {
            "reputation" 15183,
            "display_name" "Michael Ekoka"
          },
          "is_accepted" false,
          "score" 72,
          "last_activity_date" 1535390461,
          "answer_id" 40017019,
          "body_markdown" "# Role of a metaclass&#39; `__call__()` method when creating a class instance\r\n\r\nIf you&#39;ve done Python programming for more than a few months you&#39;ll eventually stumble upon code that looks like this:\r\n\r\n    # define a class\r\n    class SomeClass(object):\r\n        # ...\r\n        # some definition here ...\r\n        # ...\r\n\r\n    # create an instance of it\r\n    instance = SomeClass()\r\n\r\n    # then call the object as if it&#39;s a function\r\n    result = instance(&#39;foo&#39;, &#39;bar&#39;)\r\n\r\nThe latter is possible when you implement the `__call__()` magic method on the class.\r\n\r\n    class SomeClass(object):\r\n        # ...\r\n        # some definition here ...\r\n        # ...\r\n\r\n        def __call__(self, foo, bar):\r\n            return bar + foo\r\n\r\nThe `__call__()` method is invoked when an instance of a class is used as a callable. But as we&#39;ve seen from previous answers a class itself is an instance of a metaclass, so when we use the class as a callable (i.e. when we create an instance of it) we&#39;re actually calling its metaclass&#39; `__call__()` method. At this point most Python programmers are a bit confused because they&#39;ve been told that when creating an instance like this `instance = SomeClass()` you&#39;re calling its `__init__()` method. Some who&#39;ve dug a bit deeper know that before `__init__()` there&#39;s `__new__()`. Well, today another layer of truth is being revealed, before `__new__()` there&#39;s the metaclass&#39; `__call__()`.\r\n\r\n\r\nLet&#39;s study the method call chain from specifically the perspective of creating an instance of a class.\r\n\r\nThis is a metaclass that logs exactly the moment before an instance is created and the moment it&#39;s about to return it.\r\n\r\n    class Meta_1(type):\r\n        def __call__(cls):\r\n            print &quot;Meta_1.__call__() before creating an instance of &quot;, cls\r\n            instance = super(Meta_1, cls).__call__()\r\n            print &quot;Meta_1.__call__() about to return instance.&quot;\r\n            return instance\r\n\r\nThis is a class that uses that metaclass\r\n\r\n    class Class_1(object):\r\n\r\n        __metaclass__ = Meta_1\r\n\r\n        def __new__(cls):\r\n            print &quot;Class_1.__new__() before creating an instance.&quot;\r\n            instance = super(Class_1, cls).__new__(cls)\r\n            print &quot;Class_1.__new__() about to return instance.&quot;\r\n            return instance\r\n\r\n        def __init__(self):\r\n            print &quot;entering Class_1.__init__() for instance initialization.&quot;\r\n            super(Class_1,self).__init__()\r\n            print &quot;exiting Class_1.__init__().&quot;\r\n\r\nAnd now let&#39;s create an instance of `Class_1`\r\n\r\n    instance = Class_1()\r\n    # Meta_1.__call__() before creating an instance of &lt;class &#39;__main__.Class_1&#39;&gt;.\r\n    # Class_1.__new__() before creating an instance.\r\n    # Class_1.__new__() about to return instance.\r\n    # entering Class_1.__init__() for instance initialization.\r\n    # exiting Class_1.__init__().\r\n    # Meta_1.__call__() about to return instance.\r\n\r\nObserve that the code above doesn&#39;t actually do anything more than logging the tasks. Each method delegates the actual work to its parent&#39;s implementation, thus keeping the default behavior. Since `type` is `Meta_1`&#39;s parent class (`type` being the default parent metaclass) and considering the ordering sequence of the output above, we now have a clue as to what would be the pseudo implementation of `type.__call__()`:\r\n\r\n    class type:\r\n        def __call__(cls, *args, **kwarg):\r\n\r\n            # ... maybe a few things done to cls here\r\n\r\n            # then we call __new__() on the class to create an instance\r\n            instance = cls.__new__(cls, *args, **kwargs)\r\n\r\n            # ... maybe a few things done to the instance here\r\n\r\n            # then we initialize the instance with its __init__() method\r\n            instance.__init__(*args, **kwargs)\r\n\r\n            # ... maybe a few more things done to instance here\r\n\r\n            # then we return it\r\n            return instance\r\n\r\nWe can see that the metaclass&#39; `__call__()` method is the one that&#39;s called first. It then delegates creation of the instance to the class&#39;s `__new__()` method and initialization to the instance&#39;s `__init__()`. It&#39;s also the one that ultimately returns the instance.\r\n\r\nFrom the above it stems that the metaclass&#39; `__call__()` is also given the opportunity to decide whether or not a call to `Class_1.__new__()` or `Class_1.__init__()` will eventually be made. Over the course of its execution it could actually return an object that hasn&#39;t been touched by either of these methods. Take for example this approach to the singleton pattern:\r\n\r\n\r\n    class Meta_2(type):\r\n        singletons = {}\r\n\r\n        def __call__(cls, *args, **kwargs):\r\n            if cls in Meta_2.singletons:\r\n                # we return the only instance and skip a call to __new__()\r\n                # and __init__()\r\n                print (&quot;{} singleton returning from Meta_2.__call__(), &quot;\r\n                       &quot;skipping creation of new instance.&quot;.format(cls))\r\n                return Meta_2.singletons[cls]\r\n\r\n            # else if the singleton isn&#39;t present we proceed as usual\r\n            print &quot;Meta_2.__call__() before creating an instance.&quot;\r\n            instance = super(Meta_2, cls).__call__(*args, **kwargs)\r\n            Meta_2.singletons[cls] = instance\r\n            print &quot;Meta_2.__call__() returning new instance.&quot;\r\n            return instance\r\n\r\n    class Class_2(object):\r\n\r\n        __metaclass__ = Meta_2\r\n\r\n        def __new__(cls, *args, **kwargs):\r\n            print &quot;Class_2.__new__() before creating instance.&quot;\r\n            instance = super(Class_2, cls).__new__(cls)\r\n            print &quot;Class_2.__new__() returning instance.&quot;\r\n            return instance\r\n\r\n        def __init__(self, *args, **kwargs):\r\n            print &quot;entering Class_2.__init__() for initialization.&quot;\r\n            super(Class_2, self).__init__()\r\n            print &quot;exiting Class_2.__init__().&quot;\r\n\r\nLet&#39;s observe what happens when repeatedly trying to create an object of type `Class_2`\r\n\r\n    a = Class_2()\r\n    # Meta_2.__call__() before creating an instance.\r\n    # Class_2.__new__() before creating instance.\r\n    # Class_2.__new__() returning instance.\r\n    # entering Class_2.__init__() for initialization.\r\n    # exiting Class_2.__init__().\r\n    # Meta_2.__call__() returning new instance.\r\n\r\n    b = Class_2()\r\n    # &lt;class &#39;__main__.Class_2&#39;&gt; singleton returning from Meta_2.__call__(), skipping creation of new instance.\r\n\r\n    c = Class_2()\r\n    # &lt;class &#39;__main__.Class_2&#39;&gt; singleton returning from Meta_2.__call__(), skipping creation of new instance.\r\n\r\n    a is b is c # True\r\n",
        },
        {
          "owner" {
            "reputation" 5876,
            "display_name" "noɥʇʎԀʎzɐɹƆ"
          },
          "is_accepted" false,
          "score" 46,
          "last_activity_date" 1575563254,
          "answer_id" 41338238,
          "body_markdown" "## The tl;dr version\r\n\r\nThe `type(obj)` function gets you the type of an object. \r\n\r\n**The `type()` of a class is its *metaclass*.**\r\n\r\nTo use a metaclass:\r\n\r\n    class Foo(object):\r\n        __metaclass__ = MyMetaClass\r\n\r\n`type` is its own metaclass. The class of a class is a metaclass-- the body of a class is the arguments passed to the metaclass that is used to construct the class.\r\n\r\n[Here](https://docs.python.org/3/reference/datamodel.html#metaclasses) you can read about how to use metaclasses to customize class construction.",
        },
        {
          "owner" {
            "reputation" 1195,
            "display_name" "Xingzhou Liu"
          },
          "is_accepted" false,
          "score" 29,
          "last_activity_date" 1499933902,
          "answer_id" 45074712,
          "body_markdown" "Python classes are themselves objects - as in instance - of their meta-class. \r\n\r\nThe default metaclass, which is applied when when you determine classes as:\r\n\r\n    class foo:\r\n        ...\r\n\r\nmeta class are used to apply some rule to an entire set of classes. For example, suppose you&#39;re building an ORM to access a database, and you want records from each table to be of a class mapped to that table (based on fields, business rules, etc..,), a possible use of metaclass is for instance, connection pool logic, which is share by all classes of record from all tables. Another use is logic to to support foreign keys, which involves multiple classes of records. \r\n\r\nwhen you define metaclass, you subclass type, and can overrided the following magic methods to insert your logic. \r\n\r\n    class somemeta(type):\r\n        __new__(mcs, name, bases, clsdict):\r\n          &quot;&quot;&quot;\r\n      mcs: is the base metaclass, in this case type.\r\n      name: name of the new class, as provided by the user.\r\n      bases: tuple of base classes \r\n      clsdict: a dictionary containing all methods and attributes defined on class\r\n      \r\n      you must return a class object by invoking the __new__ constructor on the base metaclass. \r\n     ie: \r\n        return type.__call__(mcs, name, bases, clsdict).\r\n\r\n      in the following case:\r\n\r\n      class foo(baseclass):\r\n            __metaclass__ = somemeta\r\n      \r\n      an_attr = 12\r\n      \r\n      def bar(self):\r\n          ...\r\n      \r\n      @classmethod\r\n      def foo(cls):\r\n          ...\r\n\r\n          arguments would be : ( somemeta, &quot;foo&quot;, (baseclass, baseofbase,..., object), {&quot;an_attr&quot;:12, &quot;bar&quot;: &lt;function&gt;, &quot;foo&quot;: &lt;bound class method&gt;}\r\n      \r\n          you can modify any of these values before passing on to type\r\n          &quot;&quot;&quot;\r\n          return type.__call__(mcs, name, bases, clsdict)\r\n\r\n\r\n        def __init__(self, name, bases, clsdict):\r\n          &quot;&quot;&quot; \r\n          called after type has been created. unlike in standard classes, __init__ method cannot modify the instance (cls) - and should be used for class validaton.\r\n          &quot;&quot;&quot;\r\n          pass\r\n          \r\n\r\n        def __prepare__():\r\n            &quot;&quot;&quot;\r\n            returns a dict or something that can be used as a namespace.\r\n            the type will then attach methods and attributes from class definition to it.\r\n\r\n            call order :\r\n\r\n            somemeta.__new__ -&gt;  type.__new__ -&gt; type.__init__ -&gt; somemeta.__init__ \r\n            &quot;&quot;&quot;\r\n            return dict()\r\n\r\n        def mymethod(cls):\r\n            &quot;&quot;&quot; works like a classmethod, but for class objects. Also, my method will not be visible to instances of cls.\r\n            &quot;&quot;&quot;\r\n            pass\r\n\r\nanyhow, those two are the most commonly used hooks. metaclassing is powerful, and above is nowhere near and exhaustive list of uses for metaclassing. \r\n",
        },
        {
          "owner" {
            "reputation" 501,
            "display_name" "binbjz"
          },
          "is_accepted" false,
          "score" 23,
          "last_activity_date" 1515749420,
          "answer_id" 48222963,
          "body_markdown" "The type() function can return the type of an object or create a new type, \r\n\r\nfor example, we can create a Hi class with the type() function and do not  need to use this way with class Hi(object):\r\n\r\n    def func(self, name=&#39;mike&#39;):\r\n        print(&#39;Hi, %s.&#39; % name)\r\n\r\n    Hi = type(&#39;Hi&#39;, (object,), dict(hi=func))\r\n    h = Hi()\r\n    h.hi()\r\n    Hi, mike.\r\n\r\n    type(Hi)\r\n    type\r\n\r\n    type(h)\r\n    __main__.Hi\r\n\r\n\r\nIn addition to using type() to create classes dynamically, you can control creation behavior of class and use metaclass.\r\n\r\nAccording to the Python object model, the class is the object, so the class must be an instance of another certain class.\r\nBy default, a Python class is instance of the type class. That is, type is metaclass of most of the built-in classes and metaclass of user-defined classes.\r\n\r\n    class ListMetaclass(type):\r\n        def __new__(cls, name, bases, attrs):\r\n            attrs[&#39;add&#39;] = lambda self, value: self.append(value)\r\n            return type.__new__(cls, name, bases, attrs)\r\n\r\n    class CustomList(list, metaclass=ListMetaclass):\r\n        pass\r\n\r\n    lst = CustomList()\r\n    lst.add(&#39;custom_list_1&#39;)\r\n    lst.add(&#39;custom_list_2&#39;)\r\n\r\n    lst\r\n    [&#39;custom_list_1&#39;, &#39;custom_list_2&#39;]\r\n\r\nMagic will take effect when we passed keyword arguments in metaclass, it indicates the Python interpreter to create the CustomList through ListMetaclass. __new__ (), at this point, we can modify the class definition, for example, and add a new method and then return the revised definition.",
        },
        {
          "owner" {
            "reputation" 22770,
            "display_name" "Andy Fedoroff"
          },
          "is_accepted" false,
          "score" 13,
          "last_activity_date" 1537017449,
          "answer_id" 52344780,
          "body_markdown" "In addition to the published answers I can say that a `metaclass` defines the behaviour for a class. So, you can explicitly set your metaclass. Whenever Python gets a keyword `class` then it starts searching for the `metaclass`. If it&#39;s not found – the default metaclass type is used to create the class&#39;s object. Using the `__metaclass__` attribute, you can set `metaclass` of your class:\r\n\r\n    class MyClass:\r\n       __metaclass__ = type\r\n       # write here other method\r\n       # write here one more method\r\n\r\n    print(MyClass.__metaclass__)\r\n\r\nIt&#39;ll produce the output like this:\r\n\r\n    class &#39;type&#39;\r\n\r\nAnd, of course, you can create your own `metaclass` to define the behaviour of any class that are created using your class.\r\n\r\nFor doing that, your default `metaclass` type class must be inherited as this is the main `metaclass`:\r\n\r\n    class MyMetaClass(type):\r\n       __metaclass__ = type\r\n       # you can write here any behaviour you want\r\n\r\n    class MyTestClass:\r\n       __metaclass__ = MyMetaClass\r\n\r\n    Obj = MyTestClass()\r\n    print(Obj.__metaclass__)\r\n    print(MyMetaClass.__metaclass__)\r\n\r\nThe output will be:\r\n\r\n    class &#39;__main__.MyMetaClass&#39;\r\n    class &#39;type&#39;",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 4579,
                "display_name" "verisimilitude"
              },
              "score" 2,
              "post_type" "answer",
              "creation_date" 1563039712,
              "post_id" 56945952,
              "comment_id" 100573238,
              "body_markdown" "Rather than giving bookish definitions, would have been better if you had added some examples. The first line of your answer seems to have been copied from the Wikipedia entry of Metaclasses.",
            },
            {
              "owner" {
                "reputation" 4108,
                "display_name" "Venu Gopal Tewari"
              },
              "score" 1,
              "post_type" "answer",
              "creation_date" 1563170533,
              "post_id" 56945952,
              "comment_id" 100596443,
              "body_markdown" "@verisimilitude I am also learning can you help me improving this answer by providing some practical examples from your experience ??",
            },
            {
              "owner" {
                "reputation" 6592,
                "display_name" "JL Peyret"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1607966021,
              "post_id" 56945952,
              "comment_id" 115431752,
              "body_markdown" "&quot;In object-oriented programming, a metaclass is a class whose instances are classes. Just as an ordinary class defines the behavior of certain objects, a metaclass defines the behavior of certain classes and their instances.&quot;  [Metaclass - Wikipedia](https://en.wikipedia.org/wiki/Metaclass).  First 2 sentences, verbatim.  How does this answer benefit anyone besides your rep score?  Your specialty is not Python, metaclasses are some of the most complex and least well understood concepts in Python and there is no lack of excellent answers.  What does a copy-paste bring to the table?",
            }
          ],
          "owner" {
            "reputation" 4108,
            "display_name" "Venu Gopal Tewari"
          },
          "is_accepted" false,
          "score" 5,
          "last_activity_date" 1562651120,
          "answer_id" 56945952,
          "body_markdown" "In object-oriented programming, a metaclass is a class whose instances are classes. Just as an ordinary class defines the behavior of certain objects, a metaclass defines the behavior of certain class and their instances\r\nThe term metaclass simply means something used to create classes. In other words, it is the class of a class. The metaclass is used to create the class so like the object being an instance of a class, a class is an instance of a metaclass. In python classes are also considered objects.",
        },
        {
          "owner" {
            "reputation" 1176,
            "display_name" "Carson"
          },
          "is_accepted" false,
          "score" 4,
          "last_activity_date" 1576839811,
          "answer_id" 59424178,
          "body_markdown" "Here&#39;s another example of what it can be used for:\r\n\r\n- You can use the ``metaclass`` to change the function of its instance (the class).\r\n\r\n```\r\nclass MetaMemberControl(type):\r\n    __slots__ = ()\r\n\r\n    @classmethod\r\n    def __prepare__(mcs, f_cls_name, f_cls_parents,  # f_cls means: future class\r\n                    meta_args=None, meta_options=None):  # meta_args and meta_options is not necessarily needed, just so you know.\r\n        f_cls_attr = dict()\r\n        if not &quot;do something or if you want to define your cool stuff of dict...&quot;:\r\n            return dict(make_your_special_dict=None)\r\n        else:\r\n            return f_cls_attr\r\n\r\n    def __new__(mcs, f_cls_name, f_cls_parents, f_cls_attr,\r\n                meta_args=None, meta_options=None):\r\n\r\n        original_getattr = f_cls_attr.get(&#39;__getattribute__&#39;)\r\n        original_setattr = f_cls_attr.get(&#39;__setattr__&#39;)\r\n\r\n        def init_getattr(self, item):\r\n            if not item.startswith(&#39;_&#39;):  # you can set break points at here\r\n                alias_name = &#39;_&#39; + item\r\n                if alias_name in f_cls_attr[&#39;__slots__&#39;]:\r\n                    item = alias_name\r\n            if original_getattr is not None:\r\n                return original_getattr(self, item)\r\n            else:\r\n                return super(eval(f_cls_name), self).__getattribute__(item)\r\n\r\n        def init_setattr(self, key, value):\r\n            if not key.startswith(&#39;_&#39;) and (&#39;_&#39; + key) in f_cls_attr[&#39;__slots__&#39;]:\r\n                raise AttributeError(f&quot;you can&#39;t modify private members:_{key}&quot;)\r\n            if original_setattr is not None:\r\n                original_setattr(self, key, value)\r\n            else:\r\n                super(eval(f_cls_name), self).__setattr__(key, value)\r\n\r\n        f_cls_attr[&#39;__getattribute__&#39;] = init_getattr\r\n        f_cls_attr[&#39;__setattr__&#39;] = init_setattr\r\n\r\n        cls = super().__new__(mcs, f_cls_name, f_cls_parents, f_cls_attr)\r\n        return cls\r\n\r\n\r\nclass Human(metaclass=MetaMemberControl):\r\n    __slots__ = (&#39;_age&#39;, &#39;_name&#39;)\r\n\r\n    def __init__(self, name, age):\r\n        self._name = name\r\n        self._age = age\r\n\r\n    def __getattribute__(self, item):\r\n        &quot;&quot;&quot;\r\n        is just for IDE recognize.\r\n        &quot;&quot;&quot;\r\n        return super().__getattribute__(item)\r\n\r\n    &quot;&quot;&quot; with MetaMemberControl then you don&#39;t have to write as following\r\n    @property\r\n    def name(self):\r\n        return self._name\r\n\r\n    @property\r\n    def age(self):\r\n        return self._age\r\n    &quot;&quot;&quot;\r\n\r\n\r\ndef test_demo():\r\n    human = Human(&#39;Carson&#39;, 27)\r\n    # human.age = 18  # you can&#39;t modify private members:_age  &lt;-- this is defined by yourself.\r\n    # human.k = 18  # &#39;Human&#39; object has no attribute &#39;k&#39;  &lt;-- system error.\r\n    age1 = human._age  # It&#39;s OK, although the IDE will show some warnings. (Access to a protected member _age of a class)\r\n\r\n    age2 = human.age  # It&#39;s OK! see below:\r\n    &quot;&quot;&quot;\r\n    if you do not define `__getattribute__` at the class of Human,\r\n    the IDE will show you: Unresolved attribute reference &#39;age&#39; for class &#39;Human&#39;\r\n    but it&#39;s ok on running since the MetaMemberControl will help you.\r\n    &quot;&quot;&quot;\r\n\r\n\r\nif __name__ == &#39;__main__&#39;:\r\n    test_demo()\r\n\r\n```\r\n\r\nThe ``metaclass`` is powerful, there are many things (such as monkey magic) you can do with it, but be careful this may only be known to you.",
        },
        {
          "owner" {
            "reputation" 721,
            "display_name" "Swati Srivastava"
          },
          "is_accepted" false,
          "score" 3,
          "last_activity_date" 1579503572,
          "answer_id" 59818321,
          "body_markdown" "A class, in Python, is an object, and just like any other object, it is an instance of &quot;something&quot;. This &quot;something&quot; is what is termed as a Metaclass. This metaclass is a special type of class that creates other class&#39;s objects. Hence, metaclass is responsible for making new classes. This allows the programmer to customize the way classes are generated.\r\n\r\nTo create a metaclass, overriding of __new__() and __init__() methods is usually done. __new__() can be overridden to change the way objects are created, while __init__() can be overridden to change the way of initializing the object. Metaclass can be created by a number of ways. One of the ways is to use type() function. type() function, when called with 3 parameters, creates a metaclass. The parameters are :-\r\n\r\n1. Class Name\r\n2. Tuple having base classes inherited by class\r\n3. A dictionary having all class methods and class variables\r\n\r\nAnother way of creating a metaclass comprises of &#39;metaclass&#39; keyword. Define the metaclass as a simple class. In the parameters of inherited class, pass metaclass=metaclass_name\r\n\r\nMetaclass can be specifically used in the following situations :-\r\n\r\n1. when a particular effect has to be applied to all the subclasses\r\n2. Automatic change of class (on creation) is required\r\n3. By API developers",
        },
        {
          "owner" {
            "reputation" 1223,
            "display_name" "Lars"
          },
          "is_accepted" false,
          "score" 4,
          "last_activity_date" 1583230012,
          "answer_id" 60504738,
          "body_markdown" "Note that in python 3.6 a new dunder method `__init_subclass__(cls, **kwargs)` was introduced to replace a lot of common use cases for metaclasses. Is is called when a subclass of the defining class is created. See [python docs].\r\n\r\n\r\n  [python docs]: https://docs.python.org/3.6/reference/datamodel.html",
        },
        {
          "comments" [
            {
              "owner" {
                "reputation" 158,
                "display_name" "swastik"
              },
              "score" 0,
              "post_type" "answer",
              "creation_date" 1604255973,
              "post_id" 60912261,
              "comment_id" 114285208,
              "body_markdown" "Please add relevant information ..your comments are confusing",
            }
          ],
          "owner" {
            "reputation" 9,
            "display_name" "Technical A.D."
          },
          "is_accepted" false,
          "score" -6,
          "last_activity_date" 1585473957,
          "answer_id" 60912261,
          "body_markdown" "Metaclass is a kind of class which defines how the class will behave like or we can say that A class is itself an instance of a metaclass. ",
        },
        {
          "owner" {
            "reputation" 529,
            "display_name" "Neeraj Bansal"
          },
          "is_accepted" false,
          "score" 1,
          "last_activity_date" 1594286177,
          "answer_id" 62811490,
          "body_markdown" "\r\n**Defination:**  \r\nA metaclass is a class whose instances are classes. Like an &quot;ordinary&quot; class defines the behavior of the instances of the class, a metaclass defines the behavior of classes and their instances.\r\n\r\nMetaclasses are not supported by every object oriented programming language. Those programming language, which support metaclasses, considerably vary in way they implement them. Python is supporting them.\r\n\r\nSome programmers see metaclasses in Python as &quot;solutions waiting or looking for a problem&quot;.\r\n\r\nThere are numerous use cases for metaclasses.  \r\n&gt;     logging and profiling\r\n&gt;     interface checking\r\n&gt;     registering classes at creation time\r\n&gt;     automatically adding new methods\r\n&gt;     automatic property creation\r\n&gt;     proxies\r\n&gt;     automatic resource locking/synchronization.\r\n\r\n**Defining Meta class:**  \r\nit will print the content of its arguments in the __new__ method and returns the results of the type.__new__ call:\r\n\r\n    class LittleMeta(type):\r\n        def __new__(cls, clsname, superclasses, attributedict):\r\n            print(&quot;clsname: &quot;, clsname)\r\n            print(&quot;superclasses: &quot;, superclasses)\r\n            print(&quot;attributedict: &quot;, attributedict)\r\n            return type.__new__(cls, clsname, superclasses, attributedict)\r\n\r\nWe will use the metaclass &quot;LittleMeta&quot; in the following example:\r\n\r\n    class S:\r\n        pass    \r\n    class A(S, metaclass=LittleMeta):\r\n        pass    \r\n    a = A()\r\n\r\n**Output:**  \r\n\r\n    clsname:  A\r\n    superclasses:  (&lt;class &#39;__main__.S&#39;&gt;,)\r\n    attributedict:  {&#39;__module__&#39;: &#39;__main__&#39;, &#39;__qualname__&#39;: &#39;A&#39;}\r\n\r\n\r\n\r\n",
        },
        {
          "owner" {
            "reputation" 1148,
            "display_name" "Usama Abdulrehman"
          },
          "is_accepted" false,
          "score" 1,
          "last_activity_date" 1594787698,
          "answer_id" 62907685,
          "body_markdown" "A `metaclass` in Python is a class of a class that defines how a class behaves. A class is itself an instance of a `metaclass`. A class in Python defines how the instance of the class will behave. We can customize the class creation process by passing the `metaclass` keyword in the class definition. This can also be done by inheriting a class that has already passed in this keyword.\r\n\r\n```\r\nclass MyMeta(type):\r\n    pass\r\n\r\nclass MyClass(metaclass=MyMeta):\r\n    pass\r\n\r\nclass MySubclass(MyClass):\r\n    pass\r\n```\r\nWe can see that the type of `MyMeta` class is `type` and that the type of `MyClass` and `MySubClass` is `MyMeta`.\r\n\r\n```\r\nprint(type(MyMeta))\r\nprint(type(MyClass))\r\nprint(type(MySubclass))\r\n\r\n&lt;class &#39;type&#39;&gt;\r\n&lt;class &#39;__main__.MyMeta&#39;&gt;\r\n&lt;class &#39;__main__.MyMeta&#39;&gt;\r\n```\r\nWhen defining a class and no `metaclass` is defined the default type `metaclass` will be used. If a `metaclass` is given and it is not an instance of `type()`, then it is used directly as the `metaclass`.\r\n\r\nMetaclasses can be applied in logging, registration of classes at creation time and profiling among others. They seem to be quite abstract concepts, and you might be wondering if you need to use them at all. ",
        }
      ],
      "owner" {
        "reputation" 500965,
        "display_name" "e-satis"
      },
      "view_count" 849389,
      "score" 5981,
      "last_activity_date" 1600437653,
      "question_id" 100003,
      "body_markdown" "In Python, what are metaclasses and what do we use them for?",
      "title" "What are metaclasses in Python?"
    }
  ],
  "has_more" true,
  "quota_max" 10000,
  "quota_remaining" 9966
})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ui/run-input-loop items))

(def w (as->
         (staxchg.state/initialize-world staxchg.core/items 118 37) v
         (assoc v :selected-question-index 2)
         (assoc-in
           v
           [:line-offsets
            (get-in v [:questions (v :selected-question-index) "question_id"])
            (v :active-pane)]
           2)))

