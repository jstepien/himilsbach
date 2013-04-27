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
        actors (actors-in-a-ring sem n)
        domap (comp doall map)]
    (domap him/start actors)
    (domap #(him/send! % :start actors) actors)
    (.acquire sem)
    (domap #(him/send! % :die) actors)))

(defn -main
  ([] (-main "100"))
  ([times & _]
   (let [n (Integer/parseInt times)]
     (time
       (dotimes [_ n]
         (time (run n)))))
   (System/exit 0)))
