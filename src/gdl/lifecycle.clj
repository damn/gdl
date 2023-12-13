(ns gdl.lifecycle
  "Lifecycle defsystem's
  See also: https://libgdx.com/wiki/app/the-life-cycle"
  (:require [x.x :refer [defsystem]]))

; TODO consistent last arg = context/game/state like in game.entity

(defsystem create  [_ context])
(defsystem dispose [_])

; screen/.*
(defsystem show   [_ context])
(defsystem hide   [_])
(defsystem render [_ context])
(defsystem tick   [_ context delta])
