(ns gdl.simple-test
  (:require [gdl.lifecycle :as lc]
            [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.graphics.freetype :as freetype])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.Color
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defn draw-test [drawer
                 {:keys [special-font
                         gui-mouse-position
                         world-mouse-position] :as context}]
  (let [[wx wy] (map #(format "%.2f" %) world-mouse-position)
        [gx gy] gui-mouse-position
        the-str (str "World x " wx "\n"
                     "World y " wy "\n"
                     "GUI x " gx "\n"
                     "GUI y " gy "\n")]
    (draw/circle drawer gui-mouse-position 200 Color/WHITE)
    (draw/text drawer
               {:text (str "default-font\n" the-str)
                :x gx,:y gy,:h-align nil,:up? true})
    (draw/text drawer
               {:font special-font
                :text (str "exl-font\n" the-str)
                :x gx,:y gy,:h-align :left,:up? false})))

(deftype Screen []
  lc/Screen
  (lc/show [_ _ctx])
  (lc/hide [_])
  (lc/render [_ context]
    (app/render-with context
                     :gui
                     #(draw-test % context)))
  (lc/tick [_ _ctx _delta]))

(defn create-context [context]
  {:default-font (BitmapFont.) ; TODO move this default font inside gdl.app
   :special-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf") 16)
   :my-screen (->Screen)})

(defn app []
  (app/start {:app {:title "gdl demo"
                    :width 800
                    :height 600
                    :full-screen? false}
              :modules create-context
              :first-screen :my-screen}))
