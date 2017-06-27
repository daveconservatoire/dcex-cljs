(ns daveconservatoire.site.ui.listeners
  (:require [om.next :as om :include-macros true]
            [goog.events :as gevents]
            [daveconservatoire.support.specs]
            [cljs.spec.alpha :as s]))

(def KEYS
  {8  :backspace 9 :tab 13 :return 32 :space 37 :left 38 :up 39 :right 40 :down
   65 :a 66 :b 67 :c 68 :d 69 :e 70 :f 71 :g 72 :h 73 :i 74 :j 75 :k 76 :l 77 :m 78 :n
   79 :o 80 :p 81 :q 82 :r 83 :s 84 :t 85 :u 86 :v 87 :w 88 :x 89 :y 90 :z
   191 :slash})

(def KEYS_SHIFT
  {8  :backspace 9 :tab 13 :return 32 :space 37 :left 38 :up 39 :right 40 :down
   65 :A 66 :B 67 :C 68 :D 69 :E 70 :F 71 :G 72 :H 73 :I 74 :J 75 :K 76 :L 77 :M 78 :N
   79 :O 80 :P 81 :Q 82 :R 83 :S 84 :T 85 :U 86 :V 87 :W 88 :X 89 :Y 90 :Z
   191 :?})

(defn event-key [e]
  (let [code (.-keyCode e)
        key-table (if (.-shiftKey e) KEYS_SHIFT KEYS)]
    (get key-table code)))

(defn event [props] (get props :event "click"))

(defn handle-event [owner e]
  (if-let [f (-> (om/props owner) ::on-trigger)]
    (f e)))

(defn event-target [props] (get props ::target js/document))

(defn key? [e key]
  (let [code (.-keyCode e)]
    (or (= code key) (= code (KEYS key)))))

(defn handle-key-event [key-handlers]
  (fn [e]
    (let [event-key (KEYS (.-keyCode e))]
      (if-let [handler (get key-handlers event-key)]
        (handler e)))))

(om/defui ^:once SimpleListener
  Object
  (initLocalState [this]
    {:handler (partial handle-event this)})

  (componentDidMount [this]
    (let [props (om/props this)]
      (gevents/listen (event-target props) (event props) (om/get-state this :handler))))

  (componentWillUnmount [this]
    (let [props (om/props this)]
      (gevents/unlisten (event-target props) (event props) (om/get-state this :handler))))

  (render [_] nil))

(def simple-listener (om/factory SimpleListener))

(s/def ::event any?)

(s/def ::on-trigger
  (s/fspec :args (s/cat :event ::event)))

(s/fdef simple-listener
  :args (s/cat :props (s/keys :req [::on-trigger]))
  :ret :om.next/component)
