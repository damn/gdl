(ns gdl.graphics.viewport)

(defprotocol Viewport
  (update! [_])
  (mouse-position [_]))
