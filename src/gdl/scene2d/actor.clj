(ns gdl.scene2d.actor
  "Helper functions for com.badlogic.gdx.scenes.scene2d.Actor
  'id' is defined by using .getUserObject and .setUserObject in Actor class."
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn id [^Actor actor]
  (.getUserObject actor))

(defn set-id [^Actor actor id]
  (.setUserObject actor id))

(defn set-center [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn set-touchable [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :children-only Touchable/childrenOnly
                         :disabled      Touchable/disabled
                         :enabled       Touchable/enabled)))

(defn set-opts [actor {:keys [id]}]
  (-> actor
      (set-id id))
  actor)
