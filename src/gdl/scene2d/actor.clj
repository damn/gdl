(ns gdl.scene2d.actor)

(defprotocol Actor
  (id [_])
  ; TODO make as opts
  (set-id! [_ id])
  (set-name! [actor name])
  (actor-name [actor])
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
  ; TODO make as opts
  (set-touchable! [_ touchable] ":children-only, :disabled or :enabled.")
  ; TODO make as opts
  (add-listener! [_ listener] "Add a listener to receive events that hit this actor.")
  (remove! [_] "Removes this actor from its parent, if it has a parent.")
  (parent [_] "Returns the parent actor, or null if not in a group."))
