(ns benchmark.async
  (:import  [clojure.lang IPersistentMap Named Keyword Ratio]
            [java.util List Map Date Set]
            [com.mongodb ConnectionString
             ServerAddress
             Block ReadPreference]
            [com.mongodb.async.client MongoClient
             MongoClients MongoClientSettings Observables
             MongoCollection MongoDatabase]
            [com.mongodb.connection ClusterSettings ConnectionPoolSettings]
            [com.mongodb.connection.netty NettyStreamFactoryFactory]
            [com.mongodb.async SingleResultCallback]
            [org.bson Document]
            [org.bson.types ObjectId]
            [java.util.concurrent CyclicBarrier CountDownLatch]))

(def mongo-client (MongoClients/create
                   (-> (MongoClientSettings/builder)
                       (.clusterSettings (-> (ClusterSettings/builder)
                                             (.hosts [(ServerAddress. "192.168.33.10" 27017)])
                                             (.build)))
                       (.connectionPoolSettings (-> (ConnectionPoolSettings/builder)
                                                    (.maxSize 100)
                                                    (.minSize 50)
                                                    (.maxWaitQueueSize 10000)
                                                    (.build)))
                       (.build))))

(def all-run-times 10000)
(def threads 20)
(def run-times (/ all-run-times threads))

(def test-db "test-db")
(def test-class "test-class")

(def default-cb
  (reify
    SingleResultCallback
    (onResult [this result t]
      (if (or result t)
        (println "error?" result t)))))

(defn drop-all
  []
  (let [db-collection (-> (.getDatabase mongo-client test-db)
                          (.getCollection test-class))]
    (.drop db-collection default-cb)))

(defn async-query
  []
  (let [x (promise)
        y (atom 0)
        default-blockcb (reify
                          Block
                          (apply [this s]
                            (when (= (swap! y inc) run-times)
                              (deliver x true))))
        db-client (.getDatabase mongo-client test-db)
        query (Document. {"name" "_User"})
        start (System/currentTimeMillis)]
    (dotimes [_ run-times]
      (-> db-client
          (.getCollection test-class)
          (.find query)
          (.forEach default-blockcb default-cb)))
    @x
    (println  (format "async query %s times use %s ms %s" run-times (- (System/currentTimeMillis) start) @y))))



(defn async-insert
  [id cdl]
  (let [x (promise)
        y (atom 0)
        thread-ids (atom #{})
        cb (reify
             SingleResultCallback
             (onResult [this result t]
               (swap! thread-ids conj (.getId (Thread/currentThread)))
               (when t
                 (println "error:" t))
               (when (= (swap! y inc) run-times)
                 (deliver x true))))
        db-client (.getDatabase mongo-client test-db)
        start (System/currentTimeMillis)]
    (dotimes [i run-times]
      (-> db-client
          (.getCollection test-class)
          (.insertOne (Document. {"name" (str (rand-int 10000))
                                  "_id" (str (+ i id))}) cb)))
    @x
    (.countDown cdl)
    (println  (format "id: %s async insert %s times use %s ms" id run-times (- (System/currentTimeMillis) start)))))

(defn -main
  []
  (let [cdl (CountDownLatch. threads)
        barrier (CyclicBarrier. threads)]
    (dotimes [i threads]
      (.start
       (Thread. (fn []
                  (.await barrier)
                  (async-insert (* i run-times) cdl)))))
    (.await cdl)
    (drop-all)
    (println "finsh.")))

(defn async-handler [req respond raise]
  (let [cb (reify
             SingleResultCallback
             (onResult [this result t]
               (if t
                 (respond {:status 400 :body "error"})
                 (respond {:status 200 :body "success"}))))]
    (-> (.getDatabase mongo-client test-db)
        (.getCollection test-class)
        (.insertOne (Document. {"name" (str (rand-int 10000))
                                "_id" (str (System/nanoTime) (rand-int 1000))}) cb))))
