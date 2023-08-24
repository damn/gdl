(ns gdl.ui
  (:refer-clojure :exclude [name])
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.files :as files]
            [gdl.graphics :as g]
            [gdl.graphics.batch :refer [batch]]
            [gdl.graphics.gui :as gui])
  (:import com.badlogic.gdx.graphics.g2d.TextureRegion
           [com.badlogic.gdx.scenes.scene2d Stage Actor Group]
           [com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable]
           [com.badlogic.gdx.scenes.scene2d.ui Table Skin TextButton CheckBox Window Button
            Button$ButtonStyle ImageButton ImageButton$ImageButtonStyle Label TooltipManager Tooltip
            TextTooltip TextField SplitPane Stack Image]))

(comment
 ; actor opts:
 ; .setName
 ; .setTouchable
 )

(defn stage ^Stage []
  (Stage. gui/viewport batch))

(defn draw-stage [stage]
  ; Not using (.draw ^Stage stage) because we are already managing
  ; .setProjectionMatrix / begin / end of batch and setting unit-scale in g/render-with
  ; https://github.com/libgdx/libgdx/blob/75612dae1eeddc9611ed62366858ff1d0ac7898b/gdx/src/com/badlogic/gdx/scenes/scene2d/Stage.java#L119
  ; https://github.com/libgdx/libgdx/blob/75612dae1eeddc9611ed62366858ff1d0ac7898b/gdx/src/com/badlogic/gdx/scenes/scene2d/Group.java#L56
  ; => use inside g/render-gui
  (.draw ^Group (.getRoot ^Stage stage)
         batch
         (float 1)))

(defn update-stage [stage delta]
  (.act ^Stage stage delta))

(declare ^Skin skin)

; TODO default skin not included in libgdx jar? check.
(defmodule _
  (lc/create [_]
    (.bindRoot #'skin (Skin. (files/internal "scene2d.ui.skin/uiskin.json"))))
  (lc/dispose [_]
    (.dispose skin)))

; custom skin code for using custom font.
(comment
 (declare ^:dispose ^Skin skin)

 (defn create-skin
   ([]
    (create-skin default-skin))
   ([skin]
    (set-var-root #'skin skin)))

 ; TODO but how to arrange it that
 ; it will be created before other on-create??
 ; by leaving it declared only yes
 ; ...



 ; add my font to skin
 ; -> need to do before any things get created with the skin
 ; for example game/items/inventory on-create ... ui/window ...
 ; => how to set the skin before anything gets loaded ?

 (let [empty-skin (Skin.)]
   (.add skin "font" my-font)
   ; skin.addRegion(new TextureAtlas(Gdx.files.internal("mySkin.atlas")));
   ; skin.load(Gdx.files.internal("mySkin.json"));
   ; TODO will this overload 'default-font' ?
   ; => I need to pass my custom skin to gdl.ui !
   ; then, in your JSON file you can reference “font”
   ;
   ; {
   ;   font: font
   ; }

   )
 )

; https://stackoverflow.com/questions/45523878/libgdx-skin-not-updating-when-changing-font-programmatically

(defn table ^Table []
  (Table.))

(defn text-button ^TextButton [text on-clicked]
  (let [button (TextButton. ^String text skin)]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

(defn check-box ^CheckBox [text on-clicked checked?]
  (let [^Button button (CheckBox. ^String text skin)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

; TODO 'toggle' - imagebutton , :toggle true ?
(defn image-button ^ImageButton [{:keys [^TextureRegion texture] :as image} on-clicked]
  (let [style (ImageButton$ImageButtonStyle. ^Button$ButtonStyle (.get skin "toggle" Button$ButtonStyle))
        _ (set! (.imageUp   style) (TextureRegionDrawable. texture))
        _ (set! (.imageDown style) (TextureRegionDrawable. texture))
        ; imageChecked
        ; imageCheckedDown
       ; imageCheckedOver
        ; imageDisabled
        ; imageDown
        ; imageOver
        ; imageUp
        button (ImageButton. style)]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

; https://stackoverflow.com/questions/29771114/how-can-i-add-button-to-top-right-corner-of-a-dialog-in-libgdx
; window with close button
(defn window
  "A table that can be dragged and act as a modal window. The top padding is used as the window's title height.

The preferred size of a window is the preferred size of the title text and the children as laid out by the table. After adding children to the window, it can be convenient to call WidgetGroup.pack() to size the window to the size of the children.

  See https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/scenes/scene2d/ui/Window.html

  Options: :title, :modal?"
  ^Window [& {:keys [title modal?]}]
  (doto (Window. ^String title skin)
    (.setModal (boolean modal?))))

(defn label ^Label [text]
  (Label. ^CharSequence text skin))

(defn text-field ^TextField [^String text]
  (TextField. text skin))

(defn actor ^Actor [actfn]
  (proxy [Actor] []
    (act [delta]
      (actfn))))

; TODO the tooltip manager sets my spritebatch color to 0.2 alpha for short time
; TODO also the widget where the tooltip is attached is flickering after
; the tooltip disappears
; => write your own manager without animations/time
(defn- instant-show-tooltip-manager ^TooltipManager [textfn]
  (let [manager (proxy [TooltipManager] []
                  (enter [^Tooltip tooltip]
                    (.setText ^Label (.getActor tooltip) ^String (textfn))
                    (.pack (.getContainer tooltip))
                    (let [^TooltipManager this this]
                      (proxy-super enter tooltip))))]
    (set! (.initialTime manager) 0)
    (set! (.resetTime   manager) 0)
    (set! (.animations  manager) false)
    (.hideAll manager)
    manager))

(defn text-tooltip ^TextTooltip [textfn]
  (TextTooltip. "" (instant-show-tooltip-manager textfn) skin))

(defn split-pane ^SplitPane [^Actor first-widget ^Actor second-widget ^Boolean vertical?]
  (SplitPane. first-widget second-widget vertical? skin))

(defn stack ^Stack []
  (Stack.))

(defn mouseover? [^Stage stage]
  (let [[x y] (gui/mouse-position)]
    (.hit stage x y true)))

(defn visible? [^Actor actor] (.isVisible actor))
(defn name     [^Actor actor] (.getName   actor))

(defn set-position [^Actor actor x y]
  (.setPosition actor x y))

(defn image ^Image [^Drawable drawable]
  (Image. drawable))
