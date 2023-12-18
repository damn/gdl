(ns gdl.context.assets
  (:require [clojure.string :as str]
            gdl.protocols)
  (:import com.badlogic.gdx.Gdx
           com.badlogic.gdx.assets.AssetManager
           com.badlogic.gdx.audio.Sound
           com.badlogic.gdx.files.FileHandle
           com.badlogic.gdx.graphics.Texture))

(defn- recursively-search-files [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file) result
          (.isDirectory file) (recur (concat remaining (.list file)) result)
          (extensions (.extension file)) (recur remaining (conj result (str/replace-first (.path file) folder "")))
          :else (recur remaining result))))

(defn- load-assets [^AssetManager manager folder file-extensions ^Class klass log-load-assets?]
  (doseq [file (recursively-search-files folder file-extensions)]
    (when log-load-assets?
      (println "load-assets" (str "[" (.getSimpleName klass) "] - [" file "]")))
    (.load manager file klass)))

(defn- load-all-assets [{:keys [folder
                                log-load-assets?
                                sound-files-extensions
                                image-files-extensions]
                         :as config}]
  (doseq [k [:folder
             :log-load-assets?
             :sound-files-extensions
             :image-files-extensions]]
    (assert (contains? config k)
            (str "config key(s) missing: " k)))
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [file]
                    ; TODO faster when I pass class arg here at the end ?
                    ; what difference ?
                    (.get ^AssetManager this ^String file)))]
    (load-assets manager folder sound-files-extensions Sound   log-load-assets?)
    (load-assets manager folder image-files-extensions Texture log-load-assets?)
    (.finishLoading manager)
    manager))

; TODO assets used @ image & sound player thingy
; => move sounds here also

(defn ->context-map []
  {:assets (load-all-assets {:folder "resources/" ; TODO these are classpath settings ?
                             :sound-files-extensions #{"wav"}
                             :image-files-extensions #{"png" "bmp"}
                             :log-load-assets? false})})
