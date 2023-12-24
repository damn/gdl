(ns gdl.scene2d.group)

(defprotocol Group
  (children [_])
  (find-actor-with-id [_ id]))
