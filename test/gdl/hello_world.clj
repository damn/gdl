(ns gdl.hello-world
  (:require [gdl.app :as app]
            [gdl.default-context :as default-context]
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

(defn create-context []
  (-> (default-context/->context)
      (assoc :my-screen (->MyScreen))))

(def current-context (atom nil))

(defn -main []
  (app/start {:app {:title "Hello World"
                    :width 800
                    :height 600
                    :full-screen? false}
              :current-context current-context
              :create-context create-context
              :first-screen :my-screen}))
