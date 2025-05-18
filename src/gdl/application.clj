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

(defn start! [{:keys [title
                      window-width
                      window-height
                      fps
                      dock-icon
                      create!
                      dispose!
                      render!
                      resize!]}]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async") ; GLFW/lwjgl is abstracted by libgdx lwjgl3 backend -> move there
    (.setIconImage (Taskbar/getTaskbar) ; -> try also to move there?
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon))))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (when create! (create!)))

                        (dispose []
                          (when dispose! (dispose!)))

                        (render []
                          (when render! (render!)))

                        (resize [width height]
                          (when resize! (resize! width height))))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width window-height)
                        (.setForegroundFPS fps))))

(defmacro post-runnable! [& exprs]
  (.postRunnable Gdx/app (fn [] ~@exprs)))
