(ns gdl.files
  (:import (com.badlogic.gdx Gdx)))

(defn internal [path]
  (.internal Gdx/files path))
