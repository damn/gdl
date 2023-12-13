(ns gdl.scene2d.ui
  "Widget constructors and helper functions for com.kotcrab.vis.ui
  See: https://github.com/kotcrab/vis-ui"
  (:require [x.x :refer [defmodule]]
            [gdl.lifecycle :as lc]
            [gdl.scene2d.actor :as actor])
  (:import com.badlogic.gdx.files.FileHandle
           com.badlogic.gdx.graphics.g2d.TextureRegion
           com.badlogic.gdx.scenes.scene2d.Actor
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Table Skin WidgetGroup TextButton CheckBox Window Button
            Label TooltipManager Tooltip TextTooltip TextField SplitPane Stack Image)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget VisTextField VisTable VisTextButton VisImageButton VisWindow VisLabel VisSplitPane VisCheckBox)))

(declare ^Skin default-skin)

(defmodule user-skin
  (lc/create [_ _ctx]
    (.bindRoot #'default-skin user-skin)
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (VisUI/isLoaded)
      (VisUI/dispose))
    (VisUI/load #_VisUI$SkinScale/X2))
  (lc/dispose [_]
    (.dispose default-skin)
    (VisUI/dispose)))

(defn skin [^FileHandle file]
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
      :pad        (.pad       cell (float arg))
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

(defn set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?))
  (when pack?
    (.pack widget-group))
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
  (-> (VisTable.)
      (set-opts opts)))

(defn text-button ^TextButton [text on-clicked]
  (let [button (VisTextButton. ^String text)]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

(defn check-box ^CheckBox [text on-clicked checked?]
  (let [^Button button (VisCheckBox. ^String text)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

; TODO give directly texture-region
; TODO check how to make toggle-able ? with hotkeys for actionbar trigger ?
(defn image-button ^VisImageButton [image on-clicked]
  (let [button (VisImageButton. (TextureRegionDrawable. ^TextureRegion (:texture image)))]
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event actor]
                      (on-clicked))))
    button))

(defn- add-window-close-button [^Window window]
  (.add (.getTitleTable window)
        (text-button "x" #(.setVisible window false)))
  window)

(defn window ^Window [& {:keys [title modal?] :as opts}]
  (-> (doto (VisWindow. ^String title)
        (.setModal (boolean modal?)))
      (set-opts opts)
      add-window-close-button))

(defn label ^Label [text]
  (VisLabel. ^CharSequence text))

(defn set-text [^Label label ^CharSequence text]
  (.setText label text))

(defn text-field ^VisTextField [^String text & opts]
  (-> (VisTextField. text)
      (set-opts opts)))

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

; TODO VisToolTip
; https://github.com/kotcrab/vis-ui/wiki/Tooltips
(defn text-tooltip ^TextTooltip [textfn]
  (TextTooltip. "" (instant-show-tooltip-manager textfn) default-skin))

; TODO is not decendend of SplitPane anymore => check all type hints here
(defn split-pane ^VisSplitPane [& {:keys [^Actor first-widget
                                          ^Actor second-widget
                                          ^Boolean vertical?] :as opts}]
  (-> (VisSplitPane. first-widget second-widget vertical?)
      (actor/set-opts opts)))

(defn stack ^Stack []
  (Stack.))

; TODO VisImage, check other widgets too replacements ?
(defn image ^Image [^Drawable drawable & opts]
  (-> (Image. drawable)
      (set-opts opts)))

(defn texture-region-drawable ^TextureRegionDrawable [^TextureRegion texture]
  (TextureRegionDrawable. texture))
