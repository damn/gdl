(ns gdl.draw)

(defprotocol Drawer
  (text [_ {:keys [font text x y h-align up?]}])
  (image [_ image position]
         [_ image x y])
  (centered-image [_ image position])
  (rotated-centered-image [_ image rotation position])
  (ellipse [_ position radius-x radius-y color])
  (filled-ellipse [_ position radius-x radius-y color])
  (circle [_ position radius color])
  (filled-circle [_ position radius color])
  (arc [_ center-position radius start-angle degree color])
  (sector [_ center-position radius start-angle degree color])
  (rectangle [_ x y w h color])
  (filled-rectangle [_ x y w h color])
  (line [_ start-position end-position color]
        [_ x y ex ey color])
  (with-line-width [_ width draw-fn]))

(defn grid [drawer leftx bottomy gridw gridh cellw cellh color]
  (let [w (* gridw cellw)
        h (* gridh cellh)
        topy (+ bottomy h)
        rightx (+ leftx w)]
    (doseq [idx (range (inc gridw))
            :let [linex (+ leftx (* idx cellw))]]
      (line drawer linex topy linex bottomy color))
    (doseq [idx (range (inc gridh))
            :let [liney (+ bottomy (* idx cellh))]]
      (line drawer leftx liney rightx liney color))))
