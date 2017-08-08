(ns daveconservatoire.site.ui.vexflow
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.next :as om]
            [om.dom :as dom]
            [common.async :refer [wait-for]]
            [cljs.core.async :refer [chan <!]]
            [goog.object :as gobj]
            [goog.dom :as gdom]
            [daveconservatoire.site.ui.util :as u]
            [cljs.spec.alpha :as s]))

(s/def ::width pos-int?)
(s/def ::height pos-int?)
(s/def ::scale (s/and number? pos?))
(s/def ::backend #{::canvas ::svg ::raphael})

(s/def ::note-key string?)
(s/def ::duration #{"q" "h" "w" "1" "2" "4" "8"})

(s/def ::clef #{::treble ::bass ::tenor ::alto ::soprano ::percussion ::mezzo-soprano
                ::baritone-c ::baritone-f ::subbass ::french})

(s/def ::note (s/keys :req [::keys ::duration]))
(s/def ::bar (s/keys :req [::notes]))

(s/def ::keys (s/coll-of ::note-key))
(s/def ::notes (s/coll-of ::note))
(s/def ::bars (s/coll-of ::bars))
(s/def ::score (s/keys :req [::width ::height] :opt [::bars ::clef ::scale ::backend]))

(defn require-vexflow [] (u/load-external-script "/vendor/vexflow-min.js"))

(defn format-and-draw [ctx stave notes]
  (js/Vex.Flow.Formatter.FormatAndDraw ctx stave (clj->js notes)))

(def backend-renderer
  {nil       1
   ::canvas  1
   ::svg     3
   ::raphael 2})

(def backend-element
  {nil       dom/canvas
   ::canvas  dom/canvas
   ::svg     dom/div
   ::raphael dom/div})

(defn render-score [{:keys [::bars ::width ::height ::clef ::scale ::backend]} container]
  (let [VF (.. js/Vex -Flow)
        renderer (VF.Renderer. container (backend-renderer backend))
        ctx (.getContext renderer)
        scale (or scale 1)
        bar-size (/ width (count bars) scale)]
    (.resize renderer (+ width scale) height)
    (.setFont ctx "Arial" 10 "")
    (.setBackgroundFillStyle ctx "#eed")
    (.scale ctx scale scale)

    (doseq [[i {:keys [::notes]}] (map vector (range) bars)
            :let [stave (VF.Stave. (* i bar-size) 0 bar-size)]]
      (if (and (zero? i) clef) (.addClef stave (name clef)))
      (.. stave (setContext ctx) (draw))
      (format-and-draw ctx stave (into [] (map #(VF.StaveNote. (clj->js %))) notes)))))

(s/fdef render-score
  :args (s/cat :score ::score))

(om/defui ^:once Score
  Object
  (initLocalState [_] {:ready false})

  (componentDidMount [this]
    (go
      (require-vexflow)
      (<! (wait-for (fn [] (gobj/getValueByKeys js/window #js ["Vex" "Flow" "Renderer"]))))
      (let [div (gobj/get this "el")]
        (render-score (om/props this) div))))

  (componentDidUpdate [this _ _]
    (let [node (gobj/get this "el")]
      (gdom/removeChildren node)
      (render-score (om/props this) node)))

  (render [this]
    (let [{::keys [width height backend]} (om/props this)
          el (backend-element backend)]
      (el #js {:style #js {:width width :height height}
               :ref #(gobj/set this "el" %)}))))

(def score (om/factory Score))
