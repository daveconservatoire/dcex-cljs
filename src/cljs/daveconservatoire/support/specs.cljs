(ns daveconservatoire.support.specs
  (:require-macros [daveconservatoire.support.specs :refer [keys-js]])
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::uint8-array #(instance? js/Uint8Array %))
(s/def ::array-buffer #(instance? js/ArrayBuffer %))

(defn base64-str? [str] (str/starts-with? str "data:audio"))

(s/def ::base64-string (s/and string? base64-str?))
(s/def ::chan any?)

(defn chan-of [spec]
  ::chan)

(s/def :om.next/component any?)

(def event-types
  #{"copy" "cut" "paste"
    "compositiend" "compositistart" "compositiupdate"
    "keydown" "keypress" "keyup"
    "focus" "blur"
    "change" "input" "submit"
    "click" "ctextmenu" "doubleclick" "drag" "dragend" "dragenter" "dragexit"
    "dragleave" "dragover" "dragstart" "drop" "mousedown" "mouseenter" "mouseleave"
    "mousemove" "mouseout" "mouseover" "mouseup"
    "select"
    "touchcancel" "touchend" "touchmove" "touchstart"
    "scroll"
    "wheel"
    "abort" "canplay" "canplaythrough" "duratichange" "emptied" "encrypted"
    "ended" "loadeddata" "loadedmetadata" "loadstart" "pause" "play"
    "playing" "progress" "ratechange" "seeked" "seeking" "stalled" "suspend"
    "timeupdate" "volumechange" "waiting"
    "load" "error"
    "animationstart" "animationend" "animationiteration"
    "transitionend"})

(s/def :react.synth-event/bubbles boolean?)
(s/def :react.synth-event/cancelable boolean?)
(s/def :react.synth-event/currentTarget any?)
(s/def :react.synth-event/defaultPrevented boolean?)
(s/def :react.synth-event/eventPhase nat-int?)
(s/def :react.synth-event/isTrusted boolean?)
(s/def :react.synth-event/nativeEvent any?)
(s/def :react.synth-event/preventDefault (s/fspec :args (s/cat) :ret nil?))
(s/def :react.synth-event/isDefaultPrevented (s/fspec :args (s/cat) :ret boolean?))
(s/def :react.synth-event/stopPropagation (s/fspec :args (s/cat) :ret nil?))
(s/def :react.synth-event/isPropagationStopped (s/fspec :args (s/cat) :ret boolean?))
(s/def :react.synth-event/target any?)
(s/def :react.synth-event/timestamp pos-int?)
(s/def :react.synth-event/type event-types)

(s/def :react/event
  (s/keys
    :req-un [:react.synth-event/bubbles :react.synth-event/cancelable :react.synth-event/currentTarget
             :react.synth-event/defaultPrevented :react.synth-event/eventPhase :react.synth-event/isTrusted
             :react.synth-event/nativeEvent :react.synth-event/preventDefault :react.synth-event/target :react.synth-event/timestamp
             :react.synth-event/type]))


