(ns gdl.app
  (:require [x.x :refer [update-map]]
            [gdl.lc :as lc]
            [gdl.gdx :as gdx :refer [app]]
            [gdl.graphics :as g]
            [gdl.graphics.color :as color]
            [gdl.graphics.gui :as gui]
            [gdl.graphics.world :as world]
            [gdl.backends.lwjgl3 :as lwjgl3])
  (:import com.badlogic.gdx.ApplicationAdapter
           com.badlogic.gdx.utils.ScreenUtils))

(defn exit []
  (.exit app))

(defmacro with-context [& exprs]
  `(.postRunnable app (fn [] ~@exprs)))

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
  [[:gdl.gdx]
   [:gdl.assets {:folder "resources/" ; TODO these are classpath settings ?
                 :sounds-folder "sounds/"
                 :sound-files-extensions #{"wav"}
                 :image-files-extensions #{"png" "bmp"}
                 :log-load-assets? false}]
   [:gdl.graphics.gui]
   [:gdl.graphics.world (or tile-size 1)]
   [:gdl.graphics.font]
   [:gdl.graphics.batch]
   [:gdl.graphics.shape-drawer]  ; after :gdl.graphics.batch
   [:gdl.ui]])

(def state (atom nil))
(def current-screen (atom nil))

(defn- current-screen-component [] ;
  (let [component-k @current-screen]
    [component-k (get @state component-k)]))

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
  (lc/hide (current-screen-component))
  (reset! current-screen k)
  (lc/show (current-screen-component)))

(defn- ->Game [{:keys [log-lc? modules first-screen] :as config}]
  (let [modules (concat (default-modules config)
                        modules)]
    (when log-lc? (clojure.pprint/pprint modules))
    (proxy [ApplicationAdapter] []
      (create  []
        (reset! state (create-state modules))
        (set-screen first-screen))
      (dispose []
        (swap! state update-map lc/dispose))
      (render []
        (ScreenUtils/clear color/black)
        (fix-viewport-update)
        (lc/render (current-screen-component))
        (lc/tick (current-screen-component)
                 (* (.getDeltaTime gdx/graphics) 1000)))
      (resize [w h]
        (on-resize w h)))))

(comment

 ; TODO? modules internal state, functional way? impractical?
 ; TODO pass without * 1000 => speed/everything is in seconds anyway

 (clojure.pprint/pprint @state)

 )



(defn start [{:keys [window] :as config}]
  (lwjgl3/create-app (->Game config)
                     window))
