(defproject staxchg "0.1.1"
  :description "A Stack Exchange client for the terminal"
  :url "https://github.com/eureton/staxchg"
  :license {:name "MIT"
            :url "https://github.com/eureton/staxchg/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/test.check "0.10.0"]
                 [org.martinklepsch/clj-http-lite "0.4.3"]
                 [cheshire "5.10.0"]
                 [com.googlecode.lanterna/lanterna "3.0.4"]
                 [org.jsoup/jsoup "1.11.2"]
                 [org.clojars.eureton/cljmd "0.1.1"]
                 [smachine "0.1.0"]
                 [org.clojars.eureton/squirrel "0.1.0"]
                 [cookbook "0.2.0"]
                 [org.flatland/useful "0.11.6"]]
  :java-source-paths ["./java"]
  :repositories {"releases" {:url "https://clojars.org/repo"
                             :username :env
                             :password :env
                             :sign-releases false}}
  :main ^:skip-aot staxchg.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Djava.awt.headless=true"
                                  "-Dclojure.compiler.direct-linking=true"]
                       :native-image {:jvm-opts ["-Djava.awt.headless=true"
                                                 "-Dclojure.compiler.direct-linking=true"]}}}
  :jvm-opts ["-Djava.awt.headless=true"]
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :native-image {:name "staxchg"
                 :opts ["--initialize-at-build-time"
                        "--report-unsupported-elements-at-runtime"
                        "--no-server"
                        "--no-fallback"
                        "--enable-https"
                        "--enable-url-protocols=https"
                        "--initialize-at-run-time=sun.java2d.xr.XRBackendNative,sun.font.StrikeCache"
                        "-H:+ReportExceptionStackTraces"
                        "-H:ReflectionConfigurationFiles=native-image/config/reflection-config.json"
                        "-H:ResourceConfigurationFiles=native-image/config/resource-config.json"]})

