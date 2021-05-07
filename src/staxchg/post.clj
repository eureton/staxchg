(ns staxchg.post
  (:require [staxchg.markdown :as markdown])
  (:gen-class))

(defn answer?
  "Returns true if post is an answer, false otherwise."
  [post]
  (contains? post "answer_id"))

(defn question?
  "Returns true if post is a question, false otherwise."
  [post]
  (and (not (answer? post))
       (contains? post "question_id")))

(def skylight-syntax-ids #{"Abc" "asn1" "asp" "ats" "awk" "actionscript" "ada"
                           "agda" "alert" "alertindent" "apache" "bash" "bibtex"
                           "boo" "c" "cs" "cpp" "cmake" "css" "changelog"
                           "clojure" "coffee" "coldfusion" "commonlisp" "curry"
                           "d" "dtd" "diff" "djangotemplate" "dockerfile"
                           "doxygen" "doxygenlua" "eiffel" "elixir" "email"
                           "erlang" "fsharp" "fortran" "gcc" "glsl"
                           "gnuassembler" "m4" "go" "html" "hamlet" "haskell"
                           "haxe" "ini" "isocpp" "idris" "fasm" "nasm" "json"
                           "jsp" "java" "javascript" "javadoc" "julia" "kotlin"
                           "llvm" "latex" "lex" "lilypond" "literatecurry"
                           "literatehaskell" "lua" "mips" "makefile" "markdown"
                           "mathematica" "matlab" "maxima" "mediawiki"
                           "metafont" "modelines" "modula2" "modula3"
                           "monobasic" "ocaml" "objectivec" "objectivecpp"
                           "octave" "opencl" "php" "pascal" "perl" "pike"
                           "postscript" "prolog" "pure" "purebasic" "python" "r"
                           "relaxng" "relaxngcompact" "roff" "ruby" "rhtml"
                           "rust" "sgml" "sql" "sqlmysql" "sqlpostgresql"
                           "scala" "scheme" "tcl" "tcsh" "texinfo" "mandoc"
                           "vhdl" "verilog" "xml" "xul" "yaml" "yacc" "zsh"
                           "dot" "noweb" "rest" "sci" "sed" "xorg" "xslt"})

(def stackexchange-to-skylight {"abc" "Abc"
                                "asn.1" "asn1"
                                "c#" "cs"
                                "c++" "cpp"
                                "common-lisp" "commonlisp"
                                "django-templates" "djangotemplate"
                                "f#" "fsharp"
                                "gnu-assembler" "gnuassembler"
                                "modeline" "modelines"
                                "modula-2" "modula2"
                                "modula-3" "modula3"
                                "objective-c" "objectivec"
                                "objective-c++" "objectivecpp"
                                "relaxng-compact" "relaxngcompact"})

(defn syntax-tag
  ""
  [post]
  (->> (get post "tags")
       (repeat 2)
       (apply map stackexchange-to-skylight)
       set
       (clojure.set/intersection skylight-syntax-ids)
       first))

(defn code-info
  ""
  [{:as post
    :strs [question_id answer_id]}]
  (let [syntax (syntax-tag post)
        set-if-nil-or-empty #(some not-empty %&)
        annotate (comp #(update % :syntax set-if-nil-or-empty syntax)
                       #(cond-> %
                                answer_id (assoc :answer-id answer_id)
                                question_id (assoc :question-id question_id)))]
    (->> (get post "body_markdown")
         staxchg.markdown/code-info
         (map annotate))))

