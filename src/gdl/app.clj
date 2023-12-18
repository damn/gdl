(ns gdl.app
  (:require [gdl.screen :as screen]
            [gdl.protocols :refer [dispose]])
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.graphics.Color
           com.badlogic.gdx.utils.ScreenUtils))

(def state (atom nil))

(defn current-context []
  (gdl.protocols/assoc-view-mouse-positions @state))

; TODO here not current-context .... should not do @state or get mouse-positions via function call
; but then keep unprojecting ?
; TODO TEST current logic of that screen will be continued ?
(defn change-screen! [new-screen-key]
  (let [{:keys [context/current-screen] :as context} @state]
    (when-let [previous-screen (get context current-screen)]
      (screen/hide previous-screen context))
    (let [new-screen (new-screen-key context)]
      (assert new-screen (str "Cannot find screen with key: " new-screen-key))
      (swap! state assoc :context/current-screen new-screen-key)
      (screen/show new-screen @state))))

(defn- dispose-context [context]
  (doseq [[k value] context]
    (cond (extends? gdl.protocols/Disposable (class value))
          (do
           (println "Disposing " k)
           (dispose value))
          ((supers (class value)) com.badlogic.gdx.utils.Disposable)
          (do
           (println "Disposing " k)
           (.dispose ^com.badlogic.gdx.utils.Disposable value)))))

(defn- application-adapter [{:keys [context-fn first-screen]}]
  (proxy [ApplicationAdapter] []
    (create []
      (reset! state (context-fn))
      (change-screen! first-screen))
    (dispose []
      (dispose-context @state))
    (render []
      (ScreenUtils/clear Color/BLACK)
      (let [{:keys [context/current-screen] :as context} (current-context)
            screen (current-screen context)]

        ; "Sometimes the viewport update is not triggered."
        ; TODO (on mac osx, when resizing window, make bug report, fix it in libgdx?)
        (gdl.protocols/fix-viewport-update context)
        (screen/render screen context)
        (screen/tick screen context (* (.getDeltaTime Gdx/graphics) 1000))))
    (resize [w h]
      ; TODO here also @state and not current-context ...
      (gdl.protocols/update-viewports @state w h))))

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
