(ns gdl.math
  (:refer-clojure :exclude [contains?])
  (:import (com.badlogic.gdx.math Circle
                                  Intersector
                                  Rectangle)))

(defn circle [x y radius]
  (Circle. x y radius))

(defn rectangle [x y width height]
  (Rectangle. x y width height))

(defmulti overlaps?
  (fn [a b] [(class a) (class b)]))

(defmethod overlaps? [Circle Circle]
  [^Circle a ^Circle b]

;		float dx = x - c.x;
;		float dy = y - c.y;
;		float distance = dx * dx + dy * dy;
;		float radiusSum = radius + c.radius;
;		return distance < radiusSum * radiusSum;
  (Intersector/overlaps a b))

(defmethod overlaps? [Rectangle Rectangle]
  [^Rectangle a ^Rectangle b]
  ; return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
  (Intersector/overlaps a b))

(defmethod overlaps? [Rectangle Circle]
  [^Rectangle rect ^Circle circle]

;		float closestX = c.x;
;		float closestY = c.y;
;
;		if (c.x < r.x) {
;			closestX = r.x;
;		} else if (c.x > r.x + r.width) {
;			closestX = r.x + r.width;
;		}
;
;		if (c.y < r.y) {
;			closestY = r.y;
;		} else if (c.y > r.y + r.height) {
;			closestY = r.y + r.height;
;		}
;
;		closestX = closestX - c.x;
;		closestX *= closestX;
;		closestY = closestY - c.y;
;		closestY *= closestY;
;
;		return closestX + closestY < c.radius * c.radius;
  (Intersector/overlaps circle rect))

(defmethod overlaps? [Circle Rectangle]
  [^Circle circle ^Rectangle rect]
  (Intersector/overlaps circle rect))

(defn contains?
  "whether the point is contained in the rectangle"
  [rectangle [x y]]
  (Rectangle/.contains rectangle x y)
  ; return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y;
  )
