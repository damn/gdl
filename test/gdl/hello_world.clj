(ns gdl.hello-world
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.graphics :as g]
            [gdl.screen :refer [Screen]]))

(defrecord MyScreen []
  Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ {g :context/graphics}]
    (g/render-gui-view g #(g/draw-text % {:text "Hello World!" :x 400, :y 300}))))

(defn create-context [default-context]
  (assoc default-context ::my-screen (->MyScreen)))

(defn -main []
  (app/start {:app {:title "Hello World"
                    :width 800
                    :height 600
                    :full-screen? false}
              :create-context create-context
              :first-screen ::my-screen}))
