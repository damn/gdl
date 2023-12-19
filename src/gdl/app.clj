(ns gdl.app
  (:require [gdl.screen :as screen]
            gdl.disposable
            [gdl.context :refer [current-screen change-screen update-viewports fix-viewport-update]])
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.graphics.Color
           com.badlogic.gdx.utils.ScreenUtils))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.disposable/Disposable
  (dispose [this]
    (.dispose this)))

(defn- dispose-all [context]
  (doseq [[k value] context
          :when (some #(extends? gdl.disposable/Disposable %)
                      (supers (class value)))]
    (println "Disposing " k)
    (gdl.disposable/dispose value)))

(extend-type gdl.context.Context
  gdl.context/ApplicationScreens
  (current-screen [{:keys [context/current-screen] :as context}]
    (get context current-screen))

  (change-screen [context new-screen-key]
    (when-let [screen (current-screen context)]
      (screen/hide screen context)
      (screen/hide screen context))
    (let [screen (new-screen-key context)
          _ (assert screen (str "Cannot find screen with key: " new-screen-key))
          new-context (assoc context :context/current-screen new-screen-key)]
      (screen/show screen new-context)
      new-context)))

(defn- ->application [{:keys [current-context create-context first-screen]}]
  (proxy [ApplicationAdapter] []
    (create []
      (reset! current-context (change-screen (create-context) first-screen)))
    (dispose []
      (dispose-all @current-context))
    (render []
      (ScreenUtils/clear Color/BLACK)
      (let [context @current-context
            screen (current-screen context)]
        (fix-viewport-update context)
        (screen/render screen context)
        (screen/tick screen context (* (.getDeltaTime Gdx/graphics) 1000))))
    (resize [w h]
      (update-viewports @current-context w h))))

(defn- lwjgl3-configuration [{:keys [title width height full-screen? fps]}]
  (let [config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle title)
                 (.setForegroundFPS (or fps 60)))]
    (if full-screen?
      (.setFullscreenMode config (Lwjgl3ApplicationConfiguration/getDisplayMode))
      (.setWindowedMode config width height))
    config))

(defn start
  "Example for required keys:
             {:app {:title \"gdl demo\"
                    :width 800
                    :height 600
                    :full-screen? false}
              :current-context current-context ; an atom
              :create-context create-context ; function with no args creating the context
              :first-screen :my-screen}"
  [config]
  (assert (:current-context config) ":current-context key not supplied")
  (assert (:create-context config) ":create-context key not supplied")
  (assert (:first-screen config) ":first-screen key not supplied")
  (Lwjgl3Application. (->application config)
                      (lwjgl3-configuration (:app config))))
