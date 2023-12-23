(ns gdl.backends.libgdx.context.vis-ui
  (:require gdl.context
            gdl.disposable
            gdl.scene2d.ui)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.graphics.g2d.TextureRegion
           (com.badlogic.gdx.scenes.scene2d.ui Skin Button)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget VisTextButton VisCheckBox VisImageButton)))

(defn ->context [] ; TODO skin-scale arg
  ; this is the gdx default skin  - copied from libgdx project, check not included in libgdx jar somewhere?
  (.bindRoot #'gdl.scene2d.ui/default-skin (Skin. (.internal Gdx/files "scene2d.ui.skin/uiskin.json")))
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load #_VisUI$SkinScale/X2)
  {:context/vis-ui (reify gdl.disposable/Disposable
                     (dispose [_]
                       (.dispose ^Skin gdl.scene2d.ui/default-skin)
                       (VisUI/dispose)))})

(comment
 ; TODO set custom font with default skin - or set custom skin param
 ; https://stackoverflow.com/questions/45523878/libgdx-skin-not-updating-when-changing-font-programmatically
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
   ))

; TODO add Actor/Widget, also using current-context & tooltips

(defn- ->change-listener [{:keys [gdl.app/current-context]} on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor] ; TODO pass also event / actor as event/event event/actor or something
      (on-clicked @current-context))))
; TODO this could do (swap! current-context on-clicked) ??
; => all change-screens could be done pure functions o.o / hide / enter 'pure' functions

(extend-type gdl.context.Context
  gdl.context/Widgets
  ; ^TextButton
  (->text-button [context text on-clicked]
    (let [button (VisTextButton. ^String text)]
      (.addListener button (->change-listener context on-clicked))
      button))

  ; ^CheckBox
  (->check-box [context text on-clicked checked?]
    (let [^Button button (VisCheckBox. ^String text)]
      (.setChecked button checked?)
      (.addListener button
                    (proxy [ChangeListener] []
                      (changed [event ^Button actor]
                        (on-clicked (.isChecked actor)))))
      button))

  ; TODO give directly texture-region
  ; TODO check how to make toggle-able ? with hotkeys for actionbar trigger ?
  ; ^VisImageButton
  (->image-button [context image on-clicked]
    (let [button (VisImageButton. (TextureRegionDrawable. ^TextureRegion (:texture image)))]
      (.addListener button (->change-listener context on-clicked))
      button)))
