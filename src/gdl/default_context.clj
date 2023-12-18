(ns gdl.default-context
  (:require [gdl.screen :as screen]
            [gdl.protocols :refer [dispose]]
            gdl.context.assets
            gdl.context.image-drawer-creator
            gdl.context.shape-drawer
            gdl.context.text-drawer
            gdl.context.ttf-generator
            gdl.context.gui-world-views
            gdl.scene2d.ui)
  (:import com.badlogic.gdx.graphics.g2d.SpriteBatch))

(defn ->Context [& {:keys [tile-size]}]
  (gdl.protocols/map->Context
   (let [batch (SpriteBatch.)]
     (merge {:batch batch
             :context/scene2d.ui (gdl.scene2d.ui/initialize!)}
            (gdl.context.assets/->context-map)
            (gdl.context.text-drawer/->context-map)
            (gdl.context.shape-drawer/->context-map batch)
            (gdl.context.gui-world-views/->context-map :tile-size (or tile-size 1))))))
