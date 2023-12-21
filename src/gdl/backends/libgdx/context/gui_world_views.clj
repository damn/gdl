(ns gdl.backends.libgdx.context.gui-world-views
  (:require gdl.context
            [gdl.graphics.color :as color])
  (:import com.badlogic.gdx.Gdx
           (com.badlogic.gdx.graphics OrthographicCamera)
           com.badlogic.gdx.graphics.g2d.Batch
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.badlogic.gdx.math Vector2 MathUtils)))

(def ^:private gui-unit-scale 1)

(defn- screen-width  [] (.getWidth  Gdx/graphics))
(defn- screen-height [] (.getHeight Gdx/graphics))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-posi [^Viewport viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (.getLeftGutterWidth viewport)
                       (.getRightGutterX viewport))
        mouse-y (clamp (.getY Gdx/input)
                       (.getTopGutterHeight viewport)
                       (.getTopGutterY viewport))
        coords (.unproject viewport (Vector2. mouse-x mouse-y))]
    [(.x coords) (.y coords)]))

(defn- render-view [{:keys [^Batch batch
                            shape-drawer
                            gui-camera
                            world-camera
                            world-unit-scale]
                     :as context}
                    gui-or-world
                    draw-fn]
  (let [^OrthographicCamera camera (case gui-or-world
                                     :gui gui-camera
                                     :world world-camera)
        unit-scale (case gui-or-world
                     :gui gui-unit-scale
                     :world world-unit-scale)]
    (.setColor batch color/white) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (.combined camera))
    (.begin batch)
    (gdl.context/with-shape-line-width
      context
      unit-scale
      #(draw-fn (assoc context :unit-scale unit-scale)))
    (.end batch)))

(defn update-viewports [{:keys [gui-viewport world-viewport]} w h]
  (.update ^Viewport gui-viewport w h true)
  ; Do not center the camera on world-viewport. We set the position there manually.
  (.update ^Viewport world-viewport w h false))

(defn- viewport-fix-required? [{:keys [^Viewport gui-viewport]}]
  (or (not= (.getScreenWidth  gui-viewport) (screen-width))
      (not= (.getScreenHeight gui-viewport) (screen-height))))

; TODO on mac osx, when resizing window, make bug report /  fix it in libgdx?
(defn fix-viewport-update
  "Sometimes the viewport update is not triggered."
  [context]
  (when (viewport-fix-required? context)
    (update-viewports context (screen-width) (screen-height))))

(extend-type gdl.context.Context
  gdl.context/GuiWorldViews
  (render-gui-view   [this render-fn] (render-view this :gui   render-fn))
  (render-world-view [this render-fn] (render-view this :world render-fn))

  (gui-mouse-position [{:keys [gui-viewport]}]
    ; TODO mapv int needed?
    (mapv int (unproject-mouse-posi gui-viewport)))

  (world-mouse-position [{:keys [world-viewport]}]
    ; TODO clamping only works for gui-viewport ? check. comment if true
    ; TODO ? "Can be negative coordinates, undefined cells."
    (unproject-mouse-posi world-viewport))

  (pixels->world-units [{:keys [world-unit-scale]} pixels]
    (* pixels world-unit-scale)))

(defn ->context [world-unit-scale]
  {:pre [(number? world-unit-scale)]}
  (merge {:unit-scale gui-unit-scale} ; only here because actors want to use drawing without using render-gui-view
         (let [gui-camera (OrthographicCamera.)
               gui-viewport (FitViewport. (screen-width) (screen-height) gui-camera)]
           {:gui-camera   gui-camera
            :gui-viewport gui-viewport
            :gui-viewport-width  (.getWorldWidth  gui-viewport)
            :gui-viewport-height (.getWorldHeight gui-viewport)})
         (let [world-camera (OrthographicCamera.)
               world-viewport (let [width  (* (screen-width) world-unit-scale)
                                    height (* (screen-height) world-unit-scale)
                                    y-down? false]
                                (.setToOrtho world-camera y-down? width height)
                                (FitViewport. width height world-camera))]
           {:world-unit-scale world-unit-scale
            :world-camera     world-camera
            :world-viewport   world-viewport
            :world-viewport-width  (.getWorldWidth  world-viewport)
            :world-viewport-height (.getWorldHeight world-viewport)})))
