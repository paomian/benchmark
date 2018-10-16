(defproject benchmark "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.mongodb/mongodb-driver-async "3.3.0"]
                 [org.mongodb/mongodb-driver "3.3.0"]
                 [io.netty/netty-all "4.1.29.Final"]
                 [ring/ring-core "1.7.0"]
                 [ring/ring-jetty-adapter "1.7.0"]]
  :jvm-opts ["-server"]
  :main benchmark.core)
