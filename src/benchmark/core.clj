(ns benchmark.core
  (:require [benchmark.async :as ba]
            [benchmark.sync :as bs]
            [benchmark.jetty :as jetty]))

(defn -main
  [& params]
  (if (= (first params) "async")
    (jetty/go-async)
    (jetty/go-sync)))
