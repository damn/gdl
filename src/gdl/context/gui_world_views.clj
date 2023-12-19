; TODO make 2 = gui-view & world-view ?
; which has view interface ....
; I don't need in my test world-view & I don't need scene2d.ui
; => default-modules move to test
; => tree structure
(ns gdl.context.gui-world-views
  (:require gdl.context)
  (:import com.badlogic.gdx.Gdx
           (com.badlogic.gdx.graphics Color OrthographicCamera)
           com.badlogic.gdx.graphics.g2d.Batch
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.badlogic.gdx.math Vector2 MathUtils)))

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
                     :gui 1
                     :world world-unit-scale)
        context (assoc context :unit-scale unit-scale)]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (.combined camera))
    (.begin batch)
    (gdl.context/with-shape-line-width context
      unit-scale
      #(draw-fn context))
    (.end batch)))

(extend-type gdl.context.Context
  gdl.context/GuiWorldViews
  (render-gui-view [this render-fn]
    (render-view this :gui render-fn))

  (render-world-view [this render-fn]
    (render-view this :world render-fn))

  (update-viewports [{:keys [gui-viewport world-viewport]} w h]
    (.update ^Viewport gui-viewport   w h true)
    ; Do not center the camera on world-viewport. We set the position there manually.
    (.update ^Viewport world-viewport w h false))

  (fix-viewport-update
    [{:keys [^Viewport gui-viewport] :as context}]
    (let [screen-width  (.getWidth  Gdx/graphics)
          screen-height (.getHeight Gdx/graphics)]
      (when-not (and (= (.getScreenWidth  gui-viewport) screen-width)
                     (= (.getScreenHeight gui-viewport) screen-height))
        (gdl.context/update-viewports context screen-width screen-height))))

  (assoc-view-mouse-positions [context]
    (assoc context
           :gui-mouse-position (mapv int (unproject-mouse-posi (:gui-viewport context)))
           ; TODO clamping only works for gui-viewport ? check. comment if true
           ; TODO ? "Can be negative coordinates, undefined cells."
           :world-mouse-position (unproject-mouse-posi (:world-viewport context))))

  (pixels->world-units [{:keys [world-unit-scale]} pixels]
    (* pixels world-unit-scale)))

(defn ->context [& {:keys [tile-size]}]
  (merge {:unit-scale 1} ;  TODO not here ? only used @ gui drawings without render-view in 2 widgets .... ? or part of gui
         (let [gui-camera (OrthographicCamera.)
               gui-viewport (FitViewport. (.getWidth  Gdx/graphics)
                                          (.getHeight Gdx/graphics)
                                          gui-camera)]
           {:gui-camera   gui-camera
            :gui-viewport gui-viewport
            :gui-viewport-width  (.getWorldWidth  gui-viewport)
            :gui-viewport-height (.getWorldHeight gui-viewport)})
         (let [world-camera (OrthographicCamera.)
               world-unit-scale (/ (or tile-size 1))
               world-viewport (let [width  (* (.getWidth Gdx/graphics) world-unit-scale)
                                    height (* (.getHeight Gdx/graphics) world-unit-scale)
                                    y-down? false]
                                (.setToOrtho world-camera y-down? width height)
                                (FitViewport. width height world-camera))]
           {:world-unit-scale world-unit-scale
            :world-camera     world-camera
            :world-viewport   world-viewport
            :world-viewport-width  (.getWorldWidth  world-viewport)
            :world-viewport-height (.getWorldHeight world-viewport)})))
