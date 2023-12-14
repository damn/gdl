(ns gdl.app
  "Main namespace for starting an app and changing the current screen.

  Sets up SpriteBatch, viewports for GUI and WORLD, and supplies lifecycle management."
  (:require [clojure.string :as str]
            [gdl.lifecycle :as lc]
            [gdl.graphics.draw :as draw]
            gdl.scene2d.ui)
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.assets.AssetManager
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.files.FileHandle
           (com.badlogic.gdx.graphics Color Texture OrthographicCamera Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d Batch SpriteBatch BitmapFont TextureRegion)
           (com.badlogic.gdx.utils Align ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           [com.badlogic.gdx.math Vector2 MathUtils]
           space.earlygrey.shapedrawer.ShapeDrawer))

(defn- degree->radians [degree]
  (* degree (/ (Math/PI) 180)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw-texture [^Batch batch texture [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch texture
         x
         y
         (/ w 2) ; rotation origin
         (/ h 2)
         w ; width height
         h
         1 ; scaling factor
         1
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- unit-dimensions [unit-scale image]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defrecord Drawer [batch unit-scale default-font ^ShapeDrawer shape-drawer]
  draw/Drawer
  (text [_ {:keys [font text x y h-align up?]}]
    (let [^BitmapFont font (or font default-font)
          data (.getData font)
          old-scale (.scaleX data)]
      (.setScale data (float (* old-scale unit-scale)))
      (.draw font
             batch
             (str text)
             (float x)
             (float (+ y (if up? (text-height font text) 0)))
             (float 0) ; target-width
             (case (or h-align :center)
               :center Align/center
               :left   Align/left
               :right  Align/right)
             false) ; wrap false, no need target-width
      (.setScale data (float old-scale))))
  (image [_ {:keys [texture color] :as image} position]
    (draw-texture batch texture position (unit-dimensions unit-scale image) 0 color))
  (image [this image x y]
    (draw/image this image [x y]))
  (rotated-centered-image [_ {:keys [texture color] :as image} rotation [x y]]
    (let [[w h] (unit-dimensions unit-scale image)]
      (draw-texture batch
                    texture
                    [(- x (/ w 2))
                     (- y (/ h 2))]
                    [w h]
                    rotation
                    color)))
  (centered-image [this image position]
    (draw/rotated-centered-image this image 0 position))
  (ellipse [_ [x y] radius-x radius-y color]
    (.setColor shape-drawer ^Color color)
    (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)) )
  (filled-ellipse [_ [x y] radius-x radius-y color]
    (.setColor shape-drawer ^Color color)
    (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))
  (circle [_ [x y] radius color]
    (.setColor shape-drawer ^Color color)
    (.circle shape-drawer (float x) (float y) (float radius)))
  (filled-circle [_ [x y] radius color]
    (.setColor shape-drawer ^Color color)
    (.filledCircle shape-drawer (float x) (float y) (float radius)))
  (arc [_ [centre-x centre-y] radius start-angle degree color]
    (.setColor shape-drawer ^Color color)
    (.arc shape-drawer centre-x centre-y radius start-angle (degree->radians degree)))
  (sector [_ [centre-x centre-y] radius start-angle degree color]
    (.setColor shape-drawer ^Color color)
    (.sector shape-drawer centre-x centre-y radius start-angle (degree->radians degree)))
  (rectangle [_ x y w h color]
    (.setColor shape-drawer ^Color color)
    (.rectangle shape-drawer x y w h) )
  (filled-rectangle [_ x y w h color]
    (.setColor shape-drawer ^Color color)
    (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)) )
  (line [this [x y] [ex ey] color]
    (draw/line this x y ex ey color))
  (line [_ x y ex ey color]
    (.setColor shape-drawer ^Color color)
    (.line shape-drawer (float x) (float y) (float ex) (float ey)))
  (grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* gridw cellw)
          h (* gridh cellh)
          topy (+ bottomy h)
          rightx (+ leftx w)]
      (doseq [idx (range (inc gridw))
              :let [linex (+ leftx (* idx cellw))]]
        (draw/line this linex topy linex bottomy color))
      (doseq [idx (range (inc gridh))
              :let [liney (+ bottomy (* idx cellh))]]
        (draw/line this leftx liney rightx liney color))))
  (with-line-width [_ width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth shape-drawer (float old-line-width)))))

(defn- ->drawer [context]
  (assert (:default-font context))
  (-> context
      (select-keys [:batch :default-font :shape-drawer])
      (assoc :unit-scale 1)
      map->Drawer))

(defn render-with [{:keys [^Batch batch
                           shape-drawer
                           drawer
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
        drawer (assoc drawer :unit-scale unit-scale)]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (.combined camera))
    (.begin batch)
    (draw/with-line-width drawer unit-scale #(draw-fn drawer))
    (.end batch)))

(defn- update-viewports [{:keys [gui-viewport world-viewport]} w h]
  (let [center-camera? true]
    (.update ^Viewport gui-viewport   w h center-camera?)
    (.update ^Viewport world-viewport w h center-camera?)))

(defn- fix-viewport-update
  "Sometimes the viewport update is not triggered."
  ; TODO (on mac osx, when resizing window, make bug report, fix it in libgdx?)
  [{:keys [^Viewport gui-viewport] :as context}]
  (let [screen-width (.getWidth Gdx/graphics)
        screen-height (.getHeight Gdx/graphics)]
    (when-not (and (= (.getScreenWidth  gui-viewport) screen-width)
                   (= (.getScreenHeight gui-viewport) screen-height))
      (update-viewports context screen-width screen-height))))

(defn- recursively-search-files [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file) result
          (.isDirectory file) (recur (concat remaining (.list file)) result)
          (extensions (.extension file)) (recur remaining (conj result (str/replace-first (.path file) folder "")))
          :else (recur remaining result))))

(defn- load-assets [^AssetManager manager folder file-extensions ^Class klass log-load-assets?]
  (doseq [file (recursively-search-files folder file-extensions)]
    (when log-load-assets?
      (println "load-assets" (str "[" (.getSimpleName klass) "] - [" file "]")))
    (.load manager file klass)))

(defn- load-all-assets [{:keys [folder
                                log-load-assets?
                                sound-files-extensions
                                image-files-extensions]
                         :as config}]
  (doseq [k [:folder
             :log-load-assets?
             :sound-files-extensions
             :image-files-extensions]]
    (assert (contains? config k)
            (str "config key(s) missing: " k)))
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [file]
                    (.get ^AssetManager this ^String file)))]
    (load-assets manager folder sound-files-extensions Sound   log-load-assets?)
    (load-assets manager folder image-files-extensions Texture log-load-assets?)
    (.finishLoading manager)
    manager))

; TODO ! all keywords add namespace ':context/'
(defn- default-components [{:keys [tile-size]}]
  (let [batch (SpriteBatch.)]
    (merge {:batch batch
            :assets (load-all-assets {:folder "resources/" ; TODO these are classpath settings ?
                                      :sound-files-extensions #{"wav"}
                                      :image-files-extensions #{"png" "bmp"}
                                      :log-load-assets? false})
            :context/scene2d.ui (gdl.scene2d.ui/initialize!)}
           (let [texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                        (.setColor Color/WHITE)
                                        (.drawPixel 0 0))
                               texture (Texture. pixmap)]
                           (.dispose pixmap)
                           texture)]
             {:shape-drawer (ShapeDrawer. batch (TextureRegion. texture 0 0 1 1))
              :shape-drawer-texture texture})
           (let [gui-camera (OrthographicCamera.)
                 gui-viewport (FitViewport. (.getWidth Gdx/graphics)
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
              :world-viewport-height (.getWorldHeight world-viewport)}))))

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

; maybe functions 'mouse-position' on 'view' ?
(defn- update-mouse-positions [context]
  (assoc context
         :gui-mouse-position (mapv int (unproject-mouse-posi (:gui-viewport context)))
         ; TODO clamping only works for gui-viewport ? check. comment if true
         ; TODO ? "Can be negative coordinates, undefined cells."
         :world-mouse-position (unproject-mouse-posi (:world-viewport context))))

(def ^:private state (atom nil))

(defn current-context []
  (update-mouse-positions @state))

; TODO here not current-context .... should not do @state or get mouse-positions via function call
; but then keep unprojecting ?
; TODO TEST current logic of that screen will be continued ?
(defn change-screen! [new-screen-key]
  (let [{:keys [context/current-screen] :as context} @state]
    (when-let [previous-screen (get context current-screen)]
      (lc/hide previous-screen))
    (let [new-screen (new-screen-key context)]
      (assert new-screen (str "Cannot find screen with key: " new-screen-key))
      (swap! state assoc :context/current-screen new-screen-key)
      (lc/show new-screen @state))))

(defn- dispose-context [context]
  (doseq [[k value] context]
    (cond (extends? lc/Disposable (class value))
          (do
           (println "Disposing " k)
           (lc/dispose value))
          ((supers (class value)) com.badlogic.gdx.utils.Disposable)
          (do
           (println "Disposing " k)
           (.dispose ^com.badlogic.gdx.utils.Disposable value)))))

(defn- application-adapter [{:keys [modules first-screen] :as config}]
  (proxy [ApplicationAdapter] []
    (create []
      (reset! state
              (let [context (default-components config)
                    ; TODO safe-merge
                    context (merge context (modules context))]
                (assoc context :drawer (->drawer context))))
      (change-screen! first-screen))
    (dispose []
      (dispose-context @state))
    (render []
      (ScreenUtils/clear Color/BLACK)
      (let [{:keys [context/current-screen] :as context} (current-context)
            screen (current-screen context)]
        (fix-viewport-update context)
        (lc/render screen context)
        (lc/tick screen context (* (.getDeltaTime Gdx/graphics) 1000))))
    (resize [w h]
      ; TODO here also @state and not current-context ...
      (update-viewports @state w h))))

(defn- lwjgl3-configuration [{:keys [title width height full-screen? fps]}]
  ; https://github.com/trptr/java-wrapper/blob/39a0947f4e90857512c1999537d0de83d130c001/src/trptr/java_wrapper/locale.clj#L87
  ; cond->
  (let [config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle title)
                 (.setForegroundFPS (or fps 60)))]
    (if full-screen?
      (.setFullscreenMode config (Lwjgl3ApplicationConfiguration/getDisplayMode))
      (.setWindowedMode config width height))
    config))

(defn start [config]
  (Lwjgl3Application. (application-adapter config)
                      (lwjgl3-configuration (:app config))))

(defn pixels->world-units [{:keys [world-unit-scale]} pixels]
  (* pixels world-unit-scale))
