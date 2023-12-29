(ns gdl.graphics.camera
  "Convinience functions operating on com.badlogic.gdx.graphics.OrthographicCamera."
  (:import com.badlogic.gdx.graphics.OrthographicCamera
           com.badlogic.gdx.math.Vector3))

(defn zoom [^OrthographicCamera camera]
  (.zoom camera))

(defn set-zoom! [^OrthographicCamera camera amount]
  (set! (.zoom camera) amount)
  (.update camera))

(defn reset-zoom! [^OrthographicCamera camera]
  (set! (.zoom camera) 1.0))

(defn position [^OrthographicCamera camera]
  [(.x (.position camera))
   (.y (.position camera))])

(defn set-position! [^OrthographicCamera camera [x y]]
  (set! (.x (.position camera)) (float x))
  (set! (.y (.position camera)) (float y))
  (.update camera))

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

; could do onl with left-bottom and top-right points
(defn calculate-zoom
  "Calculates the zoom value for camera to see all the 4 points."
  [^OrthographicCamera camera & {:keys [left top right bottom]}]
  (let [viewport-width  (.viewportWidth  camera)
        viewport-height (.viewportHeight camera)
        [px py] (position camera)
        x-diff (max (- px (left 0)) (- (right 0) px))
        y-diff (max (- (top 1) py) (- py (bottom 1)))
        vp-ratio-w (/ (* x-diff 2) viewport-width)
        vp-ratio-h (/ (* y-diff 2) viewport-height)
        new-zoom (max vp-ratio-w vp-ratio-h)]
    new-zoom))
