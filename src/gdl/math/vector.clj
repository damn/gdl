(ns gdl.math.vector
  (:import [com.badlogic.gdx.math Vector2 MathUtils]))

(defn- ^Vector2 ->v [[x y]]
  (Vector2. x y))

(defn- ->p [^Vector2 v]
  [(.x ^Vector2 v)
   (.y ^Vector2 v)])

(defn scale     [v n]    (->p (.scl ^Vector2 (->v v) (float n))))
(defn normalise [v]      (->p (.nor ^Vector2 (->v v))))
(defn add       [v1 v2]  (->p (.add ^Vector2 (->v v1) ^Vector2 (->v v2))))
(defn length    [v]      (.len ^Vector2 (->v v)))
(defn distance  [v1 v2]  (.dst ^Vector2 (->v v1) ^Vector2 (->v v2)))

(defn normalised? [v]
  ; Returns true if a is nearly equal to b.
  (MathUtils/isEqual 1 (length v)))

(defn get-normal-vectors [[x y]]
  [[(- y)   x]
   [y    (- x)]])

(defn direction [[sx sy] [tx ty]]
  (normalise [(- tx sx)
              (- ty sy)]))

(defn get-angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), ->counterclockwise"
  [v]
  (.angleDeg (->v v)
             (Vector2. 0 1)))

(comment

 (clojure.pprint/pprint
  (for [v [[0 1]
           [1 1]
           [1 0]
           [1 -1]
           [0 -1]
           [-1 -1]
           [-1 0]
           [-1 1]]]
    [v
     (.angleDeg (->v v) (Vector2. 0 1))
     (get-angle-from-vector (->v v))]))

 )

(defn vector-from-angle
  "Given a vector which points upwards on the y-axis, returns a vector rotated by angle degrees clockwise."
  [angle]
  {:post [(normalised? %)]}
  (->p (.rotateDeg (Vector2. 0 -1) (float angle))))

(defn add-or-subtract-angle
  "man will current in richtung new rotieren. muss man dazu add or subtract machen f�r den k�rzesten weg?
  returns add or subtract-function"
  [current-angle new-angle]
	(if (<= current-angle 180)
   (if (or (<= new-angle current-angle)
           (>= new-angle (+ 180 current-angle)))
     -
     +)
   (if (and (>= new-angle (- current-angle 180))
            (<= new-angle current-angle))
     -
     +)))

(defn get-angle-to-position
  "Treat Position a the middle of the coordinate sytem and the angle is from the top
  so if target is right of origin -> 90� below origin -> 180�"
  [{originx 0 :as origin} {targetx 0 :as target}]
  (let [v (Vector2. 0 -1)
        originv (->v origin)
        targetv (->v target)
        differencev (.nor (.sub targetv originv))
        angle (int (Math/toDegrees (Math/acos (.dot v differencev))))]
     ; 0-180� ok -> target rechts von origin ansonsten adjust
    (if (< targetx originx)
      (- 360 angle)
      angle)))

(defn degrees? [n]
  (and (>= n 0) (<= n 360)))

(defn smallest-distance [a b]
	{:pre [(degrees? a)
         (degrees? b)]
	 :post [(degrees? %)]}
  (let [dist (Math/abs (float (- a b)))]
    (if (> dist 180)
      (- 360 dist)
      dist)))

(defn rotate-angle-to-angle [current-angle new-angle rotationspeed delta]
  (let [adjustment (* delta rotationspeed)]
    (if (>= adjustment
            (smallest-distance current-angle new-angle))
      new-angle
      (let [change-fn (add-or-subtract-angle current-angle new-angle)
            adjusted-angle (change-fn current-angle adjustment)]
        (cond
         (< adjusted-angle   0) (+ 360 adjusted-angle)
         (> adjusted-angle 360) (- adjusted-angle 360)
         :else adjusted-angle)))))

(defn degree-add [degree by]
  (rem (+ degree by) 360))

; TODO comment from libgdx discord.
(defn degree-minus [degree by]
  (rem (+ 360 (- degree by)) 360))
; <@927315926032998421> I'm poking around, since I used to use Clojure quite a lot... in vector.clj, `(defn degree-minus [degree by] (mod (- degree by) 360))` would be a better implementation, because your current one with `rem` can return negative values if `by` is greater than `degree` by more than 360
