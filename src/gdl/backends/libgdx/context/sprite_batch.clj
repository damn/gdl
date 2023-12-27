(ns ^:no-doc gdl.backends.libgdx.context.sprite-batch
  (:import com.badlogic.gdx.graphics.g2d.SpriteBatch))

(defn ->context []
  {:batch (SpriteBatch.)})
