(ns gdl.hello-world
  (:require [gdl.app :as app]
            gdl.default-context
            [gdl.context :refer [render-gui-view draw-text]]
            gdl.screen))

(defn draw-test [context]
  (draw-text context {:text "Hello World!" :x 400, :y 300}))

(defrecord Screen []
  gdl.screen/Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ context]
    (render-gui-view context draw-test))
  (tick [_ _context _delta]))

(defn create-context []
  (merge (gdl.default-context/->context)
         {:my-screen (->Screen)}))

(defn -main []
  (app/start {:app {:title "Hello World"
                    :width 800
                    :height 600
                    :full-screen? false}
              :context-fn create-context
              :first-screen :my-screen}))
