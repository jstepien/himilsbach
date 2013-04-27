(ns himilsbach.core
  (use [clojure.core.match :only [match]])
  (import java.util.concurrent.Semaphore))

(def kill-order (Object.))

(defmacro new
  [& body]
  (let [inbox (gensym)
        sem (gensym)
        msg (gensym)
        stopped (gensym)]
    `(let [~inbox (ref [])
           ~sem (Semaphore. 0)
           ~stopped (atom false)
           fun# (fn [~msg]
                  (let [~'self [~inbox ~sem]
                        ~'die (fn [] kill-order)]
                    ~(if (odd? (count body))
                       `(try
                         (if @~stopped
                           (throw (Exception. "Stopped!"))
                           (match
                             ~msg
                             ~@(rest body)))
                         (catch Throwable ex#
                           (send! ~(first body) :error ~'self ex#)
                           (~'die)))
                       `(if @~stopped
                          (throw Exception "Stopped!")
                          (match
                            ~msg
                            ~@body)))))]
       [~inbox ~sem ~stopped fun#])))

(defn send!
  [actor & msg]
  (let [[inbox ^Semaphore sem & _] actor]
    (dosync
      (ref-set inbox (conj @inbox (vec msg))))
    (.release sem)))

(defn- inbox-pop
  [[inbox ^Semaphore sem _]]
  (.acquire sem)
  (dosync
    (let [[head & tail] @inbox]
      (ref-set inbox tail)
      head)))

(defn start
  [actor]
  (future
    (let [[_ _ _ f] actor]
      (loop []
        (when-not (= kill-order (f (inbox-pop actor)))
          (recur)))))
  nil)

(let [ids (ref {})]
  (defn id
    [[inbox & _]]
    (dosync
      (if (@ids inbox)
        (@ids inbox)
        (let [sym (gensym 'actor_)]
          (ref-set ids (conj @ids [inbox sym]))
          sym)))))

(defn stop
  [[_ _ stopped & _]]
    (reset! stopped true))

(defmacro any-matching?
  [actor pattern]
  `(let [[inbox# & ~'_] ~actor]
     (some #(match % ~pattern true) @inbox#)))
