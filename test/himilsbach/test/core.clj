(set! *warn-on-reflection* true)

(ns himilsbach.test.core
  (require [himilsbach.core :as him])
  (use clojure.test)
  (import java.util.concurrent.Semaphore))

(defn- rel
  [^Semaphore sem]
  (.release sem))

(defn- acq
  ([^Semaphore sem] (.acquire sem))
  ([^Semaphore sem n] (.acquire sem n)))

(defn- semaphore
  []
  (Semaphore. 0))

(defmacro ^:private do-and-die
  [& body]
  `(do
     ~@body
     ~'(die)))

(deftest send-and-start
  (let [sem (semaphore)
        success (atom false)
        a (him/new [_] (do-and-die
                         (reset! success true)
                         (rel sem)))]
    (him/send! a '())
    (him/start a)
    (acq sem)
    (is @success)))

(deftest start-and-send
  (let [sem (semaphore)
        success (atom false)
        a (him/new [_] (do-and-die
                         (reset! success true)
                         (rel sem)))]
    (him/start a)
    (him/send! a '())
    (acq sem)
    (is @success)))

(deftest no-match
  (let [sem (semaphore)
        success (atom true)
        a (him/new [x y] (do-and-die
                           (reset! success true)
                           (rel sem))
                   [x]   (do-and-die
                           (reset! success true)
                           (rel sem))
                    _    (do-and-die
                           (reset! success false)
                           (rel sem)))]
    (him/start a)
    (him/send! a :qwer :asdf :zxcv)
    (acq sem)
    (is (not @success))))

(deftest successful-match
  (let [sem (semaphore)
        success (atom false)
        a (him/new [x]   (do-and-die
                           (reset! success false)
                           (rel sem))
                   [x y] (do-and-die
                           (reset! success true)
                           (rel sem)))]
    (him/start a)
    (him/send! a :foo :bar)
    (acq sem)
    (is @success)))

(deftest send-to-self
  (let [sem (semaphore)
        state (atom 4)
        a (him/new [_] (case (int @state)
                           4 (do (swap! state inc)
                                 (him/send! self '()))
                           5 (do-and-die
                               (swap! state inc)
                               (rel sem))
                             (do (throw "This didn't work")
                                 (rel sem))))]
    (him/start a)
    (him/send! a '())
    (acq sem)
    (is (= 6 @state))))

(deftest dying
  (let [sem (semaphore)
        state (atom 4)
        a (him/new [_] (do
                           (swap! state inc)
                           (him/send! self '())
                           (when (= 6 @state)
                             (rel sem)
                             (die))))]
    (him/start a)
    (him/send! a '())
    (acq sem)
    (Thread/sleep 5)
    (is (= 6 @state))))

(deftest dying-with-notified-actor
  (let [sem (semaphore)
        state (atom 4)
        received-notification (atom false)
        a (him/new [msg] (do
                           (when-not (= :all-went-fine-please-die msg)
                             (reset! received-notification true))
                           (die)))
        b (him/new a
            [_] (do
                  (swap! state inc)
                  (him/send! self '())
                  (when (= 6 @state)
                    (rel sem)
                    (die))))]
    (him/start b)
    (him/start a)
    (him/send! b '())
    (acq sem)
    (Thread/sleep 5)
    (is (= 6 @state))
    (is (not @received-notification))
    (him/send! a :all-went-fine-please-die)))

(deftest two-actor-ping-pong
  (let [sem (semaphore)
        state (atom 0)
        a (him/new [:b from] (do
                                 (swap! state #(+ 2 %))
                                 (him/send! from :c self))
                     [:d from] (do-and-die
                                 (swap! state #(+ 8 %))
                                 (rel sem)))
        b (him/new [:a from] (do
                                 (swap! state #(+ 1 %))
                                 (him/send! from :b self))
                     [:c from] (do-and-die
                                 (swap! state #(+ 4 %))
                                 (him/send! from :d self)))]
    (doseq [actor [a b]]
      (him/start actor))
    (him/send! b :a a)
    (acq sem)
    (is (= 15 @state))))

(deftest full-graph
  (let [n 32
        sem (semaphore)
        state (atom 0)
        actor (fn [] (him/new
                       [:start actors] (doseq [actor actors]
                                         (him/send! actor :ping self))
                       [:ping from] (him/send! from :pong)
                       [:pong] (when (= (* n n) (swap! state inc))
                                 (rel sem))
                       [:die] (die)))
        actors (take n (repeatedly actor))]
    (doseq [actor actors]
      (him/start actor))
    (doseq [actor actors]
      (him/send! actor :start actors))
    (acq sem)
    (doseq [a actors]
      (him/send! a :die))
    (is (= (* n n) @state))))

(deftest unique-id
  (let [sem (semaphore)
        ids (atom {})
        errors (atom [])
        a (him/new
            [:send other] (him/send! other :a self)
            [name other] (let [id (him/id other)]
                           (if (and (@ids id) (= (@ids id) name))
                             (rel sem)
                             (swap! ids #(conj % [id name]))))
            [:die] (die))
        b (him/new
            [:send other] (him/send! other :b self)
            [:die] (die))]
    (doseq [actor [a b]]
      (him/start actor))
    (him/send! b :send a)
    (him/send! a :send a)
    (him/send! b :send a)
    (him/send! a :send a)
    (acq sem 2)
    (doseq [actor [a b]]
      (him/send! actor :die))
    (doseq [fn [keys vals]]
      (is (= 2 (count (distinct (fn @ids))))))))

(deftest error-notifications-error
  (let [sem (semaphore)
        throwable (Error.)
        state (atom nil)
        a (him/new
            [:error who ex] (do-and-die
                              (swap! state (fn [_] [(him/id who) ex]))
                              (rel sem)))
        b (him/new a
            [:throw] (throw throwable))]
    (doseq [actor [a b]]
      (him/start actor))
    (him/send! b :throw)
    (acq sem)
    (is (= [(him/id b) throwable] @state))))

; Kopipasta from above.
(deftest error-notifications-exception
  (let [sem (semaphore)
        throwable (Exception.)
        state (atom nil)
        a (him/new
            [:error who ex] (do-and-die
                              (swap! state (fn [_] [(him/id who) ex]))
                              (rel sem)))
        b (him/new a
            [:throw] (throw throwable))]
    (doseq [actor [a b]]
      (him/start actor))
    (him/send! b :throw)
    (acq sem)
    (is (= [(him/id b) throwable] @state))))

(deftest stopping
  (let [sem1 (semaphore)
        sem2 (semaphore)
        success (atom false)
        watch (him/new
                [:error _ ^Exception ex]
                  (when (= (.getMessage ex) "Stopped!")
                    (reset! success true)
                    (rel sem2)
                    (die))
                _ (rel sem2))
        a (him/new watch [_] (do
                                 (Thread/sleep 1)
                                 (rel sem1)
                                 (him/send! self ())))]
    (him/start a)
    (him/start watch)
    (him/send! a '())
    (acq sem1)
    (him/stop a)
    (acq sem2)
    (is @success)))

(deftest any-matching?
  (let [a (him/new _ ())]
    (him/send! a :asdf 5)
    (is (him/any-matching? a [:asdf _]))
    (is (him/any-matching? a [:asdf 5]))
    (is (him/any-matching? a _))
    (is (not (him/any-matching? a [])))
    (is (not (him/any-matching? a [:asdf])))
    (is (not (him/any-matching? a [:asdf 5 _])))))

(defn -main
  [& _]
  (run-tests 'himilsbach.test.core)
  (prn (Thread/activeCount) 'active 'threads)
  (System/exit 0))
