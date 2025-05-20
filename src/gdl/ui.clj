(ns gdl.ui
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Stage
                                            Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Cell
                                               HorizontalGroup
                                               Image
                                               Label
                                               Table
                                               Stack
                                               Tree$Node
                                               VerticalGroup
                                               Widget
                                               WidgetGroup
                                               Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  ChangeListener
                                                  Drawable
                                                  TextureRegionDrawable)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align
                                   Scaling)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Separator
                                      Tooltip
                                      VisCheckBox
                                      VisImage
                                      VisImageButton
                                      VisLabel
                                      VisSelectBox
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisTextField
                                      VisWindow)
           (gdl.ui CtxStage)))

(defn user-object [^Actor actor]
  (.getUserObject actor))

(defn set-user-object! [^Actor actor object]
  (.setUserObject actor object))

(defn visible? [^Actor actor]
  (.isVisible actor))

(defn set-visible! [^Actor actor visible?]
  (.setVisible actor visible?))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))

(defn set-touchable! [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :disabled Touchable/disabled)))

(defn remove! [^Actor actor]
  (.remove actor))

(defn parent [^Actor actor]
  (.getParent actor))

(defn- set-actor-opts! [^Actor actor {:keys [id
                                             name
                                             user-object
                                             visible?
                                             center-position
                                             position] :as opts}]
  (when id
    (set-user-object! actor id))
  (when name
    (.setName actor name))
  (when user-object
    (set-user-object! actor user-object))
  (when (contains? opts :visible?)
    (set-visible! actor visible?))
  (when-let [[x y] center-position]
    (.setPosition actor
                  (- x (/ (.getWidth  actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (when-let [[x y] position]
    (.setPosition actor x y))
  actor)

(defn- get-stage-ctx [^Actor actor]
  (when-let [stage (.getStage actor)] ; for tooltip when actors are initialized w/o stage yet
    @(.ctx ^CtxStage stage)))

(defn actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (f this delta (get-stage-ctx this))))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (f this (get-stage-ctx this)))))
    (set-actor-opts! opts)))

(defn widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (f this (get-stage-ctx this))))))

(defn find-actor [^Group group name]
  (.findActor group name))

(defprotocol CanAddActor
  (add! [_ actor]))

(extend-protocol CanAddActor
  Group
  (add! [group actor]
    (.addActor group actor))
  Stage
  (add! [stage actor]
    (.addActor stage actor))
  Table
  (add! [table actor]
    (.add table ^Actor actor)))

(defprotocol CanHit
  (hit [_ [x y]]))

(extend-protocol CanHit
  Actor
  (hit [actor [x y]]
    (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
      (.hit actor (.x v) (.y v) true)))
  Stage
  (hit [stage [x y]]
    (.hit stage x y true)))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))

(defn- find-actor-with-id [group id]
  (let [actors (children group)
        ids (keep user-object actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (user-object %)) actors))))

(defn draw! [stage]
  (Stage/.draw stage))

(defn act! [stage]
  (Stage/.act stage))

(defn root [stage]
  (Stage/.getRoot stage))

(defn stage [viewport batch actors]
  (let [stage (proxy [CtxStage ILookup] [viewport batch (atom nil)]
                (valAt [id]
                  (find-actor-with-id (root this) id)))]
    (run! (partial add! stage) actors)
    stage))

(defn load! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group, implements `clojure.lang.ILookup` (`get`)
  via [find-actor-with-id]."
  [class args]
  `(proxy [~class ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-cell-opts! [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (.add table ^Actor (:actor props-or-actor))
                                 (set-cell-opts! (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts! [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts! (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn- set-opts! [actor opts]
  (set-actor-opts! actor opts)
  (when (instance? Table actor)
    (set-table-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  (when (instance? Group actor)
    (run! #(Group/.addActor actor %) (:actors opts)))
  actor)

(defn group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(Group/.addActor group %) actors)
    (set-opts! group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad] :as opts}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (set-opts! group opts)))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(Group/.addActor group %) actors)
    group))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(def checked? VisCheckBox/.isChecked)

(defn select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(def get-selected VisSelectBox/.getSelected)

(defn table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn cells [^Table table]
  (.getCells table))

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn text-field [text opts]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(def get-text VisTextField/.getText)

(defn stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [^Texture texture]
  (VisImage. (TextureRegion. texture)))

(defmethod image* TextureRegion [^TextureRegion tr]
  (VisImage. tr))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts! opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(defn scroll-pane [actor]
  (doto (VisScrollPane. actor)
    (set-user-object! :scroll-pane)
    (.setFlickScroll false)
    (.setFadeScrollBars false)))

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (on-clicked actor))))

(defn text-button [text on-clicked]
  (doto (VisTextButton. (str text))
    (.addListener (change-listener on-clicked))))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (TextureRegionDrawable. texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (BaseDrawable/.setMinSize drawable
                                   (float (* scale w))
                                   (float (* scale h)))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn tree-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn find-ancestor-window ^Window [actor]
  (if-let [p (parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

(defn horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (parent actor)
           (button-class? (parent actor)))))

(defn window-title-bar? ; TODO buggy FIXME
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (parent actor)]
      (when-let [p (parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn add-tooltip!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (let [actor (.getTarget this)
                              ctx (get-stage-ctx actor)]
                          (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                            (.setText this (str (tooltip-text ctx))))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [^Actor actor]
  (Tooltip/removeTooltip actor))
