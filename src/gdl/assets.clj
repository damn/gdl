(ns gdl.assets
  (:require [clojure.string :as str])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

(defn- asset-type->class ^Class [asset-type]
  (case asset-type
    :sound Sound
    :texture Texture))

(defn create [{:keys [folder
                      asset-type-extensions]}]
  (let [manager (proxy [AssetManager IFn] []
                  (invoke [path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this ^String path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[file asset-type] (for [[asset-type extensions] asset-type-extensions
                                    file (map #(str/replace-first % folder "")
                                              (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
                                                     result []]
                                                (cond (nil? file)
                                                      result

                                                      (.isDirectory file)
                                                      (recur (concat remaining (.list file)) result)

                                                      (extensions (.extension file))
                                                      (recur remaining (conj result (.path file)))

                                                      :else
                                                      (recur remaining result))))]
                                [file asset-type])]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))

(defn all-of-type [^AssetManager assets asset-type]
  (filter #(= (.getAssetType assets %) (asset-type->class asset-type))
          (.getAssetNames assets)))
