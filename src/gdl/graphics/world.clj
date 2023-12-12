(ns gdl.graphics.world
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.graphics.viewport :as viewport])
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           [com.badlogic.gdx.utils.viewport Viewport FitViewport]
           com.badlogic.gdx.math.Vector3))

(declare unit-scale)

(defn pixels->world-units [px]
  (* px unit-scale))

(declare ^OrthographicCamera camera
         ^Viewport viewport)

(defmodule {:keys [world-unit-scale world-camera world-viewport]}
  (lc/create [_ _ctx]
    (.bindRoot #'unit-scale world-unit-scale)
    (.bindRoot #'camera world-camera)
    (.bindRoot #'viewport world-viewport)))

; TODO clamping only works for gui-viewport ? check. comment if true
(defn mouse-position
  "Can be negative coordinates, undefined cells."
  []
  (viewport/unproject-mouse-posi viewport))

(defn viewport-width  [] (.getWorldWidth  viewport))
(defn viewport-height [] (.getWorldHeight viewport))

(defn camera-position []
  [(.x (.position camera))
   (.y (.position camera))])

(defn set-camera-position! [[x y]]
  (set! (.x (.position camera)) (float x))
  (set! (.y (.position camera)) (float y)))

(defn camera-frustum []
  (let [frustum-points (for [^Vector3 point (take 4 (.planePoints (.frustum camera)))
                             :let [x (.x point)
                                   y (.y point)]]
                         [x y])
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y])) ; TODO need only x,y , w/h is viewport-width/height ?

(defn visible-tiles []
  (let [[left-x right-x bottom-y top-y] (camera-frustum)]
    (for  [x (range (int left-x)   (int right-x))
           y (range (int bottom-y) (+ 2 (int top-y)))]
      [x y])))
