(ns gdl.utils
  (:import com.badlogic.gdx.utils.Disposable))

(defn dispose [^Disposable obj]
  (.dispose obj))
