(ns gdl.screen)

(defprotocol Screen
  (show   [_ context])
  (hide   [_ context])
  (render [_ context]))
