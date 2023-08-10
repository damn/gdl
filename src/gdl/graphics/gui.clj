(ns gdl.graphics.gui
  (:require [x.x :refer [defcomponent]]
            [gdl.lc :as lc]
            [gdl.graphics :as g]
            [gdl.graphics.viewport :as viewport]
            gdl.render)
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           [com.badlogic.gdx.utils.viewport Viewport FitViewport]))

(def unit-scale 1)

(declare ^OrthographicCamera camera
         ^Viewport viewport)

(defcomponent *ns* _
  (lc/create [_]
    (.bindRoot #'camera   (OrthographicCamera.))
    (.bindRoot #'viewport (FitViewport. (g/screen-width)
                                        (g/screen-height)
                                        camera))))

(defn mouse-position []
  (mapv int (viewport/unproject-mouse-posi viewport)))

(defn viewport-width  [] (.getWorldWidth  viewport))
(defn viewport-height [] (.getWorldHeight viewport))

(defn render [renderfn]
  (gdl.render/render-with unit-scale camera renderfn))