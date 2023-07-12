(ns gdx.ui
  (:require [gdx.utils :refer (set-var-root)]
            [gdx.app :as app]
            [gdx.files :as files]
            [gdx.graphics :as g])
  (:import [com.badlogic.gdx.scenes.scene2d Stage Actor]
           [com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable]
           [com.badlogic.gdx.scenes.scene2d.ui Table Skin
            TextButton TextButton$TextButtonStyle
            CheckBox CheckBox$CheckBoxStyle
            Window Window$WindowStyle
            ImageButton ImageButton$ImageButtonStyle
            Button Button$ButtonStyle
            Label Label$LabelStyle
            TooltipManager
            Tooltip TextTooltip TextTooltip$TextTooltipStyle]))

; separate scene2d (stage,actor ) & ui ?

; TODO stage [viewport batch]
(defn create-stage []
  (Stage. g/gui-viewport g/sprite-batch))

(defn draw-stage [stage]
  (g/with-gui-bindings
    ; https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/scenes/scene2d/Stage.java#L119
    ; set line width (here outside of render-gui-level)
    (.draw ^Stage stage)))

(defn update-stage [stage delta]
  (.act ^Stage stage delta))

(declare ^Skin skin)

(app/on-create
 ; TODO is this not included in libgdx?
 (set-var-root #'skin (Skin. (files/internal "scene2d.ui.skin/uiskin.json"))))

(app/on-destroy
 (.dispose skin))

(defn table []
  (Table.))

; TODO Button implements disposable??
; TODO how to check any objects disposable interface and I didnt dispose?

; TODO do I have to pass .get skin class this or can i pass directly skin?? try.

(defn text-button [text on-clicked]
  (let [button (TextButton. ^String text ^TextButton$TextButtonStyle (.get skin TextButton$TextButtonStyle))]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

(defn check-box [text on-clicked checked?]
  (let [^Button button (CheckBox. text (.get skin CheckBox$CheckBoxStyle))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

; TODO 'toggle' - imagebutton , :toggle true ?
(defn image-button [{:keys [texture]} on-clicked]
  (let [style (ImageButton$ImageButtonStyle. (.get skin "toggle" Button$ButtonStyle))
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
(defn window [title]
  (Window. title (.get skin Window$WindowStyle)))

(defn label [text]
  (Label. ^CharSequence text (.get skin Label$LabelStyle)))

(defn actor [actfn]
  (proxy [Actor] []
    (act [delta]
      (actfn))))

; TODO the tooltip manager sets my spritebatch color to 0.2 alpha for short time !!
; TODO also the widget where the tooltip is attached is flickering after
; the tooltip disappears
; => write your own manager without animations/time
(defn- instant-show-tooltip-manager [textfn]
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

(defn text-tooltip [textfn]
  (TextTooltip. ""
                (instant-show-tooltip-manager textfn)
                (.get skin TextTooltip$TextTooltipStyle)))