(ns gdl.app
  (:require [gdl.context.assets :as assets]
            [gdl.context.gui-world-views :as gui-world-views :refer [update-viewports fix-viewport-update]]
            gdl.context.image-drawer-creator
            [gdl.context.shape-drawer :as shape-drawer]
            [gdl.context.sprite-batch :as sprite-batch]
            gdl.context.stage
            [gdl.context.text-drawer :as text-drawer]
            gdl.context.ttf-generator
            [gdl.context.vis-ui :as vis-ui]
            [gdl.screen :as screen]
            gdl.disposable
            [gdl.context :refer [current-screen change-screen ]])
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.graphics.Color
           com.badlogic.gdx.utils.ScreenUtils))

(defn- ->default-context [world-unit-scale]
  (let [context (sprite-batch/->context)]
    (-> context
        (merge (shape-drawer/->context context) ; requires batch
               (assets/->context)
               (gui-world-views/->context (or world-unit-scale 1))
               (text-drawer/->context)
               (vis-ui/->context))
        gdl.context/map->Context)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.disposable/Disposable
  (dispose [this]
    (.dispose this)))

(defn- dispose-all [context]
  (doseq [[k value] context
          :when (some #(extends? gdl.disposable/Disposable %)
                      (supers (class value)))]
    ;(println "Disposing " k)
    (gdl.disposable/dispose value)))

(extend-type gdl.context.Context
  gdl.context/ApplicationScreens
  (current-screen [{:keys [context/current-screen] :as context}]
    (get context current-screen))

  (change-screen [context new-screen-key]
    (when-let [screen (current-screen context)]
      (screen/hide screen context))
    (let [screen (new-screen-key context)
          _ (assert screen (str "Cannot find screen with key: " new-screen-key))
          new-context (assoc context :context/current-screen new-screen-key)]
      (screen/show screen new-context)
      new-context)))

(def current-context (atom nil))

(defn- ->application [{:keys [create-context
                              first-screen
                              world-unit-scale]}]
  (proxy [ApplicationAdapter] []
    (create []
      (let [context (-> (->default-context world-unit-scale)
                        (assoc :gdl.app/current-context current-context)
                        create-context
                        (change-screen first-screen))]
        (reset! current-context context)))

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
  {:pre [title
         width
         height
         (boolean? full-screen?)
         (or (nil? fps) (int? fps))]}
  (let [config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle title)
                 (.setForegroundFPS (or fps 60)))]
    (if full-screen?
      (.setFullscreenMode config (Lwjgl3ApplicationConfiguration/getDisplayMode))
      (.setWindowedMode config width height))
    config))

(defn start
  "Required keys:
   {:app {:title \"gdl demo\"
          :width 800
          :height 600
          :full-screen? false}
    :create-context create-context ; function with one argument creating the context, getting the default-context.
    :first-screen :my-screen}

  Optionally you can pass :world-unit-scale for the world-view."
  [config]
  (assert (:create-context config))
  (assert (:first-screen   config))
  (Lwjgl3Application. (->application config)
                      (lwjgl3-configuration (:app config))))
