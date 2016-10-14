(ns daveconservatoire.site.ui.vexflow
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.next :as om]
            [om.dom :as dom]
            [cljs.core.async :as async :refer [chan <!]]
            [goog.dom :as gdom]
            [cljs.spec :as s]))


(s/def ::width pos-int?)
(s/def ::height pos-int?)

(s/def ::note-key string?)
(s/def ::duration #{"q" "h" "w" "1" "2" "4" "8"})

(s/def ::clef #{::treble ::bass ::tenor ::alto ::soprano ::percussion ::mezzo-soprano
                ::baritone-c ::baritone-f ::subbass ::french})

(s/def ::note (s/keys :req [::keys ::duration]))
(s/def ::bar (s/keys :req [::notes]))

(s/def ::keys (s/coll-of ::note-key))
(s/def ::notes (s/coll-of ::note))
(s/def ::bars (s/coll-of ::bars))
(s/def ::score (s/keys :req [::width ::height] :opt [::bars ::clef]))

(defonce scripts (atom {}))

(defn load-external-script [path]
  (if-let [script (get @scripts path)]
    script
    (let [c (chan)
          script (gdom/createDom "script" #js {:src    path
                                               :onload #(async/close! c)})]
      (swap! scripts assoc path c)
      (gdom/append js/document.body script)
      c)))

(defn require-vexflow [] (load-external-script "/vendor/vexflow-min.js"))

(defn format-and-draw [ctx stave notes]
  (js/Vex.Flow.Formatter.FormatAndDraw ctx stave (clj->js notes)))

(defn render-score [{:keys [::bars ::width ::height ::clef] :as props} container]
  (let [VF (.. js/Vex -Flow)
        renderer (VF.Renderer. container VF.Renderer.Backends.SVG)
        ctx (.getContext renderer)
        bar-size (/ width (count bars))]
    (.resize renderer (inc width) height)
    (.setFont ctx "Arial" 10 "")
    (.setBackgroundFillStyle ctx "#eed")

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
      (<! (require-vexflow))
      (let [div (js/ReactDOM.findDOMNode this)]
        (render-score (om/props this) div))))

  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil))))

(def score (om/factory Score))
