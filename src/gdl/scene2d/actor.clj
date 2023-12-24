(ns gdl.scene2d.actor)

(defprotocol Actor
  (id [_])
  (set-id! [_ id])
  (visible? [_])
  (set-visible! [_ bool])
  (toggle-visible! [_])
  (set-position! [_ x y])
  (set-center! [_ x y])
  (set-width! [_ width])
  (set-height! [_ height])
  (get-x [_])
  (get-y [_])
  (width [_])
  (height [_])
  (set-touchable! [_ touchable]
                 ":children-only, :disabled or :enabled."))
