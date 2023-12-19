(ns gdl.context)

(defrecord Context [])

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

  ; use in gdl.app only
  (update-viewports [_ w h])
  (fix-viewport-update [_])
  (assoc-view-mouse-positions [_]))
