(ns gdx.utils
  (:import com.badlogic.gdx.utils.Disposable))

(defn set-var-root [v value]
  (alter-var-root v (constantly value)))

(def events->listeners {})

(defn add-listener [event listener]
  (alter-var-root #'events->listeners update event
                  #(conj (or % []) listener))
  nil)

(defmacro on [event & exprs]
  `(add-listener ~event (fn [] ~@exprs)))

(defn fire-event! [event]
  (doseq [listener (get events->listeners event)]
    (listener)))

(defn dispose [^Disposable obj]
  (.dispose obj))
