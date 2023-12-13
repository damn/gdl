(ns gdl.graphics.camera
  "Convinience functions operating on com.badlogic.gdx.graphics.OrthographicCamera."
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           com.badlogic.gdx.math.Vector3))

(defn set-zoom! [^OrthographicCamera camera amount]
  (set! (.zoom camera) amount)
  (.update camera))

(defn position [^OrthographicCamera camera]
  [(.x (.position camera))
   (.y (.position camera))])

(defn set-position! [^OrthographicCamera camera [x y]]
  (set! (.x (.position camera)) (float x))
  (set! (.y (.position camera)) (float y)))

(defn frustum [^OrthographicCamera camera]
  (let [frustum-points (for [^Vector3 point (take 4 (.planePoints (.frustum camera)))
                             :let [x (.x point)
                                   y (.y point)]]
                         [x y])
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y])) ; TODO need only x,y , w/h is viewport-width/height ?

(defn visible-tiles [camera]
  (let [[left-x right-x bottom-y top-y] (frustum camera)]
    (for  [x (range (int left-x)   (int right-x))
           y (range (int bottom-y) (+ 2 (int top-y)))]
      [x y])))
