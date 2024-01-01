(ns ^:no-doc gdl.backends.libgdx.context.vis-ui
  (:require [gdl.app :refer [current-context]]
            gdl.context
            gdl.disposable
            [gdl.scene2d.actor :as actor]
            gdl.scene2d.group
            gdl.scene2d.ui.button
            gdl.scene2d.ui.button-group
            [gdl.scene2d.ui.label :refer [set-text!]]
            [gdl.scene2d.ui.table :refer [add-rows]]
            gdl.scene2d.ui.cell
            gdl.scene2d.ui.text-field
            gdl.scene2d.ui.widget-group
            gdl.scene2d.ui.window
            gdl.backends.libgdx.context.image-drawer-creator)
  (:import com.badlogic.gdx.graphics.g2d.TextureRegion
           (com.badlogic.gdx.utils Align Scaling)
           (com.badlogic.gdx.scenes.scene2d Actor Group Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Image Button Label Table Cell WidgetGroup Stack ButtonGroup HorizontalGroup Window)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener TextureRegionDrawable Drawable)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget VisTextButton VisCheckBox VisImage VisImageButton VisTextField VisWindow VisTable VisLabel VisSplitPane Tooltip)))

(defn ->context []
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load #_VisUI$SkinScale/X2) ; TODO skin-scale arg
  ; X2 everything too big .. need to change viewports for macbook ..
  {:context/vis-ui (reify gdl.disposable/Disposable
                     (dispose [_]
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

; this could do (swap! current-context on-clicked)
; & enter/hide pure & render returning a context also...
(defn- ->change-listener [_ on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (on-clicked @current-context))))

; candidate for opts: :tooltip
(defn- set-actor-opts [actor {:keys [id name visible? touchable center-position position] :as opts}]
  (when id   (actor/set-id!   actor id))
  (when name (actor/set-name! actor name))
  (when (contains? opts :visible?)  (actor/set-visible! actor visible?))
  (when touchable (actor/set-touchable! actor touchable))
  (when-let [[x y] center-position] (actor/set-center!   actor x y))
  (when-let [[x y] position]        (actor/set-position! actor x y))
  actor)

; add opts
; image-button
; group (also elements)
; stack

; TODO docstrings for this !
(defn- set-cell-opts [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg)))))

(comment
 ; fill parent & pack is from Widget TODO
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-table-opts [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts (.defaults table) cell-defaults)
  (add-rows table rows))

(defn- set-opts [actor opts]
  (set-actor-opts actor opts)
  (when (instance? Table actor)       (set-table-opts        actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor) (set-widget-group-opts actor opts))
  actor)

#_(defn- add-window-close-button [^Window window]
    (.add (.getTitleTable window)
          (text-button "x" #(.setVisible window false)))
    window)

(defn- find-actor-with-id [^Group group id]
  (let [actors (.getChildren group)
        ids (keep actor/id actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/id %))
                   actors))))

(defmulti ^:private ->vis-image type)

(defmethod ->vis-image Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod ->vis-image gdl.backends.libgdx.context.image_drawer_creator.Image
  [{:keys [^TextureRegion texture]}]
  (VisImage. texture))

(extend-type gdl.context.Context
  gdl.context/Widgets
  (->actor [_ {:keys [draw act]}]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (when draw
          (draw @current-context)))
      (act [_delta]
        (when act
          (act @current-context)))))

  (->group [_]
    (proxy [Group clojure.lang.ILookup] []
      (valAt
        ([id]
         (find-actor-with-id this id))
        ([id not-found]
         (or (find-actor-with-id this id) not-found)))))

  (->horizontal-group [_]
    (proxy [HorizontalGroup clojure.lang.ILookup] []
      (valAt
        ([id]
         (find-actor-with-id this id))
        ([id not-found]
         (or (find-actor-with-id this id) not-found)))))

  (->button-group [_ {:keys [max-check-count min-check-count]}]
    (let [button-group (ButtonGroup.)]
      (.setMaxCheckCount button-group max-check-count)
      (.setMinCheckCount button-group min-check-count)
      button-group))

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
    (let [drawable (TextureRegionDrawable. ^TextureRegion (:texture image))
          button (VisImageButton. drawable)]
      ;(.setMinSize drawable (float 96) (float 96))
      (.addListener button (->change-listener context on-clicked))
      button))

  (->table ^Table [_ opts]
    (-> (proxy [VisTable clojure.lang.ILookup] []
          (valAt
            ([id]
             (find-actor-with-id this id))
            ([id not-found]
             (or (find-actor-with-id this id) not-found))))
        (set-opts opts)))

  (->window ^Window [_ {:keys [title modal? close-button? center? close-on-escape?] :as opts}]
    (-> (let [window (doto (proxy [VisWindow clojure.lang.ILookup] [^String title true] ; true = showWindowBorder
                             (valAt
                               ([id]
                                (find-actor-with-id this id))
                               ([id not-found]
                                (or (find-actor-with-id this id) not-found))))
                       (.setModal (boolean modal?)))]
          (when close-button?    (.addCloseButton window))
          (when center?          (.centerWindow   window))
          (when close-on-escape? (.closeOnEscape  window))
          window)
        (set-opts opts)))

  (->label [_ text]
    (VisLabel. ^CharSequence text))

  (->text-field ^VisTextField [_ ^String text opts]
    (-> (VisTextField. text)
        (set-opts opts)))

  ; TODO is not decendend of SplitPane anymore => check all type hints here
  (->split-pane ^VisSplitPane [_ {:keys [^Actor first-widget
                                         ^Actor second-widget
                                         ^Boolean vertical?] :as opts}]
    (-> (VisSplitPane. first-widget second-widget vertical?)
        (set-actor-opts opts)))

  (->stack [_ actors]
    (proxy [Stack clojure.lang.ILookup] [(into-array Actor actors)]
      (valAt
        ([id]
         (find-actor-with-id this id))
        ([id not-found]
         (or (find-actor-with-id this id) not-found)))))

  ; TODO widget also make, for fill parent
  (->image-widget [_ object {:keys [scaling align fill-parent?] :as opts}]
    (-> (let [^Image image (->vis-image object)]
          (when (= :center align) (.setAlign image Align/center))
          (when (= :fill scaling) (.setScaling image Scaling/fill))
          (when fill-parent? (.setFillParent image true))
          image)
        (set-opts opts)))

  ; => maybe with VisImage not necessary anymore?
  (->texture-region-drawable ^TextureRegionDrawable [_ ^TextureRegion texture]
    (TextureRegionDrawable. texture)))

(extend-type Cell
  gdl.scene2d.ui.cell/Cell
  (set-actor! [cell actor]
    (.setActor cell actor)))

(extend-type Table
  gdl.scene2d.ui.table/Table
  (cells [table]
    (.getCells table))

  (add-rows [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (if (map? props-or-actor)
          (-> (.add table ^Actor (:actor props-or-actor))
              (set-cell-opts (dissoc props-or-actor :actor)))
          (.add table ^Actor props-or-actor)))
      (.row table))
    table)

  (add! [table actor]
    (.add table ^Actor actor))

  (add-separator! [table]
    (.addSeparator ^VisTable table)))

(extend-type Label
  gdl.scene2d.ui.label/Label
  (set-text! [^Label label ^CharSequence text]
    (.setText label text)))

(extend-type VisTextField
  gdl.scene2d.ui.text-field/TextField
  (text [text-field]
    (.getText text-field)))

(extend-type Group
  gdl.scene2d.group/Group
  (children [group]
    (seq (.getChildren group)))

  (clear-children! [group]
    (.clearChildren group))

  (find-actor-with-id [^Group group id]
    (find-actor-with-id group id))

  (add-actor! [group actor]
    (.addActor group actor)))

(extend-type Actor
  gdl.scene2d.actor/Actor
  (id [actor] (.getUserObject actor))
  (set-id! [actor id] (.setUserObject actor id))
  (set-name! [actor name] (.setName actor name))
  (name [actor] (.getName actor))
  (visible? [actor] (.isVisible actor))
  (set-visible! [actor bool] (.setVisible actor (boolean bool)))
  (toggle-visible! [actor]
    (.setVisible actor (not (.isVisible actor))))
  (set-position! [actor x y] (.setPosition actor x y))
  (set-center! [actor x y]
    (.setPosition actor
                  (- x (/ (.getWidth actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (set-width! [actor width] (.setWidth actor width))
  (set-height! [actor height] (.setHeight actor height))
  (get-x [actor] (.getY actor))
  (get-y [actor] (.getX actor))
  (width [actor] (.getWidth actor))
  (height [actor] (.getHeight actor))
  (set-touchable! [actor touchable]
    (.setTouchable actor (case touchable
                           :children-only Touchable/childrenOnly
                           :disabled      Touchable/disabled
                           :enabled       Touchable/enabled)))
  (add-listener! [actor listener]
    (.addListener actor listener))
  (remove! [actor]
    (.remove actor))
  (parent [actor]
    (.getParent actor))
  (add-tooltip! [actor tooltip-text]
    (let [label (VisLabel. "")
          tooltip (proxy [Tooltip] []
                    (draw [batch parent-alpha] ; can not use fadeIn, it is private not getting called.
                      (let [^Tooltip this this
                            text (tooltip-text @current-context)]
                        (when-not (= (str (.getText ^VisLabel (.getContent this))) text)
                          (set-text! this ^String text))
                        (proxy-super draw batch parent-alpha))))]
      (.setAlignment label Align/center)
      (.setTarget  tooltip ^Actor actor)
      (.setContent tooltip ^Actor label)))
  (remove-tooltip! [actor]
    (Tooltip/removeTooltip actor)))

(extend-type ButtonGroup
  gdl.scene2d.ui.button-group/ButtonGroup
  (clear! [button-group]
    (.clear button-group))
  (add! [button-group button]
    (.add button-group ^Button button))
  (checked [button-group]
    (.getChecked button-group))
  (remove! [button-group button]
    (.remove button-group ^Button button)))

(extend-type WidgetGroup
  gdl.scene2d.ui.widget-group/WidgetGroup
  (pack! [group]
    (.pack group)))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(extend-type Actor
  gdl.scene2d.ui.button/Actor
  (button? [actor]
    (or (button-class? actor)
        (and (actor/parent actor)
             (button-class? (actor/parent actor))))))

(extend-type Actor
  gdl.scene2d.ui.window/Actor
  (window-title-bar? [actor]
    (when (instance? Label actor)
      (when-let [prnt (actor/parent actor)]
        (when-let [prnt (actor/parent prnt)]
          (and (instance? VisWindow prnt)
               (= (.getTitleLabel ^Window prnt) actor)))))))
