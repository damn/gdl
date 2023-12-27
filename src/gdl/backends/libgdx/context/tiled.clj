(ns gdl.backends.libgdx.context.tiled
  (:require gdl.context
            [gdl.maps.tiled :as tiled])
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           [com.badlogic.gdx.maps MapRenderer MapLayer]
           com.badlogic.gdx.maps.tiled.TmxMapLoader
           [gdl OrthogonalTiledMapRendererWithColorSetter ColorSetter]))

; OrthogonalTiledMapRenderer extends BatchTiledMapRenderer
; and when a batch is passed to the constructor
; we do not need to dispose the renderer
(defn- map-renderer-for [{:keys [batch world-unit-scale]}
                         tiled-map
                         color-setter]
  (OrthogonalTiledMapRendererWithColorSetter. tiled-map
                                              (float world-unit-scale)
                                              batch
                                              (reify ColorSetter
                                                (apply [_ color x y]
                                                  (color-setter color x y)))))

(def ^:private cached-map-renderer (memoize map-renderer-for))

(extend-type gdl.context.Context
  gdl.context/TiledMapLoader
  (->tiled-map [_ file]
    (.load (TmxMapLoader.) file))

  (render-tiled-map [{:keys [world-camera] :as context} tiled-map color-setter]
    (let [^MapRenderer map-renderer (cached-map-renderer context tiled-map color-setter)]
      (.update ^OrthographicCamera world-camera)
      (.setView map-renderer world-camera)
      (->> tiled-map
           tiled/layers
           (filter #(.isVisible ^MapLayer %))
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render map-renderer)))))
