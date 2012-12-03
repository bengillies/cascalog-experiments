(defproject foosite "0.1.0-SNAPSHOT"
            :description "A quick Noir project to use Cascalog"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3"]
                           [cascalog "1.10.0"]]
            :main foosite.server
            :jvm-opts ["-Xmx1500m"]
            :profiles {:dev {:dependencies
                             [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]}})
