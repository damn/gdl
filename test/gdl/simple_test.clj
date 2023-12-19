(ns gdl.simple-test
  (:require [gdl.app :as app]
            [gdl.default-context :as default-context]
            gdl.disposable
            [gdl.context :refer [draw-centered-image draw-circle draw-text generate-ttf create-image render-gui-view
                                 gui-mouse-position world-mouse-position]]
            gdl.screen)
  (:import com.badlogic.gdx.graphics.Color))

(defn draw-test [{:keys [special-font logo] :as context}]
  (let [[wx wy] (map #(format "%.2f" %) (world-mouse-position context))
        [gx gy] (gui-mouse-position context)
        the-str (str "World x " wx "\n"
                     "World y " wy "\n"
                     "GUI x " gx "\n"
                     "GUI y " gy "\n")]
    (draw-centered-image context logo [gx (+ gy 230)])
    (draw-circle context [gx gy] 170 Color/WHITE)
    (draw-text context
               {:text (str "default-font\n" the-str)
                :x gx,:y gy,:h-align nil,:up? true})
    (draw-text context
               {:font special-font
                :text (str "exl-font\n" the-str)
                :x gx,:y gy,:h-align :left,:up? false})))

(defrecord Screen []
  gdl.screen/Screen
  (show [_ _context])
  (hide [_ _context])
  (render [_ context]
    (render-gui-view context draw-test))
  (tick [_ _context _delta]))

(def current-context (atom nil))

(defn create-context []
  (let [context (default-context/->context)]
    (merge context
           {:special-font (generate-ttf context {:file "exocet/films.EXL_____.ttf"
                                                 :size 16})
            :logo (create-image context "logo.png")
            :my-screen (->Screen)})))

(defn app []
  (app/start {:app {:title "gdl demo"
                    :width 800
                    :height 600
                    :full-screen? false}
              :current-context current-context
              :create-context create-context
              :first-screen :my-screen}))
