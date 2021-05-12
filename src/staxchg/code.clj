(ns staxchg.code
  (:gen-class))

(def tabstops
  "Associates syntaxes to the number of spaces a tab should expand to."
  {"asp" 2
   "ada" 3
   "bash" 2
   "c" 4
   "cs" 4
   "cpp" 4
   "cmake" 4
   "css" 2
   "clojure" 2
   "commonlisp" 2
   "djangotemplate" 4
   "dockerfile" 4
   "eiffel" 2
   "elixir" 2
   "erlang" 2
   "fsharp" 4
   "fortran" 4
   "glsl" 4
   "go" 2
   "html" 2
   "haskell" 2
   "isocpp" 4
   "idris" 2
   "fasm" 4
   "nasm" 4
   "json" 4
   "jsp" 4
   "java" 4
   "javascript" 2
   "javadoc" 4
   "kotlin" 4
   "lua" 2
   "makefile" 4
   "matlab" 2
   "ocaml" 2
   "objectivec" 4
   "objectivecpp" 4
   "php" 4
   "pascal" 2
   "perl" 4
   "prolog" 8
   "python" 4
   "r" 2
   "ruby" 2
   "rust" 4
   "scala" 2
   "scheme" 2
   "tcl" 4
   "tcsh" 4
   "vhdl" 4
   "verilog" 2
   "xml" 2
   "zsh" 4})

(def default-tabstop
  "The number of spaces that tabs will be expanded to if syntax is neither
   specified nor can be derived."
  4)

(defn expand-tabs
  "Replaces all tabs in the contained markdown with the appropriate number of
   spaces, as corresponds to the given syntax."
  [string syntax]
  (when (some? string)
    (let [expanded (-> syntax
                       (tabstops default-tabstop)
                       (repeat \space)
                       clojure.string/join)]
      (clojure.string/replace string #"\t" expanded))))

