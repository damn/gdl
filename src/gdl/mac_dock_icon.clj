(ns ^:no-doc gdl.mac-dock-icon
  (:require [clojure.java.io :as io])
  (:import javax.imageio.ImageIO))

; TODO :
; -> move to libgdx lwjgl3 backend code?
; https://github.com/libgdx/libgdx/blob/75612dae1eeddc9611ed62366858ff1d0ac7898b/backends/gdx-backend-lwjgl3/src/com/badlogic/gdx/backends/lwjgl3/Lwjgl3Window.java#L281

; https://github.com/glfw/glfw/issues/2041

; Alternative:
; java.awt.Toolkit
; java.awt.Taskbar

;  private static void updateDockIconForMacOs() {
;        if (SharedLibraryLoader.isMac) {
;            try {
;
;                Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
;                // icon.png is in assets
;                final URL imageResource = DesktopLauncher.class.getClassLoader().getResource("icon.png");
;                final Image image = defaultToolkit.getImage(imageResource);
;                Taskbar taskbar = Taskbar.getTaskbar();
;                taskbar.setIconImage(image);
;            } catch (Throwable throwable) {
;                throwable.printStackTrace()
;            }
;        }
;    }

; setWindowIcon not working libgdx if mac :
; https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-lwjgl3/src/com/badlogic/gdx/backends/lwjgl3/Lwjgl3Window.java#L159
(defn set-mac-os-dock-icon []

  ; https://github.com/LWJGL/lwjgl3/issues/68
  ; This is because using those objects will cause the AWT to run it's event loop, and on OSX, the main thread is already used by GLFW. The solution is to set the headless property of the AWT to true.
  ; Set that property before creating the window, then create the window. Also note that you have to create the fonts or the resources only after the window is created.

  ; do set only once in case of
  ; clojure.tools.namespace.repl/refresh-all
  ; otherwise it gives a console warning every app start
  (let [property "java.awt.headless",value "true"]
    (when-not (= (System/getProperty property) value)
      (System/setProperty property value)))

  ; TODO in jar file resources/ in classpath -> no need to give path here?

  ; TODO in dependent projects doesnt work !
  ; how to find the logo file ??
  (import 'com.apple.eawt.Application)
  (let [image (ImageIO/read (io/file (str "resources/logo.png")))]
    (.setDockIconImage (com.apple.eawt.Application/getApplication) image)))
