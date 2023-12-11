(ns gdl.graphics.font
  (:require [clojure.string :as str])
  (:import com.badlogic.gdx.utils.Align
           com.badlogic.gdx.graphics.g2d.BitmapFont))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw-text [{:keys [default-font unit-scale batch]} {:keys [font text x y h-align up?]}]
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (.scaleX data)]
    (.setScale data (float (* old-scale unit-scale)))
    (.draw font
           batch
           (str text)
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float 0) ; target-width
           (case (or h-align :center)
             :center Align/center
             :left   Align/left
             :right  Align/right)
           false) ; wrap false, no need target-width
    (.setScale data (float old-scale))))
