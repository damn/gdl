(ns gdl.backends.libgdx.context.graphics
  (:require gdl.context
            [gdl.graphics.color :as color]
            [gdl.backends.libgdx.utils.reflect :refer [bind-roots]])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.Color))

(extend-type gdl.context.Context
  gdl.context/Graphics
  (frames-per-second [_]
    (.getFramesPerSecond Gdx/graphics)))

(bind-roots "com.badlogic.gdx.graphics.Color"
            'com.badlogic.gdx.graphics.Color
            "gdl.graphics.color")
