(ns gdl.graphics
  (:import (com.badlogic.gdx Gdx Graphics)))

(defn graphics ^Graphics []
  Gdx/graphics)

(defn screen-width  [] (.getWidth           (graphics)))
(defn screen-height [] (.getHeight          (graphics)))
(defn fps           [] (.getFramesPerSecond (graphics)))
(defn delta-time    [] (.getDeltaTime       (graphics)))
