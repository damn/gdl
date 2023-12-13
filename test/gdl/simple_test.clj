(ns gdl.simple-test
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.graphics.font :as font]
            [gdl.graphics.freetype :as freetype])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defmodule {:keys [special-font default-font]}
  (lc/create [_ _ctx]
    {:special-font (freetype/generate (.internal Gdx/files "exocet/films.EXL_____.ttf")
                                      16)
     :default-font (BitmapFont.)})
  (lc/dispose [_]
    (.dispose ^BitmapFont special-font)
    (.dispose ^BitmapFont default-font))
  (lc/render [_ {:keys [gui-mouse-position world-mouse-position] :as context}]
    (app/render-with (assoc context :default-font default-font)
                     :gui
                     (fn [context]
                       (let [[wx wy] (map #(format "%.2f" %) world-mouse-position)
                             [gx gy] gui-mouse-position
                             the-str (str "World x " wx "\n"
                                          "World y " wy "\n"
                                          "GUI x " gx "\n"
                                          "GUI y " gy "\n")]
                         (font/draw-text context
                                         {:text (str "default-font\n" the-str)
                                          :x gx,:y gy,:h-align nil,:up? true})
                         (font/draw-text context
                                         {:font special-font
                                          :text (str "exl-font\n" the-str)
                                          :x gx,:y gy,:h-align :left,:up? false}))))))

(defn app []
  (app/start {:app {:title "gdl demo"
                    :width 800
                    :height 600
                    :full-screen? false}
              :modules {:gdl.simple-test nil}
              :first-screen :gdl.simple-test}))
