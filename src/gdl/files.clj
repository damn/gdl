(ns gdl.files
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

(defn internal ^FileHandle [path]
  (.internal Gdx/files path))
