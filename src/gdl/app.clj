(ns gdl.app
  (:require [x.x :refer [update-map]]
            [gdl.lc :as lc]
            [gdl.graphics :as g]
            [gdl.files :as files]
            [gdl.graphics.batch :refer [sprite-batch]]
            [gdl.graphics.color :as color]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.world :as world]
            [gdl.scene2d.ui :as ui]
            [gdl.backends.lwjgl3 :as lwjgl3])
  (:import (com.badlogic.gdx Gdx Application ApplicationAdapter)
           com.badlogic.gdx.utils.ScreenUtils))

(defn app ^Application []
  Gdx/app)

(defn exit []
  (.exit (app)))

(defmacro with-context [& exprs]
  `(.postRunnable (app) (fn [] ~@exprs)))

(defn- on-resize [w h]
  (let [center-camera? true]
    (.update gui/viewport   w h center-camera?)
    (.update world/viewport w h center-camera?)))

(defn- fix-viewport-update
  "Sometimes the viewport update is not triggered."
  []
  (when-not (and (= (.getScreenWidth  gui/viewport) (g/screen-width))
                 (= (.getScreenHeight gui/viewport) (g/screen-height)))
    (on-resize (g/screen-width) (g/screen-height))))

(defn- default-modules [{:keys [tile-size]}]
  [[:gdl.assets {:folder "resources/" ; TODO these are classpath settings ?
                 :sounds-folder "sounds/"
                 :sound-files-extensions #{"wav"}
                 :image-files-extensions #{"png" "bmp"}
                 :log-load-assets? false}]
   [:gdl.graphics.gui]
   [:gdl.graphics.world (or tile-size 1)]
   [:gdl.graphics.font]
   [:gdl.graphics.batch (sprite-batch)]
   [:gdl.graphics.shape-drawer]  ; requires batch
   ; this is the gdx default skin  - copied from libgdx project, check not included in libgdx jar somewhere?
   [:gdl.scene2d.ui (ui/skin (files/internal "scene2d.ui.skin/uiskin.json"))]])

(def state (atom nil))

(defn- current-screen-component []
  (let [k (::current-screen @state)]
    [k (k @state)]))

(defn current-screen-value []
  ((::current-screen @state) @state))

(defn- create-state [modules]
  ; turn state into a map after create, because order is important!
  (assert (apply distinct? (map first modules)))
  (->> (for [[k v] modules]
         (do
          ;(println "Create > " k)
          (let [ns-sym (-> k name symbol)]
            (or (find-ns ns-sym)
                (require ns-sym))
            (assert (find-ns ns-sym)))
          [k (lc/create [k v])]))
       (into {})))

(defn set-screen [k]
  (assert (contains? @state k) (str "Cannot find screen with key: " k " in state."))
  (when (::current-screen @state)
    (lc/hide (current-screen-component)))
  (swap! state assoc ::current-screen k)
  (lc/show (current-screen-component)))

(defn- application-adapter [{:keys [log-lc? modules first-screen] :as config}]
  (proxy [ApplicationAdapter] []
    (create  []
      (reset! state (create-state (concat (default-modules config)
                                          modules)))
      (set-screen first-screen))
    (dispose []
      (swap! state update-map lc/dispose))
    (render []
      (ScreenUtils/clear color/black)
      (fix-viewport-update)
      (lc/render (current-screen-component))
      (lc/tick (current-screen-component)
               (* (g/delta-time) 1000)))
    (resize [w h]
      (on-resize w h))))

(comment

 ; TODO? modules internal state, functional way? impractical?
 ; TODO pass without * 1000 => speed/everything is in seconds anyway

 (clojure.pprint/pprint @state)

 )



(defn start [{:keys [window] :as config}]
  (lwjgl3/create-app (application-adapter config)
                     window))
