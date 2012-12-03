(defproject pipeline "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cascalog "1.10.0"]
                 [clojure-csv/clojure-csv "2.0.0-alpha2"]]
  :main pipeline.core
  :jvm-opts ["-Xmx1500m"]
  :profiles {:dev
             {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]}})
