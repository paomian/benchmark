(ns benchmark.jetty
  (:require [ring.adapter.jetty :as jetty]
            [benchmark.async :as async]
            [benchmark.sync :as sync]))

(defn go-async []
  (jetty/run-jetty async/async-handler {:async? true :port 9000}))

(defn go-sync []
  (jetty/run-jetty sync/sync-handler {:port 9000}))

