(ns ^:no-doc gdl.backends.libgdx.context.shape-drawer
  (:require gdl.context
            [gdl.graphics.color :as color])
  (:import com.badlogic.gdx.math.MathUtils
           (com.badlogic.gdx.graphics Color Texture Pixmap Pixmap$Format)
           com.badlogic.gdx.graphics.g2d.TextureRegion
           space.earlygrey.shapedrawer.ShapeDrawer))

(defn- degree->radians [degree]
  (* degree MathUtils/degreesToRadians))

(defn- ->Color
  ([r g b]
   (->Color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn ->color ^Color [color]
  (if (= Color (class color))
    color
    (apply ->Color color)))

(defn- set-color [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (->color color)))

(extend-type gdl.context.Context
  gdl.context/ShapeDrawer
  (draw-ellipse [{:keys [^ShapeDrawer shape-drawer]} [x y] radius-x radius-y color]
    (set-color shape-drawer color)
    (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)) )

  (draw-filled-ellipse [{:keys [^ShapeDrawer shape-drawer]} [x y] radius-x radius-y color]
    (set-color shape-drawer color)
    (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

  (draw-circle [{:keys [^ShapeDrawer shape-drawer]} [x y] radius color]
    (set-color shape-drawer color)
    (.circle shape-drawer (float x) (float y) (float radius)))

  (draw-filled-circle [{:keys [^ShapeDrawer shape-drawer]} [x y] radius color]
    (set-color shape-drawer color)
    (.filledCircle shape-drawer (float x) (float y) (float radius)))

  (draw-arc [{:keys [^ShapeDrawer shape-drawer]} [centre-x centre-y] radius start-angle degree color]
    (set-color shape-drawer color)
    (.arc shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

  (draw-sector [{:keys [^ShapeDrawer shape-drawer]} [centre-x centre-y] radius start-angle degree color]
    (set-color shape-drawer color)
    (.sector shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

  (draw-rectangle [{:keys [^ShapeDrawer shape-drawer]} x y w h color]
    (set-color shape-drawer color)
    (.rectangle shape-drawer x y w h) )

  (draw-filled-rectangle [{:keys [^ShapeDrawer shape-drawer]} x y w h color]
    (set-color shape-drawer color)
    (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)) )

  (draw-line [{:keys [^ShapeDrawer shape-drawer]} [sx sy] [ex ey] color]
    (set-color shape-drawer color)
    (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

  (draw-grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* gridw cellw)
          h (* gridh cellh)
          topy (+ bottomy h)
          rightx (+ leftx w)]
      (doseq [idx (range (inc gridw))
              :let [linex (+ leftx (* idx cellw))]]
        (gdl.context/draw-line this [linex topy] [linex bottomy] color))
      (doseq [idx (range (inc gridh))
              :let [liney (+ bottomy (* idx cellh))]]
        (gdl.context/draw-line this [leftx liney] [rightx liney] color))))

  (with-shape-line-width [{:keys [^ShapeDrawer shape-drawer]} width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth shape-drawer (float old-line-width)))))

(defn ->context [{:keys [batch]}]
  (let [texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                               (.setColor ^Color color/white)
                               (.drawPixel 0 0))
                      texture (Texture. pixmap)]
                  (.dispose pixmap)
                  texture)]
    {:shape-drawer (ShapeDrawer. batch (TextureRegion. texture 0 0 1 1))
     :shape-drawer-texture texture}))
