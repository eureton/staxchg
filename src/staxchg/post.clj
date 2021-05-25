(ns staxchg.post
  (:require [staxchg.markdown :as markdown])
  (:require [staxchg.code :as code])
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

(defn df
  "Dispatch function for post-based multimethods. Returns a keyword."
  [post]
  (cond
    (question? post) :question
    (answer? post) :answer))

(defmulti id
  "Returns the StackExchange post ID."
  df)

(defmethod id :question
  [post]
  (post "question_id"))

(defmethod id :answer
  [post]
  (post "answer_id"))

(defmethod id :default
  [_]
  nil)

(def stackexchange-syntax-tags #{"4d" "abc" "asn.1" "abnf" "access-log" "ada"
                                 "arduino" "armasm" "avr" "actionscript"
                                 "apache-config" "apache" "applescript" "arcade"
                                 "asciidoc" "aspectj" "autohotkey" "autoit"
                                 "awk" "gawk" "nawk" "mawk" "bash" "sh" "zsh"
                                 "basic" "bbcode" "blade" "bnf" "brainfuck" "c#"
                                 "c" "c++" "cal" "cls" "cmake" "common-lisp"
                                 "coq" "css" "capnproto" "chaos" "chapel"
                                 "cisco" "clojure" "coffeescript" "cson"
                                 "crystal-lang" "cypher" "d" "dns" "zone" "bind"
                                 "dos" "batch-file" "cmd" "dart" "delphi" "dfm"
                                 "django-templates" "pascal" "freepascal"
                                 "lazarus" "lpr" "diff" "patch" "django"
                                 "jinja2" "jinjava" "dockerfile" "docker" "dts"
                                 "dust.js" "dst" "dust-helpers" "dustc" "dylan"
                                 "ebnf" "elixir" "elm" "erlang" "erl" "excel"
                                 "xls" "xlsx" "f#" "fortran" "fortran90"
                                 "fortran95" "g-code" "gauss" "gnu-assembler"
                                 "gss" "godot" "gdscript" "gherkin" "glimmer.js"
                                 "handlebars.js" "handlebars.java"
                                 "handlebars.net" "htmlbars" "go" "gf" "gradle"
                                 "groovy" "xml" "html" "xhtml" "rss" "atom"
                                 "xjb" "xsd" "plist" "svg" "http" "https" "haml"
                                 "haskell" "haxe" "hlsl" "hy" "toml" "inform7"
                                 "json" "java" "jsp" "javascript" "js" "jsx"
                                 "jolie" "julia" "kotlin" "latex" "tex" "leaf"
                                 "lean" "lasso-lang" "less" "ldif" "lisp"
                                 "livecode" "livescript" "lua" "makefile"
                                 "markdown" "wolfram-mathematica"
                                 "mathematica-8" "matlab" "maxima" "mel"
                                 "mercury" "mirc" "mizar" "modeline" "modula-2"
                                 "modula-3" "mojolicious" "mojolicious-lite"
                                 "monkey" "moonscript" "n1ql" "nsis" "nginx"
                                 "nginx-config" "nimrod" "nix" "ocl" "ocaml"
                                 "ml" "objective-c" "objective-c++" "glsl"
                                 "openscad" "oxygene" "php" "php4" "php-5.2"
                                 "php-5.3" "php-5.4" "php-5.5" "php-5.6" "php-7"
                                 "php-7.0" "php-7.1" "php-7.2" "php-7.3"
                                 "php-7.4" "php-8" "papyrus" "perl" "pony"
                                 "plpgsql" "postgresql" "powershell" "prolog"
                                 "puppet" "python" "gyp" "q#" "kdb" "qml" "r"
                                 "cshtml" "razor" "reason" "reasonml" "rebol"
                                 "red-system" "relaxng-compact" "rsl" "rpm-spec"
                                 "ruby" "gemspecs" "podspec" "thor" "irb" "rust"
                                 "sas" "scss" "sql" "step" "scala" "scheme"
                                 "scilab" "sci" "smali" "smalltalk" "solidity"
                                 "splunk" "spl" "stan" "stata" "stylus"
                                 "supercollider" "svelte" "swift" "tcl" "tk"
                                 "terraform" "tf" "hcl" "tap" "thrift" "tsql"
                                 "twig" "craftcms" "typescript"
                                 "unicorn-rails-log" "vb.net" "vb6" "vba"
                                 "vbscript" "vhdl" "vala" "verilog" "vim"
                                 "axapta" "x++" "x86" "tao" "xquery" "xpath"
                                 "xq" "yaml" "zs" "zephir"})

(defn code-info
  ""
  [{:as post
    :strs [question_id answer_id]}]
  (let [tags (-> post (get "tags") set)
        fix-syntax (comp #(clojure.set/intersection % stackexchange-syntax-tags)
                         #(if ((every-pred some? (comp not set?)) %) #{%} %)
                         #(or %1 %2))
        annotate (comp #(update % :string code/expand-tabs (first (:syntax %)))
                       #(update % :syntax fix-syntax tags)
                       #(cond-> %
                                answer_id (assoc :answer-id answer_id)
                                question_id (assoc :question-id question_id)))]
    (->> (get post "body_markdown")
         staxchg.markdown/code-info
         (map annotate)
         (remove (comp empty? :syntax)))))

