(ns gdl.graphics.animation)

(defprotocol Animation
  (tick [_ delta])
  (restart [_])
  (stopped? [_])
  (current-frame [_]))

(defrecord ImmutableAnimation [frames frame-duration looping? cnt maxcnt]
  Animation
  (tick [this delta]
    (let [newcnt (+ cnt delta)]
      (assoc this :cnt (cond (< newcnt maxcnt) newcnt
                             looping? (min maxcnt (- newcnt maxcnt))
                             :else maxcnt))))
  (restart [this]
    (assoc this :cnt 0))
  (stopped? [_]
    (and (not looping?) (= cnt maxcnt)))
  (current-frame [this]
    ; dec because otherwise (quot frame-duration frame-duration) = 1, so we get the next frame
    ; which leads to java.lang.IndexOutOfBoundsException
    (-> cnt dec (quot frame-duration) int frames)))

(defn create
  [frames & {:keys [frame-duration looping?]}]
  (map->ImmutableAnimation
    {:frames (vec frames)
     :frame-duration frame-duration
     :looping? looping?
     :cnt 0
     :maxcnt (* (count frames) frame-duration)}))
