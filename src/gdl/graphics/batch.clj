(ns gdl.graphics.batch)

(defprotocol Batch
  (draw-on-viewport! [_ viewport draw-fn])
  (draw-texture-region! [_ texture-region [x y] [w h] rotation color]))
