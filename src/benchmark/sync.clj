 (ns benchmark.sync
  (:refer-clojure :exclude [sync])
  (:import
   ;;[com.mongodb.client MongoClients]
   ;;[com.mongodb.client MongoClient]
   [com.mongodb.client MongoCollection]
   [com.mongodb.client MongoDatabase]
   [com.mongodb MongoClientOptions MongoClient ServerAddress Block]
   [org.bson Document]
   ;;[com.mongodb Block ConnectionString MongoClientSettings ServerAddress]
   ;;[com.mongodb.connection ClusterSettings ConnectionPoolSettings]
   [java.util.concurrent CyclicBarrier CountDownLatch]
   ))


#_(def mongo-client (MongoClients/create
                   (-> (MongoClientSettings/builder)
                       (.applyToClusterSettings
                        (reify
                          Block
                          (apply [this x]
                            (-> x
                                (.hosts [(ServerAddress. "192.168.33.10" 27017)])))))
                       (.applyToConnectionPoolSettings
                        (reify
                          Block
                          (apply [this x]
                            (-> x

                                (.maxSize 100)
                                (.minSize 50)
                                (.maxWaitQueueSize 10000)))))
                       (.build))))

(def mongo-client (MongoClient. [(ServerAddress. "192.168.33.10" 27017)]))
(def all-run-times 10000)
(def threads 20)
(def run-times (/ all-run-times threads))
(def test-db "test-db")
(def test-class "test-class")

(def default-blockcb (reify
                       Block
                       (apply [this s])))

(defn drop-all
  []
  (let [db-collection (-> (.getDatabase mongo-client test-db)
                          (.getCollection test-class))]
    (println "start drop data.")
    (.drop db-collection)))

(defn sync-query
  []
  (let [db-client (.getDatabase mongo-client test-db)
        query (Document. {"name" "_User"})
        start (System/currentTimeMillis)]
    (dotimes [_ run-times]
      (-> db-client
          (.getCollection "_Class")
          (.find query)
          (.forEach default-blockcb)))
    (println  (format "sync query %s times user %s ms" run-times (- (System/currentTimeMillis) start)))))

(defn sync-insert
  [id cdl]
  (let [db-client (.getDatabase mongo-client test-db)
        start (System/currentTimeMillis)]
    (dotimes [i run-times]
      (-> db-client
          (.getCollection test-class)
          (.insertOne (Document. {"name" (str (rand-int 10000))
                                  "_id" (str (+ i id))}))))
    (.countDown cdl)
    (println  (format "id: %s sync insert %s times user %s ms" id run-times (- (System/currentTimeMillis) start)))))

(defn sync
  []
  (let [barrier (CyclicBarrier. threads)
        cdl (CountDownLatch. threads)]
    (dotimes [i threads]
      (future (do
                (.await barrier)
                (sync-insert (* i run-times) cdl))))
    (.await cdl)
    (drop-all)
    (println "finsh.")))

(defn sync-handler [req]
  (try
    (-> (.getDatabase mongo-client test-db)
        (.getCollection test-class)
        (.insertOne (Document. {"name" (str (rand-int 10000))
                                "_id" (str (System/nanoTime) (rand-int 1000))})))
    {:status 200 :body "success"}
    (catch Exception e
      {:status 400 :body "error"})))
