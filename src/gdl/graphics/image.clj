(ns gdl.graphics.image
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d Batch TextureRegion)))

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

(defn- get-texture-region [assets file & [x y w h]]
  (let [^Texture texture (get assets file)]
    (if (and x y w h)
      (TextureRegion. texture (int x) (int y) (int w) (int h))
      (TextureRegion. texture))))

(defn create
  "Scale can be a number or [width height]"
  [{:keys [assets world-unit-scale]} file & {:keys [scale]}]
  (assoc-dimensions (map->Image {:file file
                                 :scale (or scale 1)
                                 :texture (get-texture-region assets file)})
                    world-unit-scale))

(defn get-scaled-copy
  "Scaled of original texture-dimensions, not any existing scale."
  [{:keys [world-unit-scale]} image scale]
  (assoc-dimensions (assoc image :scale scale)
                    world-unit-scale))

(defn get-sub-image
  "Coordinates are from original image, not scaled one."
  [{:keys [assets world-unit-scale]} {:keys [file] :as image} & sub-image-bounds]
  (assoc-dimensions (assoc image
                           :scale 1
                           :texture (apply get-texture-region assets file sub-image-bounds)
                           :sub-image-bounds sub-image-bounds)
                    world-unit-scale))

(defn spritesheet [context file tilew tileh]
  (assoc (create context file) :tilew tilew :tileh tileh))

(defn get-sprite [context {:keys [tilew tileh] :as sheet} [x y]]
  (get-sub-image context sheet (* x tilew) (* y tileh) tilew tileh))
