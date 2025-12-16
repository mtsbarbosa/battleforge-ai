(defproject battleforge-ai "0.1.0-SNAPSHOT"
  :description "A command-line tool for simulating hundreds of Keyforge deck battles and generating statistical analysis"
  :url "https://github.com/your-username/battleforge-ai"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [org.clojure/data.json "2.4.0"]
                 [prismatic/schema "1.4.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]
                 [clojure.java-time "1.2.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.8"]]
  
  :main ^:skip-aot battleforge-ai.diplomat.cli
  :target-path "target/%s"
  
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[org.clojure/test.check "1.1.1"]]
                   :plugins [[com.github.clj-kondo/lein-clj-kondo "2024.03.13"]
                             [jonase/eastwood "1.4.2"]
                             [lein-zprint "1.2.9"]]}}
  
  :jvm-opts ["-Xmx2g"]

  :zprint {:old? false}

  :aliases {"battle" ["run" "-m" "battleforge-ai.diplomat.cli"]
            "simulate" ["run" "-m" "battleforge-ai.diplomat.cli"]
            "stats" ["run" "-m" "battleforge-ai.diplomat.cli"]
            "fetch-deck" ["run" "fetch-deck"]
            "lint" ["with-profile" "dev" "do" ["clj-kondo"] ["eastwood"]]
            "lint:kondo" ["with-profile" "dev" "clj-kondo"]
            "lint:eastwood" ["with-profile" "dev" "eastwood"]
}) 