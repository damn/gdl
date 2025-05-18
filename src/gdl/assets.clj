(ns gdl.assets
  (:import (clojure.lang IFn)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn- asset-type->class ^Class [asset-type]
  (case asset-type
    :sound Sound
    :texture Texture))

(defn create [assets]
  (let [manager (proxy [AssetManager IFn] []
                  (invoke [path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this ^String path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[file asset-type] assets]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))

(defn all-of-type [^AssetManager assets asset-type]
  (filter #(= (.getAssetType assets %) (asset-type->class asset-type))
          (.getAssetNames assets)))
