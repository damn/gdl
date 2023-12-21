(ns gdl.hello-world
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :refer [render-gui-view draw-text]]
            [gdl.screen :refer [Screen]]))

(defn draw [context]
  (draw-text context {:text "Hello World!" :x 400, :y 300}))

(defrecord MyScreen []
  Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ context]
    (render-gui-view context draw))
  (tick [_ _context _delta]))

(defn create-context [default-context]
  (assoc default-context ::my-screen (->MyScreen)))

(defn -main []
  (app/start {:app {:title "Hello World"
                    :width 800
                    :height 600
                    :full-screen? false}
              :create-context create-context
              :first-screen ::my-screen}))
