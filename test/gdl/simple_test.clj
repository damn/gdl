(ns gdl.simple-test
  (:require [x.x :refer [defcomponent]]
            [gdl.lifecycle :as lc]
            [gdl.app :as app]
            [gdl.graphics.draw :as draw]
            [gdl.graphics.freetype :as freetype])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.Color
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defcomponent :default-font font
  (lc/create [_ _ctx]
    (BitmapFont.))
  (lc/dispose [_]
    (.dispose ^BitmapFont font)))

(defcomponent :screen {:keys [special-font default-font]}
  (lc/create [_ _ctx]
    {:special-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf")
                                      16)})
  (lc/dispose [_]
    (.dispose ^BitmapFont special-font))
  (lc/render [_ {:keys [gui-mouse-position world-mouse-position] :as context}]
    (app/render-with context
                     :gui
                     (fn [drawer]
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
                                     :x gx,:y gy,:h-align :left,:up? false}))))))

(defn app []
  (app/start {:app {:title "gdl demo"
                    :width 800
                    :height 600
                    :full-screen? false}
              :modules {:default-font nil
                        :screen nil}
              :first-screen :screen}))
