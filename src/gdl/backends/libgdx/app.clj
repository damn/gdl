(ns gdl.backends.libgdx.app
  (:require [gdl.app :refer [current-context]]
            [gdl.screen :as screen]
            [gdl.disposable :refer [dispose]]
            [gdl.context :refer [current-screen change-screen]]
            [gdl.graphics.color :as color]
            (gdl.backends.libgdx.context [assets :as assets]
                                         graphics
                                         [gui-world-views :as views]
                                         image-drawer-creator
                                         input
                                         [shape-drawer :as shape-drawer]
                                         [sprite-batch :as sprite-batch]
                                         stage
                                         [text-drawer :as text-drawer]
                                         tiled
                                         ttf-generator
                                         [vis-ui :as vis-ui]))
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           com.badlogic.gdx.utils.ScreenUtils))

(extend-type gdl.context.Context
  gdl.context/Application
  (exit-app [_]
    (.exit Gdx/app)))

(defn- ->default-context [world-unit-scale]
  (let [context (sprite-batch/->context)]
    (-> context
        (merge (shape-drawer/->context context) ; requires batch
               (assets/->context)
               (views/->context (or world-unit-scale 1))
               (text-drawer/->context)
               (vis-ui/->context)
               {:context/disposables (atom #{})})
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
    (dispose value))
  (doseq [object @(:context/disposables context)]
    ;(println "Disposing " object)
    (dispose object)))

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

(defn- ->application [{:keys [create-context
                              first-screen
                              world-unit-scale]}]
  (proxy [ApplicationAdapter] []
    (create []
      (let [context (-> (->default-context world-unit-scale)
                        create-context
                        (change-screen first-screen))]
        (reset! current-context context)))

    (dispose []
      (dispose-all @current-context))

    (render []
      (ScreenUtils/clear color/black)
      (let [context @current-context]
        (views/fix-viewport-update context)
        (screen/render (current-screen context) context)))

    (resize [w h]
      (views/update-viewports @current-context w h))))

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
