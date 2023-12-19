(ns gdl.context.vis-ui
  (:require gdl.disposable
            gdl.scene2d.ui)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.ui.Skin
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)))

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
