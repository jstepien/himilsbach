(ns himilsbach.test.benchmark
  (require [himilsbach.core :as him]))

(defn- actors-in-a-ring
  [sem n]
  (let [state (atom 0)
        actor (fn [] (him/new
                       [:start actors] (doseq [actor actors]
                                         (him/send! actor :ping self))
                       [:ping from] (him/send! from :pong)
                       [:pong] (when (= (* n n) (swap! state inc))
                                 (.release sem))
                       [:die] (die)))
        actors (take n (repeatedly actor))]
    actors))

(defn run
  [n]
  (let [sem (java.util.concurrent.Semaphore. 0)
        all (actors-in-a-ring sem n)]
    (doseq [actor all]
      (him/start actor)
      (him/send! actor :start all))
    (.acquire sem)
    (doseq [actor all]
      (him/send! actor :die))))

(defn -main
  ([] (-main "100"))
  ([times & _]
   (let [n (Integer/parseInt times)]
     (time
       (dotimes [_ n]
         (time (run n)))))
   (System/exit 0)))
