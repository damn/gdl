(ns gdl.scene2d.ui
  (:require [x.x :refer [defmodule]]
            [gdl.lc :as lc]
            [gdl.scene2d.actor :as actor])
  (:import com.badlogic.gdx.graphics.g2d.TextureRegion
           (com.badlogic.gdx.scenes.scene2d Stage Actor Group)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Table Skin WidgetGroup TextButton CheckBox Window Button
            Button$ButtonStyle ImageButton ImageButton$ImageButtonStyle Label TooltipManager Tooltip
            TextTooltip TextField SplitPane Stack Image)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)))

(defn actors [^Stage stage] ; TODO make stage seq-able.
  (.getActors stage))

(defn- find-actor-with-id [stage id]
  (let [actors (actors stage)
        ids (keep actor/id actors)]
    (assert (apply distinct? ids)
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/id %))
                   actors))))

(defn stage ^Stage [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (find-actor-with-id this id))
      ([id not-found]
       (or (find-actor-with-id this id)
           not-found)))))

(defn draw-stage [stage batch]
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

(defn mouseover? [^Stage stage [x y]]
  (.hit stage x y true))

(declare ^Skin default-skin)

(defmodule user-skin
  (lc/create [_]
    (.bindRoot #'default-skin user-skin)
    (when-not (VisUI/isLoaded) ; app has error before VisUI/dispose and we call refresh-all
      (VisUI/load #_VisUI$SkinScale/X2)))
  (lc/dispose [_]
    (.dispose default-skin)
    (VisUI/dispose)))

(defn skin [file]
  (Skin. file))

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

(defn set-cell-opts [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :expand?    (.expand    cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int arg))
      :pad-bottom (.padBottom cell (float arg)))) )

(defn add-rows [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (if (map? props-or-actor)
        (-> (.add table ^Actor (:actor props-or-actor))
            (set-cell-opts (dissoc props-or-actor :actor)))
        (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn pack [^WidgetGroup widget-group]
  (.pack widget-group))

(defn set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?))
  (when pack? (pack widget-group))
  widget-group)

(defn set-table-opts [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts (.defaults table) cell-defaults)
  (add-rows table rows))

(defn set-opts [actor opts]
  (actor/set-opts actor opts)
  (when (instance? Table actor)       (set-table-opts        actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor) (set-widget-group-opts actor opts))
  actor)

(defn table ^Table [& opts]
  (-> (Table.)
      (set-opts opts)))

(defn text-button ^TextButton [text on-clicked]
  (let [button (TextButton. ^String text default-skin)]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

(defn check-box ^CheckBox [text on-clicked checked?]
  (let [^Button button (CheckBox. ^String text default-skin)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

; TODO 'toggle' - imagebutton , :toggle true ?
(defn image-button ^ImageButton [{:keys [^TextureRegion texture] :as image} on-clicked]
  (let [style (ImageButton$ImageButtonStyle. ^Button$ButtonStyle (.get default-skin "toggle" Button$ButtonStyle))
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
(defn window ^Window [& {:keys [title modal?] :as opts}]
  (-> (doto (Window. ^String title default-skin)
        (.setModal (boolean modal?)))
      (set-opts opts)))

(defn label ^Label [text]
  (Label. ^CharSequence text default-skin))

(defn text-field ^TextField [^String text]
  (TextField. text default-skin))

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
  (TextTooltip. "" (instant-show-tooltip-manager textfn) default-skin))

(defn split-pane ^SplitPane [& {:keys [^Actor first-widget
                                       ^Actor second-widget
                                       ^Boolean vertical?] :as opts}]
  (-> (SplitPane. first-widget second-widget vertical? default-skin)
      (actor/set-opts opts)))

(defn stack ^Stack []
  (Stack.))

(defn image ^Image [^Drawable drawable]
  (Image. drawable))
