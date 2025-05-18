(ns gdl.application
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create! [_])
  (dispose! [_])
  (render! [_])
  (resize! [_]))

(defn- gdx-listener [listener]
  (proxy [ApplicationAdapter] []
    (create []
      (create! listener))

    (dispose []
      (dispose! listener))

    (render []
      (render! listener))

    (resize [_width _height]
      (resize! listener))))

(defn start! [listener
              {:keys [title
                      window-width
                      window-height
                      fps
                      dock-icon]}]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon))))
  (Lwjgl3Application. (gdx-listener listener)
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width window-height)
                        (.setForegroundFPS fps))))

(defmacro post-runnable! [& exprs]
  (.postRunnable Gdx/app (fn [] ~@exprs)))
