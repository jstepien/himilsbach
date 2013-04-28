(ns himilsbach.core
  (use [clojure.core.match :only [match]])
  (import java.util.concurrent.Semaphore))

(def ^{:doc "Internal use only. Please don't touch."}
  -kill-order (Object.))

(defmacro new
  "Creates a new actor. It has to be explicitly started with the `start'
  function. Takes a set of pattern/callback pairs. If a message sent to the
  actor matches a given pattern the respective callback is evaluated with
  binding specified in the pattern.

  In scopes of all callbacks following special vars are defined.

    - `die' is a function which causes the actor to be terminated if it's the
      last expression in the callback.
    - `self' is the current actor.

  Pattern/callback pairs can be preceded with a single argument specifying an
  actor to be notified when this actor throws an exception. The notified actor
  will receive a message [:error other ex], where `other' is the actor which
  thrown exception `ex'.

  See `clojure.core.match/match' for details regarding pattern matching."
  [& body]
  (let [inbox (gensym)
        sem (gensym)
        msg (gensym)]
    `(let [~inbox (ref [])
           ~sem (Semaphore. 0)
           fun# (fn [~msg]
                  (let [~'self [~inbox ~sem]
                        ~'die (fn [] -kill-order)]
                    ~(if (odd? (count body))
                       `(try
                          (match
                            ~msg
                            ~@(rest body))
                         (catch Throwable ex#
                           (send! ~(first body) :error ~'self ex#)
                           (~'die)))
                       `(match
                          ~msg
                          ~@body))))]
       [~inbox ~sem fun#])))

(defn send!
  "Sends a message to a given actor."
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
  "Starts the actor created with `new' in a new future.
  Immediately returns nil."
  [actor]
  (future
    (let [[_ _ f] actor]
      (loop []
        (when-not (= -kill-order (f (inbox-pop actor)))
          (recur)))))
  nil)

(let [ids (ref {})]
  (defn id
    "Returns the unique identifier of a given actor."
    [[inbox & _]]
    (dosync
      (if (@ids inbox)
        (@ids inbox)
        (let [sym (gensym 'actor_)]
          (ref-set ids (conj @ids [inbox sym]))
          sym)))))

(defmacro any-matching?
  "Returns true if any message in the inbox of the given actor matches the given
  pattern."
  [actor pattern]
  `(let [[inbox# & ~'_] ~actor]
     (some #(match % ~pattern true) @inbox#)))
