(defproject staxchg "0.1.0-SNAPSHOT"
  :description "A Stack Exchange client for the terminal"
  :url "https://github.com/eureton/staxchg"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.3.610"]
                 [clj-http "3.10.3"]
                 [cheshire "5.10.0"]
                 [com.googlecode.lanterna/lanterna "3.0.4"]
                 [org.jsoup/jsoup "1.7.3"]
                 [com.vladsch.flexmark/flexmark-all "0.62.2"]]
  :java-source-paths ["./java"]
  :repositories {"releases" {:url "https://clojars.org/repo"
                             :username :env
                             :password :env
                             :sign-releases false}}
  :main ^:skip-aot staxchg.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
