(ns gdl.graphics.batch
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc])
  (:import (com.badlogic.gdx.graphics.g2d Batch SpriteBatch)))

(declare ^Batch batch)

(defmodule user-batch
  (lc/create [_]
    (.bindRoot #'batch user-batch))
  (lc/dispose [_]
    (.dispose batch)))

(defn sprite-batch []
  (SpriteBatch.))
