(ns gdl.backends.libgdx.context.image-drawer-creator
  (:require gdl.context)
  (:import (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.graphics.g2d Batch TextureRegion)))

(defn- draw-texture [^Batch batch texture [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch texture ; TODO this is texture-region ?
         x
         y
         (/ w 2) ; rotation origin
         (/ h 2)
         w ; width height
         h
         1 ; scaling factor
         1
         rotation)
  (if color (.setColor batch Color/WHITE)))

; TODO just make in image map of unit-scales to dimensions for each view
; and get by view key ?
(defn- unit-dimensions [unit-scale image]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(extend-type gdl.context.Context
  gdl.context/ImageDrawer
  (draw-image [{:keys [batch unit-scale]}
               {:keys [texture color] :as image}
               position]
    (draw-texture batch texture position (unit-dimensions unit-scale image) 0 color))

  (draw-rotated-centered-image [{:keys [batch unit-scale]} {:keys [texture color] :as image} rotation [x y]]
    (let [[w h] (unit-dimensions unit-scale image)]
      (draw-texture batch
                    texture
                    [(- x (/ w 2))
                     (- y (/ h 2))]
                    [w h]
                    rotation
                    color)))

  (draw-centered-image [this image position]
    (gdl.context/draw-rotated-centered-image this image 0 position)))

(defn- texture-dimensions [^TextureRegion texture]
  [(.getRegionWidth  texture)
   (.getRegionHeight texture)])

(defn- assoc-dimensions [{:keys [texture scale] :as image} world-unit-scale]
  {:pre [(number? world-unit-scale)
         (or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (mapv (partial * scale) (texture-dimensions texture))
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (mapv (partial * world-unit-scale) pixel-dimensions))))

; (.getTextureData (.getTexture (:texture (first (:frames (:animation @(game.db/get-entity 1)))))))
; can remove :file @ Image because its in texture-data
; only TextureRegion doesn't have toString , can implement myself ? so can see which image is being used (in case)
(defrecord Image [file ; -> is in texture data, can remove.
                  texture ; -region ?
                  sub-image-bounds ; => is in texture-region data?
                  scale
                  pixel-dimensions
                  world-unit-dimensions
                  tilew
                  tileh])
; color missing ?

(defn- get-texture-region [assets file & [x y w h]]
  (let [^Texture texture (get assets file)]
    (if (and x y w h)
      (TextureRegion. texture (int x) (int y) (int w) (int h))
      (TextureRegion. texture))))

(extend-type gdl.context.Context
  gdl.context/ImageCreator
  (create-image [{:keys [assets world-unit-scale]} file]
    (assoc-dimensions (map->Image {:file file
                                   :scale 1 ; not used anymore as arg (or scale 1) because varargs protocol methods not possible, anyway refactor images
                                   ; take only texture-region, scale,color
                                   :texture (get-texture-region assets file)})
                      world-unit-scale))

  (get-scaled-copy [{:keys [world-unit-scale]} image scale]
    (assoc-dimensions (assoc image :scale scale)
                      world-unit-scale))


  (get-sub-image [{:keys [assets world-unit-scale]}
                  {:keys [file sub-image-bounds] :as image}]
    (assoc-dimensions (assoc image
                             :scale 1
                             :texture (apply get-texture-region assets file sub-image-bounds)
                             :sub-image-bounds sub-image-bounds)
                      world-unit-scale))

  (spritesheet [context file tilew tileh]
    (assoc (gdl.context/create-image context file) :tilew tilew :tileh tileh))

  (get-sprite [context {:keys [tilew tileh] :as sheet} [x y]]
    (gdl.context/get-sub-image context
                                 (assoc sheet :sub-image-bounds [(* x tilew) (* y tileh) tilew tileh]))))
