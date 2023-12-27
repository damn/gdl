(ns gdl.context)

(defrecord Context [])

(defprotocol Application
  (exit-app [_]))

(defprotocol ApplicationScreens
  (current-screen [_])
  (change-screen [_ new-screen]
                 "Calls screen/hide on the current-screen (if there is one).
                 Throws AssertionError when the context does not have a new-screen.
                 Calls screen/show on the new screen and
                 returns the context with current-screen set to new-screen."))

(defprotocol Graphics
  (delta-time [_]
              "the time span between the current frame and the last frame in seconds.")
  (frames-per-second [_]
                     "the average number of frames per second")
  (->cursor [_ file hotspot-x hotspot-y]
            "Takes care of disposing the cursor at application exit.")
  (set-cursor! [_ cursor])
  (->color [_ r g b a]))

(defprotocol Input
  (button-pressed?      [_ button])
  (button-just-pressed? [_ button])
  (key-pressed?      [_ k])
  (key-just-pressed? [_ k]))

(defprotocol TrueTypeFontGenerator
  (generate-ttf [_ {:keys [file size]}]))

(defprotocol TextDrawer
  (draw-text [_ {:keys [x y text font h-align up? scale]}]
             "font, h-align, up? and scale are optional.
             h-align one of: :center, :left, :right. Default :center.
             up? renders the font over y, otherwise under.
             scale will multiply the drawn text size with the scale."))

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
                  "A screen with a stage as an input-processor which gets drawn and 'act'ed after the given sub-screen.
                  The stage will get disposed also.
                  Sub-screen is optional.")
  (get-stage [_] "Stage implements clojure.lang.ILookup (get) on actor id.")
  (mouse-on-stage-actor? [_]))

; TODO
; actor-opts
; table-opts
; widget-group-opts
(defprotocol Widgets
  (->actor [_ {:keys [draw act]}])
  (->group [_] "Implements clojure.lang.ILookup (get) on actor id.")
  (->text-button [_ text on-clicked])
  (->check-box [_ text on-clicked checked?])
  (->image-button [_ image on-clicked])
  (->text-tooltip [_ textfn])
  (->table [_ opts] ":rows like gdl.scene2d.ui.table/add-rows.
Extra opts: :modal?

Implements clojure.lang.ILookup (get) on actor id.

https://javadoc.io/static/com.badlogicgames.gdx/gdx/1.12.1/com/badlogic/gdx/scenes/scene2d/ui/Table.html
A group that sizes and positions children using table constraints.

Children added with add(Actor...) (and similar methods returning a Cell) are laid out in rows and columns. Other children may be added with Group.addActor(Actor) (and similar methods) but are not laid out automatically and don't affect the preferred or minimum sizes.

By default, Actor.getTouchable() is Touchable.childrenOnly.

The preferred and minimum sizes are that of the children laid out in columns and rows.")
  (->window [_ {:keys [title modal?] :as opts}])
  (->label [_ text])
  (->text-field [_ text opts])
  (->split-pane [_ {:keys [first-widget
                           second-widget
                           vertical?] :as opts}])
  (->stack [_ actors]"A stack is a container that sizes its children to its size and positions them at 0,0 on top of each other.

The preferred and min size of the stack is the largest preferred and min size of any children. The max size of the stack is the smallest max size of any children.

Implements clojure.lang.ILookup (get) on actor id.

https://javadoc.io/static/com.badlogicgames.gdx/gdx/1.12.1/com/badlogic/gdx/scenes/scene2d/ui/Table.html
A group that sizes and positions children using table constraints.

Children added with add(Actor...) (and similar methods returning a Cell) are laid out in rows and columns. Other children may be added with Group.addActor(Actor) (and similar methods) but are not laid out automatically and don't affect the preferred or minimum sizes.

By default, Actor.getTouchable() is Touchable.childrenOnly.

The preferred and minimum sizes are that of the children laid out in columns and rows. ")
  (->image-widget [_ object opts] "Takes either an image or drawable.")
  (->texture-region-drawable [_ texture-region])
  (->horizontal-group [_] "Implements clojure.lang.ILookup (get) on actor id.")
  (->button-group [_ {:keys [max-check-count min-check-count]}]))

(defprotocol TiledMapLoader
  (->tiled-map [_ file] "Needs to be disposed.")
  (render-tiled-map [_ tiled-map color-setter]
                    "Renders tiled-map using world-view at world-camera position and with world-unit-scale.
                    Color-setter is a gdl.ColorSetter which is called for every tile-corner to set the color.
                    Can be used for lights & shadows.
                    The map-renderers are created and cached internally.
                    Renders only visible layers."))
