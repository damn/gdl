(ns gdl.simple-test
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.app :as app]
            [gdl.files :as files]
            [gdl.graphics.world :as world]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.font :as font]
            [gdl.graphics.freetype :as freetype]))

(defn- gen-font []
  (freetype/generate (files/internal "exocet/films.EXL_____.ttf")
                     16))

(defn render-mouse-coordinates [font]
  (let [[wx wy] (map #(format "%.2f" %) (world/mouse-position))
        [gx gy] (gui/mouse-position)
        the-str (str "World x " wx "\n"
                     "World y " wy "\n"
                     "GUI x " gx "\n"
                     "GUI y " gy "\n")]
    (font/draw-text {:font nil
                     :text (str "default-font\n" the-str)
                     :x gx,:y gy,:h-align nil,:up? true})
    (font/draw-text {:font font
                     :text (str "exl-font\n" the-str)
                     :x gx,:y gy,:h-align :left,:up? false})))

(defmodule
  font
  (lc/create [_] (gen-font))
  (lc/dispose [_] (.dispose font))
  (lc/render [_]
    (gui/render #(render-mouse-coordinates font))))

(defn app []
  (app/start {:window {:title "gdl demo"
                       :width 800
                       :height 600
                       :full-screen false}
              :log-lc? true
              :modules [[:gdl.simple-test]]
              :first-screen :gdl.simple-test}))
