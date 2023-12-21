(ns gdl.backends.libgdx.context.input
  (:require gdl.context
            gdl.input.keys
            gdl.input.buttons
            [gdl.backends.libgdx.utils.reflect :refer [bind-roots]])
  (:import (com.badlogic.gdx Gdx Input$Buttons Input$Keys)))

(extend-type gdl.context.Context
  gdl.context/Input
  (button-pressed?      [_ button] (.isButtonPressed     Gdx/input button))
  (button-just-pressed? [_ button] (.isButtonJustPressed Gdx/input button))

  (key-pressed?      [_ k] (.isKeyPressed     Gdx/input k))
  (key-just-pressed? [_ k] (.isKeyJustPressed Gdx/input k)))

(bind-roots "com.badlogic.gdx.Input$Keys"    'int "gdl.input.keys")
(bind-roots "com.badlogic.gdx.Input$Buttons" 'int "gdl.input.buttons")
