(ns himilsbach.test.benchmark
  (require [himilsbach.core :as him]))

(defn- actors-in-a-ring
  [sem n]
  (let [state (atom 0)
        actor (fn [] (him/new
                       [:start actors] (doall
                                         (map
                                           #(him/send! % :ping self)
                                           actors))
                       [:ping from] (him/send! from :pong)
                       [:pong] (when (= (* n n) (swap! state inc))
                                 (.release sem))
                       [:die] (die)))
        actors (take n (repeatedly actor))]
    actors))

(defn run
  [n]
  (let [sem (java.util.concurrent.Semaphore. 0)
        actors (actors-in-a-ring sem n)]
    (doall (map him/start actors))
    (doall (map #(him/send! % :start actors) actors))
    (.acquire sem)
    (doall (map #(him/send! % :die) actors))))

(defn -main
  ([] (-main "100"))
  ([times & _]
   (dotimes [_ (Integer/parseInt times)]
     (time (run (Integer/parseInt times))))
   (System/exit 0)))
