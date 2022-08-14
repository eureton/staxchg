(ns staxchg.post
  (:require [staxchg.code :as code]
            [staxchg.markdown :as markdown])
  (:gen-class))

(defn answer?
  "True if post is an answer, false otherwise."
  [post]
  (contains? post "answer_id"))

(defn question?
  "True if post is a question, false otherwise."
  [post]
  (and (not (answer? post))
       (contains? post "question_id")))

(defn df
  "Dispatch function for multimethods in this namespace."
  [post]
  (cond
    (question? post) :question
    (answer? post) :answer))

(defmulti id
  "StackExchange post ID."
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

(def stackexchange-syntax-tags
  "Set of relevant syntax tags used by StackExchange."
  #{"4d" "abc" "asn.1" "abnf" "access-log" "ada" "arduino" "armasm" "avr"
    "actionscript" "apache-config" "apache" "applescript" "arcade" "asciidoc"
    "aspectj" "autohotkey" "autoit" "awk" "gawk" "nawk" "mawk" "bash" "sh" "zsh"
    "basic" "bbcode" "blade" "bnf" "brainfuck" "c#" "c" "c++" "cal" "cls"
    "cmake" "common-lisp" "coq" "css" "capnproto" "chaos" "chapel" "cisco"
    "clojure" "coffeescript" "cson" "crystal-lang" "cypher" "d" "dns" "zone"
    "bind" "dos" "batch-file" "cmd" "dart" "delphi" "dfm" "django-templates"
    "pascal" "freepascal" "lazarus" "lpr" "diff" "patch" "django" "jinja2"
    "jinjava" "dockerfile" "docker" "dts" "dust.js" "dst" "dust-helpers" "dustc"
    "dylan" "ebnf" "elixir" "elm" "erlang" "erl" "excel" "xls" "xlsx" "f#"
    "fortran" "fortran90" "fortran95" "g-code" "gauss" "gnu-assembler" "gss"
    "godot" "gdscript" "gherkin" "glimmer.js" "handlebars.js" "handlebars.java"
    "handlebars.net" "htmlbars" "go" "gf" "gradle" "groovy" "xml" "html" "xhtml"
    "rss" "atom" "xjb" "xsd" "plist" "svg" "http" "https" "haml" "haskell"
    "haxe" "hlsl" "hy" "toml" "inform7" "json" "java" "jsp" "javascript" "js"
    "jsx" "jolie" "julia" "kotlin" "latex" "tex" "leaf" "lean" "lasso-lang"
    "less" "ldif" "lisp" "livecode" "livescript" "lua" "makefile" "markdown"
    "wolfram-mathematica" "mathematica-8" "matlab" "maxima" "mel" "mercury"
    "mirc" "mizar" "modeline" "modula-2" "modula-3" "mojolicious"
    "mojolicious-lite" "monkey" "moonscript" "n1ql" "nsis" "nginx"
    "nginx-config" "nimrod" "nix" "ocl" "ocaml" "ml" "objective-c"
    "objective-c++" "glsl" "openscad" "oxygene" "php" "php4" "php-5.2" "php-5.3"
    "php-5.4" "php-5.5" "php-5.6" "php-7" "php-7.0" "php-7.1" "php-7.2"
    "php-7.3" "php-7.4" "php-8" "papyrus" "perl" "pony" "plpgsql" "postgresql"
    "powershell" "prolog" "puppet" "python" "gyp" "q#" "kdb" "qml" "r" "cshtml"
    "razor" "reason" "reasonml" "rebol" "red-system" "relaxng-compact" "rsl"
    "rpm-spec" "ruby" "gemspecs" "podspec" "thor" "irb" "rust" "sas" "scss"
    "sql" "step" "scala" "scheme" "scilab" "sci" "smali" "smalltalk" "solidity"
    "splunk" "spl" "stan" "stata" "stylus" "supercollider" "svelte" "swift"
    "tcl" "tk" "terraform" "tf" "hcl" "tap" "thrift" "tsql" "twig" "craftcms"
    "typescript" "unicorn-rails-log" "vb.net" "vb6" "vba" "vbscript" "vhdl"
    "vala" "verilog" "vim" "axapta" "x++" "x86" "tao" "xquery" "xpath" "xq"
    "yaml" "zs" "zephir"})

(defn code-info
  "Sequence of hashes, one for each code block within the markdown of the post.
   Each hash contains:
     * a set of StackExchange syntax tags under :syntax
     * a string containing the tab-expanded code under :string
     * the info string under :info (if code block is fenced and has one)"
  [{:strs [tags question_id answer_id body_markdown]}]
  (let [tags (set tags)
        fix-syntax (comp #(clojure.set/intersection % stackexchange-syntax-tags)
                         #(if ((every-pred some? (comp not set?)) %) #{%} %)
                         #(or %1 %2))
        annotate (comp #(update % :string code/expand-tabs (first (:syntax %)))
                       #(update % :syntax fix-syntax tags)
                       #(cond-> %
                                answer_id (assoc :answer-id answer_id)
                                question_id (assoc :question-id question_id)))]
    (->> body_markdown
         staxchg.markdown/code-info
         (map annotate)
         (remove (comp empty? :syntax)))))

