(ns gdl.default-context
  (:require [gdl.context.assets :as assets]
            [gdl.context.gui-world-views :as gui-world-views]
            gdl.context.image-drawer-creator
            [gdl.context.shape-drawer :as shape-drawer]
            [gdl.context.sprite-batch :as sprite-batch]
            [gdl.context.text-drawer :as text-drawer]
            gdl.context.ttf-generator
            [gdl.context.vis-ui :as vis-ui]))

(defn ->context [& {:keys [tile-size]}]
  (let [context (sprite-batch/->context)]
    (-> context
        (merge (shape-drawer/->context context) ; requires batch
               (assets/->context)
               (gui-world-views/->context :tile-size tile-size)
               (text-drawer/->context)
               (vis-ui/->context))
        gdl.context/map->Context)))
