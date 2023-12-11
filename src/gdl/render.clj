(ns gdl.render
  (:require [gdl.graphics.shape-drawer :as shape-drawer])
  (:import (com.badlogic.gdx.graphics Color OrthographicCamera)
           com.badlogic.gdx.graphics.g2d.Batch))

(defn render-with [^Batch batch unit-scale ^OrthographicCamera camera renderfn]
  (shape-drawer/set-line-width unit-scale)
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined camera))
  (.begin batch)
  (renderfn unit-scale)
  (.end batch)
  (shape-drawer/set-line-width 1))
