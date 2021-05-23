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

(def highlight.js-syntax-ids #{"1c" "4d" "abnf" "accesslog" "ada" "arduino"
                               "ino" "armasm" "arm" "avrasm" "actionscript" "as"
                               "alan" "angelscript" "asc" "apache" "apacheconf"
                               "applescript" "osascript" "arcade" "asciidoc"
                               "adoc" "aspectj" "autohotkey" "autoit" "awk"
                               "mawk" "nawk" "gawk" "bash" "sh" "zsh" "basic"
                               "bbcode" "blade" "bnf" "brainfuck" "bf" "c#"
                               "csharp" "cs" "c" "c++" "cpp" "hpp" "cc" "hh"
                               "h++" "cxx" "hxx" "c/al" "cal" "cos" "cls"
                               "cmake" "cmake.in" "coq" "csp" "css" "capnproto"
                               "capnp" "chaos" "kaos" "chapel" "chpl" "cisco"
                               "clojure" "clj" "coffeescript" "coffee" "cson"
                               "iced" "cpcdosc+" "cpc" "crmsh" "crm" "pcmk"
                               "crystal" "cr" "cypher" "d" "dns" "zone" "bind"
                               "dos" "bat" "cmd" "dart" "delphi" "dpr" "dfm"
                               "pas" "pascal" "freepascal" "lazarus" "lpr" "lfm"
                               "diff" "patch" "django" "jinja" "dockerfile"
                               "docker" "dsconfig" "dts" "dust" "dst" "dylan"
                               "ebnf" "elixir" "elm" "erlang" "erl" "excel"
                               "xls" "xlsx" "extempore" "xtlang" "xtm" "f#"
                               "fsharp" "fs" "fix" "fortran" "f90" "f95"
                               "g-code" "gcode" "nc" "gams" "gms" "gauss" "gss"
                               "godot" "gdscript" "gherkin" "hbs" "glimmer"
                               "html.hbs" "htmlbars" "gni" "go" "golang" "gf"
                               "golo" "gololang" "gradle" "groovy" "xml" "html"
                               "xhtml" "rss" "atom" "xjb" "xsd" "xsl" "plist"
                               "svg" "http" "https" "haml" "handlebars"
                               "html.handlebars" "haskell" "hs" "haxe" "hx"
                               "hlsl" "hy" "hylang" "toml" "inform7" "i7"
                               "irpf90" "json" "java" "jsp" "javascript" "js"
                               "jsx" "jolie" "iol" "ol" "julia" "julia-repl"
                               "kotlin" "kt" "latex" "tex" "leaf" "lean" "lasso"
                               "lassoscript" "less" "ldif" "lisp"
                               "livecodeserver" "livescript" "lua" "makefile"
                               "make" "markdown" "md" "mkdown" "mkd"
                               "mathematica" "mma" "wl" "matlab" "maxima" "mel"
                               "mercury" "mirc" "mrc" "mizar" "mojolicious"
                               "monkey" "moonscript" "moon" "n1ql" "nsis"
                               "never" "nginx" "nginxconf" "nim" "nimrod" "nix"
                               "ocl" "ocaml" "objectivec" "mm" "objc" "obj-c"
                               "obj-c++" "objective-c++" "glsl" "openscad"
                               "scad" "ruleslanguage" "oxygene" "pf" "pf.conf"
                               "php" "php3" "php4" "php5" "php6" "php7" "php8"
                               "papyrus" "psc" "parser3" "perl" "pl" "pm" "pony"
                               "pgsql" "postgres" "postgresql" "powershell" "ps"
                               "ps1" "prolog" "protobuf" "puppet" "pp" "python"
                               "py" "gyp" "profile" "python-repl" "pycon" "q#"
                               "qsharp" "kdb" "qml" "r" "cshtml" "razor"
                               "razor-cshtml" "reasonml" "re" "redbol" "rebol"
                               "red" "red-system" "rib" "rsl" "risc" "riscript"
                               "roboconf" "robot" "rf" "rpm-specfile" "rpm"
                               "spec" "rpm-spec" "specfile" "ruby" "rb"
                               "gemspec" "podspec" "thor" "irb" "rust" "rs"
                               "sas" "scss" "sql" "p21" "step" "stp" "scala"
                               "scheme" "scilab" "sci" "shexc" "shell" "console"
                               "smali" "smalltalk" "st" "sml" "ml" "solidity"
                               "sol" "splunk" "spl" "stan" "stanfuncs" "stata"
                               "iecst" "scl" "stl" "structured-text" "stylus"
                               "styl" "subunit" "supercollider" "sc" "svelte"
                               "swift" "tcl" "tk" "terraform" "tf" "hcl" "tap"
                               "thrift" "tp" "tsql" "twig" "craftcms"
                               "typescript" "ts" "unicorn-rails-log" "vb.net"
                               "vbnet" "vb" "vba" "vbscript" "vbs" "vhdl" "vala"
                               "verilog" "v" "vim" "axapta x++" "x86asm" "xl"
                               "tao" "xquery" "xpath" "xq" "yml" "yaml"
                               "zenscript" "zs" "zephir" "zep"})

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

