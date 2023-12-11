(ns gdl.lc
  "https://libgdx.com/wiki/app/the-life-cycle"
  (:require [x.x :refer [defsystem]]))

(defsystem create  [_ state])
(defsystem dispose [_])

; screen/.*
(defsystem show   [_])
(defsystem hide   [_])
(defsystem render [_ state])
(defsystem tick   [_ state delta])
