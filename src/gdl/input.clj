; remove all 'is-...?' -> just add '?' at end of fn name -> grep
; vimgrep/is-.*-down?\|is-.*-pressed?/g src/**
(ns gdl.input
  (:import (com.badlogic.gdx Gdx Input Input$Buttons Input$Keys)))

(defn input ^Input []
  Gdx/input)

; TODO use set-input-processor (its the class name)
(defn set-processor [processor]
  (.setInputProcessor (input) processor))

(defn- to-mouse-key [k]
  (case k
    :left  Input$Buttons/LEFT
    :right Input$Buttons/RIGHT))

(defn- is-mouse-button-down? [k] (.isButtonPressed    (input) (to-mouse-key k)))

; returns true no matter how many times called in 1 frame (non-consuming)
(defn- is-mouse-pressed?     [k] (.isButtonJustPressed (input) (to-mouse-key k)))

(def is-leftbutton-down?  (partial is-mouse-button-down? :left))
(def is-leftm-pressed?    (partial is-mouse-pressed?     :left))
(def is-rightbutton-down? (partial is-mouse-button-down? :right))
(def is-rightm-pressed?   (partial is-mouse-pressed?     :right))

(defn- fix-number-key ; TODO remove.
  "Keys :0, :1, ... :9 are understood as NUM_0, NUM_1, ..."
  [k]
  (try
   (let [is-num (Integer/parseInt (name k))]
     (str "NUM_" (name k)))
   (catch NumberFormatException e
     (name k))))

(def ^:private to-keyboard-key
  (memoize (fn [k]
             (eval (symbol (str "com.badlogic.gdx.Input$Keys/" (fix-number-key k)))))))

(defn is-key-pressed?
  ; TODO check if this docstring is still true.
  "Since last call to this. So do not call this twice in one frame else it will return false."
  [k]
  (.isKeyJustPressed (input) (to-keyboard-key k)))

(defn is-key-down? [k]
  (.isKeyPressed (input) (to-keyboard-key k)))
