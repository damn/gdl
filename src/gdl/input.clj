(ns gdl.input
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn key-pressed? [key]
  (.isKeyPressed Gdx/input (k->code key)))

(defn key-just-pressed? [key]
  (.isKeyJustPressed Gdx/input (k->code key)))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn button-just-pressed? [button]
  (.isButtonJustPressed Gdx/input (button->code button)))

(defn set-processor! [input-processor]
  (.setInputProcessor Gdx/input input-processor))
