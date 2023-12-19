(ns gdl.default-context
  (:require [gdl.context.assets :as assets]
            gdl.context.image-drawer-creator
            [gdl.context.shape-drawer :as shape-drawer]
            [gdl.context.text-drawer :as text-drawer]
            [gdl.context.sprite-batch :as sprite-batch]
            gdl.context.ttf-generator
            [gdl.context.gui-world-views :as gui-world-views]
            [gdl.context.vis-ui :as vis-ui]))

(defn ->Context [& {:keys [tile-size]}]
  (gdl.context/map->Context
   (let [context (sprite-batch/->context)]
     (merge context
            (vis-ui/->context)
            (assets/->context)
            (text-drawer/->context)
            (shape-drawer/->context context)
            (gui-world-views/->context :tile-size tile-size)))))
