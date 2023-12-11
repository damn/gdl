(ns gdl.scene2d.stage
  (:require [gdl.scene2d.actor :as actor])
  (:import com.badlogic.gdx.scenes.scene2d.Stage))

(defn hit [^Stage stage [x y]]
  (.hit stage x y true))

(defn- find-actor-with-id [^Stage stage id]
  (let [actors (.getActors stage)
        ids (keep actor/id actors)]
    (assert (apply distinct? ids)
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/id %))
                   actors))))

(defn create ^Stage [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (find-actor-with-id this id))
      ([id not-found]
       (or (find-actor-with-id this id)
           not-found)))))
