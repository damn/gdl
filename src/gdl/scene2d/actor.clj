(ns gdl.scene2d.actor
  (:refer-clojure :exclude [remove])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)))

(defn id            [^Actor actor]     (.getUserObject actor))
(defn set-id        [^Actor actor id]  (.setUserObject actor id))
(defn visible?      [^Actor actor]     (.isVisible     actor))
(defn set-invisible [^Actor actor]     (.setVisible    actor false))
(defn set-position  [^Actor actor x y] (.setPosition   actor x y))
(defn width         [^Actor actor]     (.getWidth      actor))
(defn height        [^Actor actor]     (.getHeight     actor))

(defn remove
  "Removes this actor from its parent, if it has a parent. Returns a boolean."
  [^Actor actor]
  (.remove actor))

(defn set-center [^Actor actor x y]
  (.setPosition actor
                (- x (/ (width  actor) 2))
                (- y (/ (height actor) 2))))

(defn set-touchable [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :children-only Touchable/childrenOnly
                         :disabled      Touchable/disabled
                         :enabled       Touchable/enabled)))

(defn set-opts [actor {:keys [id]}]
  (-> actor
      (set-id id))
  actor)
