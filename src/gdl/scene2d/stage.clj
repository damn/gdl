(ns gdl.scene2d.stage
  (:require [gdl.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Stage Group)))

(defn add-actor [^Stage stage actor]
  (.addActor stage actor))

(defn draw [^Stage stage batch]
  ; Not using (.draw ^Stage stage) because we are already managing
  ; .setProjectionMatrix / begin / end of batch and setting unit-scale in g/render-with
  ; https://github.com/libgdx/libgdx/blob/75612dae1eeddc9611ed62366858ff1d0ac7898b/gdx/src/com/badlogic/gdx/scenes/scene2d/Stage.java#L119
  ; https://github.com/libgdx/libgdx/blob/75612dae1eeddc9611ed62366858ff1d0ac7898b/gdx/src/com/badlogic/gdx/scenes/scene2d/Group.java#L56
  ; => use inside g/render-gui
  (.draw ^Group (.getRoot stage)
         batch
         (float 1)))

(defn act [^Stage stage delta]
  (.act stage delta))

(defn hit [^Stage stage [x y]]
  (.hit stage x y true))

(defn- actors [^Stage stage]
  (.getActors stage))

(defn- find-actor-with-id [stage id]
  (let [actors (actors stage)
        ids (keep actor/id actors)]
    (assert (apply distinct? ids)
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/id %))
                   actors))))

(defn create [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (find-actor-with-id this id))
      ([id not-found]
       (or (find-actor-with-id this id)
           not-found)))))
