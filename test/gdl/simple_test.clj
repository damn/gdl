(ns gdl.simple-test
  (:require [gdl.backends.libgdx.app :as app]
            [gdl.context :as ctx]
            [gdl.graphics :as g]
            gdl.screen
            [gdl.graphics.color :as color]))

(defn draw-test [g {:keys [special-font logo]}]
  (let [[wx wy] (map #(format "%.2f" %) (g/world-mouse-position g))
        [gx gy] (g/gui-mouse-position g)
        the-str (str "World x " wx "\n"
                     "World y " wy "\n"
                     "GUI x " gx "\n"
                     "GUI y " gy "\n")]
    (g/draw-centered-image g logo [gx (+ gy 230)])
    (g/draw-circle g [gx gy] 170 color/white)
    (g/draw-text g
                 {:text (str "default-font\n" the-str)
                  :x gx,:y gy,:h-align nil,:up? true})
    (g/draw-text g
                 {:font special-font
                  :text (str "exl-font\n" the-str)
                  :x gx,:y gy,:h-align :left,:up? false
                  :scale 2})))

(defrecord Screen []
  gdl.screen/Screen
  (show [_ _ctx])
  (hide [_ _ctx])
  (render [_ {g :context/graphics :as ctx}]
    (g/render-gui-view g #(draw-test % ctx))))

(defn create-context [ctx]
  (assoc ctx
         :special-font (ctx/generate-ttf ctx {:file "exocet/films.EXL_____.ttf"
                                              :size 16})
         :logo (ctx/create-image ctx "logo.png")
         :my-screen (->Screen)))

(defn app []
  (app/start {:app {:title "gdl demo"
                    :width 800
                    :height 600
                    :full-screen? false}
              :create-context create-context
              :first-screen :my-screen}))
