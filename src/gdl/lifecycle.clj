(ns gdl.lifecycle)

(defprotocol Disposable
  (dispose [_]))

; TODO consistent last arg = context/game/state like in game.entity
(defprotocol Screen
  (show   [_ context])
  (hide   [_])
  (render [_ context])
  (tick   [_ context delta]))
