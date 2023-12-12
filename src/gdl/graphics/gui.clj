(ns gdl.graphics.gui
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.graphics.viewport :as viewport])
  (:import com.badlogic.gdx.utils.viewport.Viewport))

(declare ^Viewport viewport)

(defmodule {:keys [gui-viewport]}
  (lc/create [_ _ctx]
    (.bindRoot #'viewport gui-viewport)))

(defn mouse-position []
  (mapv int (viewport/unproject-mouse-posi viewport)))

(defn viewport-width  [] (.getWorldWidth  viewport))
(defn viewport-height [] (.getWorldHeight viewport))
