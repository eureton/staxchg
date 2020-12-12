(defproject staxchg "0.1.0-SNAPSHOT"
  :description "A Stack Exchange client for the terminal"
  :url "https://github.com/eureton/staxchg"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-http "3.10.3"]
                 [cheshire "5.10.0"]
                 [clojure-lanterna "0.9.7"]]
  :main ^:skip-aot staxchg.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
