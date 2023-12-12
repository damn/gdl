(ns gdl.graphics.color
  (:import com.badlogic.gdx.graphics.Color))

(defn rgb
  ([r g b]
   (rgb r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defmacro defrgb [symbol & rgb-args]
  `(def ~symbol (rgb ~@rgb-args)))
