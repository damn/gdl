(ns gdl.backends.libgdx.context.stage
  (:require [gdl.context :refer [gui-mouse-position current-screen get-stage delta-time]]
            gdl.disposable
            [gdl.screen :as screen]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui :refer [find-actor-with-id]])
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.scenes.scene2d.Stage))

(defrecord StageScreen [^Stage stage sub-screen]
  gdl.disposable/Disposable
  (dispose [_]
    (.dispose stage))

  gdl.screen/Screen
  (show [_ context]
    (.setInputProcessor Gdx/input stage)
    (when sub-screen (screen/show sub-screen context)))

  (hide [_ context]
    (.setInputProcessor Gdx/input nil)
    (when sub-screen (screen/hide sub-screen context)))

  (render [_ context]
    (when sub-screen (screen/render sub-screen context))
    (.draw stage)
    (.act stage (delta-time context))))

(extend-type gdl.context.Context
  gdl.context/Stage
  (->stage-screen [{:keys [gui-viewport batch]} {:keys [actors sub-screen]}]
    (let [stage (proxy [Stage clojure.lang.ILookup] [gui-viewport batch]
                  (valAt
                    ([id]
                     (find-actor-with-id (.getRoot ^Stage this) id))
                    ([id not-found]
                     (or (find-actor-with-id (.getRoot ^Stage this) id)
                         not-found))))]
      (doseq [actor actors]
        (.addActor stage actor))
      (->StageScreen stage sub-screen)))
  (get-stage [context]
    (:stage (current-screen context)))
  (mouse-on-stage-actor? [context]
    (let [[x y] (gui-mouse-position context)]
      (.hit ^Stage (get-stage context) x y true))))
