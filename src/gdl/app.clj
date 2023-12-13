(ns gdl.app
  (:require [clojure.string :as str]
            [x.x :refer [defcomponent update-map]]
            [gdl.lc :as lc]
            [gdl.draw :as draw]
            [gdl.graphics.viewport :as viewport]
            [gdl.scene2d.ui :as ui])
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.assets.AssetManager
           com.badlogic.gdx.files.FileHandle
           (com.badlogic.gdx.utils Align ScreenUtils)
           (com.badlogic.gdx.graphics Color Texture OrthographicCamera Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d Batch SpriteBatch BitmapFont TextureRegion)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.utils.SharedLibraryLoader
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
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
    (image this image [x y]))
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
  (line [_ [x y] [ex ey] color]
    (draw/line shape-drawer x y ex ey color))
  (line [_ x y ex ey color]
    (.setColor shape-drawer ^Color color)
    (.line shape-drawer (float x) (float y) (float ex) (float ey)))
  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
      (draw-fn this)
      (.setDefaultLineWidth shape-drawer (float old-line-width)))))

(defn render-with [{:keys [^Batch batch
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
        drawer (-> context
                   (select-keys [:batch :default-font :shape-drawer])
                   (assoc :unit-scale unit-scale)
                   map->Drawer)]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (.combined camera))
    (.begin batch)
    (draw/with-line-width drawer unit-scale draw-fn)
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

(defcomponent :batch batch
  (lc/dispose [_]
    (.dispose ^Batch batch)))

(defcomponent :assets manager
  (lc/dispose [_]
    (.dispose ^AssetManager manager)))

(defcomponent :shape-drawer-texture texture
  (lc/dispose [_]
    (.dispose ^Texture texture)))

(defn- default-components [{:keys [tile-size]}]
  (let [batch (SpriteBatch.)]
    (merge {:batch batch
            :assets (load-all-assets {:folder "resources/" ; TODO these are classpath settings ?
                                      :sound-files-extensions #{"wav"}
                                      :image-files-extensions #{"png" "bmp"}
                                      :log-load-assets? false})
            ; this is the gdx default skin  - copied from libgdx project, check not included in libgdx jar somewhere?
            :gdl.scene2d.ui (ui/skin (.internal Gdx/files "scene2d.ui.skin/uiskin.json"))}
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

(def ^:private state (atom nil))

(defn- update-mouse-positions [context]
  (assoc context
         :gui-mouse-position (mapv int (viewport/unproject-mouse-posi (:gui-viewport context)))
         ; TODO clamping only works for gui-viewport ? check. comment if true
         ; TODO ? "Can be negative coordinates, undefined cells."
         :world-mouse-position (viewport/unproject-mouse-posi (:world-viewport context))))

(defn current-context []
  (update-mouse-positions @state))

(defn- current-screen-component []
  (let [k (::current-screen @state)]
    [k (k @state)]))

(defn current-screen-value []
  ((::current-screen @state) @state))

(defn set-screen [k]
  (assert (contains? @state k) (str "Cannot find screen with key: " k " in state."))
  (when (::current-screen @state)
    (lc/hide (current-screen-component)))
  (swap! state assoc ::current-screen k)
  (lc/show (current-screen-component)
           (current-context)))

(defn- application-adapter [{:keys [modules first-screen] :as config}]
  (proxy [ApplicationAdapter] []
    (create []
      (reset! state
              (let [context (update-map (default-components config) lc/create nil)]
                (merge context
                       (update-map modules lc/create context))))
      (set-screen first-screen))
    (dispose []
      (swap! state update-map lc/dispose))
    (render []
      (ScreenUtils/clear Color/BLACK)
      (let [context (current-context)]
        (fix-viewport-update context)
        (lc/render (current-screen-component) context)
        (lc/tick (current-screen-component)
                 context
                 (* (.getDeltaTime Gdx/graphics) 1000))))
    (resize [w h]
      (update-viewports @state w h))))

(defn- lwjgl3-configuration [{:keys [title width height full-screen? fps]}]
  #_(when SharedLibraryLoader/isMac
      (mac-dock-icon/set-mac-os-dock-icon))
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
