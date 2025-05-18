(ns gdl.graphics
  (:require [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.viewport :as viewport]
            [gdl.interop :as interop]
            [clojure.string :as str])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math MathUtils
                                  Vector2)
           (com.badlogic.gdx.utils Disposable
                                   ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(def white Color/WHITE)
(def black Color/BLACK)

(defn def-markdown-color [name color]
  (Colors/put name color))

(defn texture-region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h))))

(defn sub-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region
                  (int x)
                  (int y)
                  (int w)
                  (int h)) )

(defn dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn sprite-batch []
  (let [this (SpriteBatch.)]
    (reify
      batch/Batch
      (draw-on-viewport! [_ viewport draw-fn]
        (.setColor this Color/WHITE) ; fix scene2d.ui.tooltip flickering
        (.setProjectionMatrix this (camera/combined (:camera viewport)))
        (.begin this)
        (draw-fn)
        (.end this))

      (draw-texture-region! [_ texture-region [x y] [w h] rotation color]
        (if color (.setColor this color))
        (.draw this
               texture-region
               x
               y
               (/ (float w) 2) ; rotation origin
               (/ (float h) 2)
               w
               h
               1 ; scale-x
               1 ; scale-y
               rotation)
        (if color (.setColor this Color/WHITE)))

      Disposable
      (dispose [_]
        (.dispose this))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this)))))

(defn color [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))

(defn set-cursor! [cursor]
  (.setCursor Gdx/graphics cursor))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn cursor [path hotspot-x hotspot-y]
  (let [pixmap (Pixmap. (.internal Gdx/files path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn truetype-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units not visible
    font))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw-text! [^BitmapFont font batch {:keys [scale x y text h-align up?]}]
  (let [data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale (float scale)))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           (:java-object batch)
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))

(defn shape-drawer [batch texture-region]
  (let [this (ShapeDrawer. (:java-object batch) texture-region)]
    (reify
      sd/ShapeDrawer
      (set-color! [_ color]
        (.setColor this (interop/->color color)))

      (ellipse! [_ x y radius-x radius-y]
        (.ellipse this
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

      (filled-ellipse! [_ x y radius-x radius-y]
        (.filledEllipse this
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

      (circle! [_ x y radius]
        (.circle this
                 (float x)
                 (float y)
                 (float radius)))

      (filled-circle! [_ x y radius]
        (.filledCircle this
                       (float x)
                       (float y)
                       (float radius)))

      (arc! [_ center-x center-y radius start-angle degree]
        (.arc this
              (float center-x)
              (float center-y)
              (float radius)
              (float (degree->radians start-angle))
              (float (degree->radians degree))))

      (sector! [_ center-x center-y radius start-angle degree]
        (.sector this
                 (float center-x)
                 (float center-y)
                 (float radius)
                 (float (degree->radians start-angle))
                 (float (degree->radians degree))))

      (rectangle! [_ x y w h]
        (.rectangle this
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

      (filled-rectangle! [_ x y w h]
        (.filledRectangle this
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

      (line! [_ sx sy ex ey]
        (.line this
               (float sx)
               (float sy)
               (float ex)
               (float ey)))

      (with-line-width [_ width draw-fn]
        (let [old-line-width (.getDefaultLineWidth this)]
          (.setDefaultLineWidth this (float (* width old-line-width)))
          (draw-fn)
          (.setDefaultLineWidth this (float old-line-width)))))))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      viewport/Viewport
      (update! [_]
        (.update this
                 (.getWidth  Gdx/graphics)
                 (.getHeight Gdx/graphics)
                 center-camera?))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (mouse-position [_]
        (let [mouse-x (clamp (.getX Gdx/input)
                             (.getLeftGutterWidth this)
                             (.getRightGutterX    this))
              mouse-y (clamp (.getY Gdx/input)
                             (.getTopGutterHeight this)
                             (.getTopGutterY      this))]
          (let [v2 (.unproject this (Vector2. mouse-x mouse-y))]
            [(.x v2) (.y v2)])))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera      this))))))

(defn ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width
                  world-height
                  camera
                  {:center-camera? false})))
