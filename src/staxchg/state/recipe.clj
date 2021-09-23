(ns staxchg.state.recipe
  (:require [clojure.string :as string])
  (:require [staxchg.api :as api])
  (:require [staxchg.state :as state])
  (:require [staxchg.presentation :as presentation])
  (:gen-class))

(def poll-loop-latency
  "The number of milliseconds to sleep for between poll runs."
  500)

(def syntax-map
  "A 2-level hash which maps StackExchange tags to syntax IDs which the
   highlighters support.
   The first level is keyed to the highligher keyword.
   The second level is keyed to the tag."
  {:skylighting {"abc" "Abc"
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
                 "relaxng-compact" "relaxngcompact"}
   :highlight.js {"access-log" "accesslog"
                  "avr" "avrasm"
                  "apache-config" "apacheconf"
                  "common-lisp" "lisp"
                  "crystal-lang" "crystal"
                  "batch-file" "bat"
                  "django-templates" "django"
                  "jinja2" "jinja"
                  "jinjava" "jinja"
                  "dust.js" "dust"
                  "dust-helpers" "dust"
                  "dustc" "dust"
                  "fortran90" "f90"
                  "fortran95" "f95"
                  "glimmer.js" "glimmer"
                  "handlebars.js" "handlebars"
                  "handlebars.java" "handlebars"
                  "handlebars.net" "handlebars"
                  "lasso-lang" "lasso"
                  "livecode" "livecodeserver"
                  "wolfram-mathematica" "mathematica"
                  "mathematica-8" "mathematica"
                  "mojolicious-lite" "mojolicious"
                  "nginx-config" "nginxconf"
                  "objective-c" "obj-c"
                  "objective-c++" "obj-c++"
                  "php-5.2" "php5"
                  "php-5.3" "php5"
                  "php-5.4" "php5"
                  "php-5.5" "php5"
                  "php-5.6" "php5"
                  "php-7" "php7"
                  "php-7.0" "php7"
                  "php-7.1" "php7"
                  "php-7.2" "php7"
                  "php-7.3" "php7"
                  "php-7.4" "php7"
                  "php-8" "php8"
                  "plpgsql" "postgresql"
                  "reason" "reasonml"
                  "gemspecs" "gemspec"
                  "vb6" "vb.net"
                  "vba" "vb.net"
                  "vbscript" "vb.net"
                  "x86" "x86asm"}
   :pygments {}})

(def syntax-ids
  "A hash for validating highlighter syntax IDs. Its keys are the highlighter
   keywords and its values are sets of valid syntax IDs."
  {:skylighting #{"Abc" "asn1" "asp" "ats" "awk" "actionscript" "ada" "agda"
                  "alert" "alertindent" "apache" "bash" "bibtex" "boo" "c" "cs"
                  "cpp" "cmake" "css" "changelog" "clojure" "coffee"
                  "coldfusion" "commonlisp" "curry" "d" "dtd" "diff"
                  "djangotemplate" "dockerfile" "doxygen" "doxygenlua" "eiffel"
                  "elixir" "email" "erlang" "fsharp" "fortran" "gcc" "glsl"
                  "gnuassembler" "m4" "go" "html" "hamlet" "haskell" "haxe"
                  "ini" "isocpp" "idris" "fasm" "nasm" "json" "jsp" "java"
                  "javascript" "javadoc" "julia" "kotlin" "llvm" "latex" "lex"
                  "lilypond" "literatecurry" "literatehaskell" "lua" "mips"
                  "makefile" "markdown" "mathematica" "matlab" "maxima"
                  "mediawiki" "metafont" "modelines" "modula2" "modula3"
                  "monobasic" "ocaml" "objectivec" "objectivecpp" "octave"
                  "opencl" "php" "pascal" "perl" "pike" "postscript" "prolog"
                  "pure" "purebasic" "python" "r" "relaxng" "relaxngcompact"
                  "roff" "ruby" "rhtml" "rust" "sgml" "sql" "sqlmysql"
                  "sqlpostgresql" "scala" "scheme" "tcl" "tcsh" "texinfo"
                  "mandoc" "vhdl" "verilog" "xml" "xul" "yaml" "yacc" "zsh"
                  "dot" "noweb" "rest" "sci" "sed" "xorg" "xslt"}
   :highlight.js #{"1c" "4d" "abnf" "accesslog" "ada" "arduino" "ino" "armasm"
                   "arm" "avrasm" "actionscript" "as" "alan" "angelscript" "asc"
                   "apache" "apacheconf" "applescript" "osascript" "arcade"
                   "asciidoc" "adoc" "aspectj" "autohotkey" "autoit" "awk"
                   "mawk" "nawk" "gawk" "bash" "sh" "zsh" "basic" "bbcode"
                   "blade" "bnf" "brainfuck" "bf" "c#" "csharp" "cs" "c" "c++"
                   "cpp" "hpp" "cc" "hh" "h++" "cxx" "hxx" "c/al" "cal" "cos"
                   "cls" "cmake" "cmake.in" "coq" "csp" "css" "capnproto"
                   "capnp" "chaos" "kaos" "chapel" "chpl" "cisco" "clojure"
                   "clj" "coffeescript" "coffee" "cson" "iced" "cpcdosc+" "cpc"
                   "crmsh" "crm" "pcmk" "crystal" "cr" "cypher" "d" "dns"
                   "zone" "bind" "dos" "bat" "cmd" "dart" "delphi" "dpr" "dfm"
                   "pas" "pascal" "freepascal" "lazarus" "lpr" "lfm" "diff"
                   "patch" "django" "jinja" "dockerfile" "docker" "dsconfig"
                   "dts" "dust" "dst" "dylan" "ebnf" "elixir" "elm" "erlang"
                   "erl" "excel" "xls" "xlsx" "extempore" "xtlang" "xtm" "f#"
                   "fsharp" "fs" "fix" "fortran" "f90" "f95" "g-code" "gcode"
                   "nc" "gams" "gms" "gauss" "gss" "godot" "gdscript" "gherkin"
                   "hbs" "glimmer" "html.hbs" "htmlbars" "gni" "go" "golang"
                   "gf" "golo" "gololang" "gradle" "groovy" "xml" "html" "xhtml"
                   "rss" "atom" "xjb" "xsd" "xsl" "plist" "svg" "http" "https"
                   "haml" "handlebars" "html.handlebars" "haskell" "hs" "haxe"
                   "hx" "hlsl" "hy" "hylang" "toml" "inform7" "i7" "irpf90"
                   "json" "java" "jsp" "javascript" "js" "jsx" "jolie" "iol"
                   "ol" "julia" "julia-repl" "kotlin" "kt" "latex" "tex" "leaf"
                   "lean" "lasso" "lassoscript" "less" "ldif" "lisp"
                   "livecodeserver" "livescript" "lua" "makefile" "make"
                   "markdown" "md" "mkdown" "mkd" "mathematica" "mma" "wl"
                   "matlab" "maxima" "mel" "mercury" "mirc" "mrc" "mizar"
                   "mojolicious" "monkey" "moonscript" "moon" "n1ql" "nsis"
                   "never" "nginx" "nginxconf" "nim" "nimrod" "nix" "ocl"
                   "ocaml" "objectivec" "mm" "objc" "obj-c" "obj-c++"
                   "objective-c++" "glsl" "openscad" "scad" "ruleslanguage"
                   "oxygene" "pf" "pf.conf" "php" "php3" "php4" "php5" "php6"
                   "php7" "php8" "papyrus" "psc" "parser3" "perl" "pl" "pm"
                   "pony" "pgsql" "postgres" "postgresql" "powershell" "ps"
                   "ps1" "prolog" "protobuf" "puppet" "pp" "python" "py" "gyp"
                   "profile" "python-repl" "pycon" "q#" "qsharp" "kdb" "qml" "r"
                   "cshtml" "razor" "razor-cshtml" "reasonml" "re" "redbol"
                   "rebol" "red" "red-system" "rib" "rsl" "risc" "riscript"
                   "roboconf" "robot" "rf" "rpm-specfile" "rpm" "spec"
                   "rpm-spec" "specfile" "ruby" "rb" "gemspec" "podspec" "thor"
                   "irb" "rust" "rs" "sas" "scss" "sql" "p21" "step" "stp"
                   "scala" "scheme" "scilab" "sci" "shexc" "shell" "console"
                   "smali" "smalltalk" "st" "sml" "ml" "solidity" "sol" "splunk"
                   "spl" "stan" "stanfuncs" "stata" "iecst" "scl" "stl"
                   "structured-text" "stylus" "styl" "subunit" "supercollider"
                   "sc" "svelte" "swift" "tcl" "tk" "terraform" "tf" "hcl" "tap"
                   "thrift" "tp" "tsql" "twig" "craftcms" "typescript" "ts"
                   "unicorn-rails-log" "vb.net" "vbnet" "vb" "vba" "vbscript"
                   "vbs" "vhdl" "vala" "verilog" "v" "vim" "axapta x++" "x86asm"
                   "xl" "tao" "xquery" "xpath" "xq" "yml" "yaml" "zenscript"
                   "zs" "zephir" "zep"}
   :pygments #{}})

(defn sanitize-syntax
  "Returns the syntax ID which corresponds to syntax for the highlighter.
   If no syntax ID does, returns nil."
  [highlighter syntax]
  (let [valid (syntax-ids highlighter)
        translated (get-in syntax-map [highlighter syntax] syntax)]
    (get valid translated)))

(defn highlight-code-step-df
  "Dispatch function for staxchg.state.recipe/highlight-code-step"
  [_ highlighter]
  highlighter)

(defmulti highlight-code-step
  "Returns a recipe step hash for highlighting snippet with highlighter."
  highlight-code-step-df)

(defmethod highlight-code-step :skylighting
  [{:keys [string syntax question-id answer-id]} _]
  (when-some [syntax (->> syntax
                          (map sanitize-syntax (repeat :skylighting))
                          (remove nil?)
                          first)]
    {:function :staxchg.io/run-skylighting!
     :params [string syntax question-id answer-id]}))

(defmethod highlight-code-step :highlight.js
  [{:keys [string syntax question-id answer-id]} _]
  (when-some [syntax (->> syntax
                          (map sanitize-syntax (repeat :highlight.js))
                          (remove nil?)
                          set)]
    {:function :staxchg.io/run-highlight.js!
     :params [string syntax question-id answer-id]}))

(defmethod highlight-code-step :pygments
  [snippet _]
  (highlight-code-step snippet :skylighting))

(defn input-df
  "Dispatch function for staxchg.state.recipe/input"
  [{:as world
    :keys [query? questions search-term fetch-answers no-questions no-answers
           fetch-failed snippets quit? width]
    {:keys [screen]} :io/context}]
  (cond (nil? screen)      :initialize
        (nil? width)       :enable-screen
        snippets           :snippets
        search-term        :search-term
        fetch-answers      :fetch-answers
        no-questions       :no-questions
        no-answers         :no-answers
        fetch-failed       :fetch-failed
        query?             :query
        (empty? questions) :query
        quit?              :quit
        :else              :sleep))

(defmulti input
  "Recipes for requesting input, as required by world."
  input-df)

(defmethod input :initialize
  [_]
  [[{:function :staxchg.io/register-theme!
     :params ["staxchg" "lanterna-theme.properties"]}
    {:function :staxchg.io/acquire-screen!
     :params []}
    {:function :staxchg.io/resolve-highlighter!
     :params []}]])

(defmethod input :enable-screen
  [_]
  [[{:function :staxchg.io/enable-screen!
     :params [:screen]}]])

(defmethod input :snippets
  [{:io/keys [context] :keys [snippets]}]
  (list (map highlight-code-step snippets (repeat (get context :highlighter)))))

(defmethod input :search-term
  [{:keys [search-term]}]
  [[{:function :staxchg.io/fetch-questions!
     :params [:screen
              (api/questions-url)
              (api/questions-query-params search-term)]}]])

(defmethod input :fetch-answers
  [{:keys [fetch-answers]}]
  [[{:function :staxchg.io/fetch-answers!
     :params [:screen
              (api/answers-url (fetch-answers :question-id))
              (api/answers-query-params (fetch-answers :page))
              (fetch-answers :question-id)]}]])

(defmethod input :no-questions
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "No matches found"}
              {:function :no-questions! :values []}]}]])

(defmethod input :no-answers
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:text "Question has no answers"}
              {:function :no-answers! :values []}]}]])

(defmethod input :fetch-failed
  [_]
  [[{:function :staxchg.io/show-message!
     :params [:screen
              {:title "Error" :text "Could not fetch data"}
              {:function :fetch-failed! :values []}]}]])

(defmethod input :query
  [_]
  [[{:function :staxchg.io/query!
     :params [:screen presentation/search-legend]}]])

(defmethod input :quit
  [_]
  [[{:function :staxchg.io/quit!
     :params [:screen]}]])

(defmethod input :sleep
  [_]
  [[{:function :staxchg.io/sleep!
     :params [poll-loop-latency]}
    {:function :staxchg.io/poll-resize!
     :params [:screen]}
    {:function :staxchg.io/poll-key!
     :params [:screen]}]])

(defn output
  "Recipes for requesting output, as required by world."
  [world]
  (if (state/render? world)
    (presentation/recipes world)
    []))

(def all
  "Recipes requests, as required by world. First come the output, then the
   output recipes."
  (comp (partial apply concat)
        (juxt output input)))

(defn request
  "Packages recipes in the format smachine expects."
  [world]
  {:recipes (all world)
   :context (:io/context world)})

