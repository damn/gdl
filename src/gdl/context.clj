(ns gdl.context)

(defrecord Context [])

(defprotocol ApplicationScreens
  (current-screen [_])
  (change-screen [_ new-screen]
                 "Calls screen/hide on the current-screen (if there is one).
                 Throws AssertionError when the context does not have a new-screen.
                 Calls screen/show on the new screen and
                 returns the context with current-screen set to new-screen."))

(defprotocol TrueTypeFontGenerator
  (generate-ttf [_ {:keys [file size]}]))

(defprotocol TextDrawer
  (draw-text [_ {:keys [font text x y h-align up?]}]))

(defprotocol ShapeDrawer
  (draw-ellipse [_ position radius-x radius-y color])
  (draw-filled-ellipse [_ position radius-x radius-y color])
  (draw-circle [_ position radius color])
  (draw-filled-circle [_ position radius color])
  (draw-arc [_ center-position radius start-angle degree color])
  (draw-sector [_ center-position radius start-angle degree color])
  (draw-rectangle [_ x y w h color])
  (draw-filled-rectangle [_ x y w h color])
  (draw-line [_ start-position end-position color])
  (draw-grid [drawer leftx bottomy gridw gridh cellw cellh color])
  (with-shape-line-width [_ width draw-fn]))

(defprotocol ImageDrawer
  (draw-image [_ image position])
  (draw-centered-image [_ image position])
  (draw-rotated-centered-image [_ image rotation position]))

(defprotocol ImageCreator
  (create-image [_ file])
  (get-scaled-copy [_ image scale]
                   "Scaled of original texture-dimensions, not any existing scale.")
  (get-sub-image [_ {:keys [file sub-image-bounds] :as image}]
                 "Coordinates are from original image, not scaled one.")
  (spritesheet [_ file tilew tileh])
  (get-sprite [_ {:keys [tilew tileh] :as sheet} [x y]]))

(defprotocol GuiWorldViews
  (render-gui-view   [_ render-fn])
  (render-world-view [_ render-fn])
  (pixels->world-units [_ pixels])
  (gui-mouse-position [_])
  (world-mouse-position [_]))

(defprotocol SoundStore
  (play-sound! [_ file]
               "Sound is already loaded from file, this will perform only a lookup for the sound and play it." ))

(defprotocol Stage
  (->stage-screen [_ {:keys [stage sub-screen]}]
                  "A screen with a stage as an input-processor which gets drawn and 'act'ed and disposed.
                  The sub-screen is rendered and tick'ed before the stage.
                  Sub-screen is optional.")
  (get-stage [_])
  (mouse-on-stage-actor? [_]))

(defprotocol Widgets
  (->text-button [_ text on-clicked])
  (->check-box [_ text on-clicked checked?])
  (->image-button [_ image on-clicked]))
