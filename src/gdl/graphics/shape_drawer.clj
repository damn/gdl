(ns gdl.graphics.shape-drawer
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc])
  (:import [com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format Color]
           com.badlogic.gdx.graphics.g2d.TextureRegion
           space.earlygrey.shapedrawer.ShapeDrawer))

(defn- gen-drawer-texture ^Texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(declare ^ShapeDrawer drawer)

(defmodule texture
  (lc/create [[_ batch] _ctx]
    (let [texture (gen-drawer-texture)]
      (.bindRoot #'drawer (ShapeDrawer. batch (TextureRegion. texture 0 0 1 1)))
      texture))
  (lc/dispose [_]
    (.dispose ^Texture texture)))

(defn- set-color [^Color color]
  (.setColor drawer color))

(defn set-line-width [width]
  (.setDefaultLineWidth drawer (float width)))

(defmacro with-line-width [width & exprs]
  `(let [old-line-width# (.getDefaultLineWidth drawer)]
     (set-line-width (* ~width old-line-width#))
     ~@exprs
     (set-line-width old-line-width#)))

(defn ellipse [[x y] radius-x radius-y color]
  (set-color color)
  (.ellipse drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (set-color color)
  (.filledEllipse drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn circle [[x y] radius color]
  (set-color color)
  (.circle drawer (float x) (float y) (float radius)))

(defn filled-circle [[x y] radius color]
  (set-color color)
  (.filledCircle drawer (float x) (float y) (float radius)))

(defn- degree->radians [degree] ; TODO not here
  (* degree (/ (Math/PI) 180)))

(defn arc [[centre-x centre-y] radius start-angle degree color]
  (set-color color)
  (.arc drawer centre-x centre-y radius start-angle (degree->radians degree)))

(defn sector [[centre-x centre-y] radius start-angle degree color]
  (set-color color)
  (.sector drawer centre-x centre-y radius start-angle (degree->radians degree)))

(defn rectangle [x y w h color]
  (set-color color)
  (.rectangle drawer x y w h))

(defn filled-rectangle [x y w h color]
  (set-color color)
  (.filledRectangle drawer (float x) (float y) (float w) (float h)))

(defn line
  ([[x y] [ex ey] color]
   (line x y ex ey color))
  ([x y ex ey color]
   (set-color color)
   (.line drawer (float x) (float y) (float ex) (float ey))))

(defn grid
  [leftx bottomy gridw gridh cellw cellh color]
  (let [w (* gridw cellw)
        h (* gridh cellh)
        topy (+ bottomy h)
        rightx (+ leftx w)]
    (doseq [idx (range (inc gridw))
            :let [linex (+ leftx (* idx cellw))]]
           (line linex topy linex bottomy color))
    (doseq [idx (range (inc gridh))
            :let [liney (+ bottomy (* idx cellh))]]
           (line leftx liney rightx liney color))))
