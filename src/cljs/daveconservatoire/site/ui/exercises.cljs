(ns daveconservatoire.site.ui.exercises
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [untangled.client.core :as uc]
            [untangled.client.mutations :as um]
            [cljs.spec :as s]
            [daveconservatoire.audio.core :as audio]
            [daveconservatoire.site.ui.util :as u]))

(defprotocol IExercise
  (new-round [this props]))

(s/def ::progress-value number?)
(s/def ::progress-total number?)
(s/def ::react-component object?)

(s/def ::description string?)
(s/def ::option (s/cat :value string? :label string?))
(s/def ::options (s/+ (s/spec ::option)))

(s/def ::ex-props (s/keys :req [::description ::options]))
(s/def ::ex-answer (s/nilable string?))
(s/def ::ex-total-questions pos-int?)
(s/def ::streak-count nat-int?)

(s/def ::value-descriptor
  (s/or :sound ::audio/sound
        :range (s/and (s/tuple ::audio/sound #{".."} ::audio/sound)
                      (fn [[[_ a] _ [_ b] :as t]]
                        (if (< (audio/note->semitone a)
                               (audio/note->semitone b))
                          t
                          false)))
        :list (s/coll-of ::audio/sound [])))

(s/def ::pitch ::value-descriptor)
(s/def ::variation ::value-descriptor)

(om/defui ^:once ProgressBar
  Object
  (render [this]
    (let [{:keys [::progress-value ::progress-total] :as props} (om/props this)
          pct (-> (/ progress-value progress-total) (* 100))]
      (dom/div (u/props->html {:className "progress progress-striped active success" :style {:margin 20}}
                              props)
        (dom/div #js {:className "bar" :style #js {:width (str pct "%")}})))))

(def progress-bar (om/factory ProgressBar))

(s/fdef progress-bar
  :args (s/cat :props (s/keys :opt [::progress-total ::progress-value])
               :children (s/* ::react-component)))

(defn play-notes [notes]
  (let [nodes (->> notes
                   (into [] (map (fn [s]
                                   {::audio/node-gen (->> s audio/semitone->note (get @audio/*sound-library*))}))))]
    (audio/play-regular-sequence nodes {::audio/time     (audio/current-time)
                                        ::audio/interval 2})))

(defn play-sound [c] (play-notes (-> (om/props c) ::notes)))

(defn check-answer [c]
  (let [{:keys [::ex-answer ::correct-answer ::streak-count ::parent] :as props} (om/props c)]
    (if (= ex-answer correct-answer)
      (let [next-streak (inc streak-count)
            new-props (new-round parent
                             (merge props
                                    {::streak-count next-streak
                                     ::ex-answer    nil}))]
        (om/transact! c `[(ui/set-props ~new-props) (dcex/play-round-sound)]))
      (um/set-value! c ::streak-count 0))))

(defmethod um/mutate 'dcex/play-round-sound [{:keys [state ref]} _ _]
  {:action
   (fn []
     (play-notes (::notes (get-in @state ref))))})

(defn render-ex [c]
  (let [{:keys [::description ::options ::ex-answer
                ::ex-total-questions ::streak-count] :as props} (om/props c)]
    (assert (s/valid? ::ex-props props) (s/explain-str ::ex-props props))
    (dom/div #js {:className "lesson-content"}
      (dom/div #js {:className "single-exercise visited-no-recolor"
                    :style     #js {:overflow "hidden" :visibility "visible"}}
        (dom/article #js {:className "exercises-content clearfix"}
          (dom/div #js {:className "exercises-body"}
            (dom/div #js {:className "exercises-stack"})
            (dom/div #js {:className "exercises-card current-card"}
              (dom/div #js {:className "current-card-container card-type-problem"}
                (dom/div #js {:className "current-card-container-inner vertical-shadow"}
                  (dom/div #js {:className "current-card-contents"}
                    (progress-bar {::progress-value streak-count
                                   ::progress-total ex-total-questions})
                    (dom/div #js {:id "problem-and-answer" :className "framework-khan-exercises"}
                      (dom/div #js {:id "problemarea"}
                        (dom/div #js {:id "workarea"}
                          (dom/div #js {:id "problem-type-or-description"}
                            (dom/div #js {:className "problem"}
                              (dom/p nil
                                description)
                              (dom/a #js {:className "btn_primary"
                                          :onClick   #(play-sound c)}
                                "Play Again"))))
                        (dom/div #js {:id "hintsarea"}))
                      (dom/div #js {:id "answer_area_wrap"}
                        (dom/div #js {:id "answer_area"}
                          (dom/form #js {:id "answerform" :name "answerform"}
                            (dom/div #js {:className "info-box" :id "answercontent"}
                              (dom/span #js {:className "info-box-header"}
                                "Answer")
                              (dom/div #js {:className "fancy-scrollbar"}
                                (dom/ul nil
                                  (for [[value label] options]
                                    (dom/li #js {:key value}
                                      (dom/label nil
                                        (dom/input #js {:type     "radio" :name "exercise-answer"
                                                        :checked  (= ex-answer value)
                                                        :value    value
                                                        :onChange #(um/set-string! c ::ex-answer :event %)})
                                        (dom/span #js {:className "value"} label))))))
                              (dom/div #js {:className "answer-buttons"}
                                (dom/div #js {:className "check-answer-wrapper"}
                                  (dom/input #js {:className "simple-button green" :type "button" :value "Check Answer"
                                                  :onClick   #(check-answer c)}))
                                (dom/input #js {:className "simple-button green" :id "next-question-button" :name "correctnextbutton" :style #js {:display "none"} :type "button" :value "Correct! Next Question..."})
                                (dom/div #js {:id "positive-reinforcement" :style #js {:display "none"}}
                                  (dom/img #js {:src "/images/face-smiley.png"})))))))
                      (dom/div #js {:style #js {:clear "both"}}))))))))))))

(defn int-in [min max] (+ min (rand-int (- max min))))

(defn descriptor->value [desc]
  {:pre [(s/valid? ::value-descriptor desc)]}
  (let [[type] (s/conform ::value-descriptor desc)]
    (audio/note->semitone
      (case type
        :sound desc
        :list (rand-nth (vec desc))
        :range (let [[a _ b] desc]
                 (int-in (audio/note->semitone a) (audio/note->semitone b)))))))

(s/fdef descriptor->value
  :args (s/cat :desc ::value-descriptor)
  :ret ::audio/semitone)

(om/defui ^:once Exercise
  static uc/InitialAppState
  (initial-state [_ _] {::ex-answer          nil
                        ::ex-total-questions 10
                        ::streak-count       0})

  static om/IQuery
  (query [_] ['*])

  static om/Ident
  (ident [_ props] (om/ident (::parent props) props))

  Object
  (render [this]
    (render-ex this)))

(def exercise (om/factory Exercise))

(defn rand-direction [] (rand-nth [1 -1]))

(s/fdef rand-direction
  :args (s/cat)
  :ret #{-1 1})

(defn gen-pitch-notes [{:keys [::pitch ::variation]}]
  (let [a (descriptor->value pitch)
        b (+ a (* (descriptor->value variation) (rand-direction)))]
    [a b]))

(s/fdef gen-pitch-notes
  :args (s/cat :data (s/keys :req [::pitch ::variation]))
  :ret (s/tuple ::audio/semitone ::audio/semitone))

(om/defui ^:once PitchDetection
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::description "You will hear two notes. Is the second note lower or higher in pitch?"
         ::options     [["lower" "Lower"] ["higher" "Higher"]]
         ::pitch       ["C3" ".." "B5"]
         ::variation   24
         ::parent      this}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name "pitch"])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [[a b :as notes] (gen-pitch-notes props)]
      (assoc props
        ::notes notes
        ::correct-answer (if (< a b) "higher" "lower"))))

  Object
  (render [this]
    (exercise (om/props this))))

(def pitch-detection (om/factory PitchDetection))

(defmulti slug->exercice identity)

(defmethod slug->exercice :default [_] nil)

(defmethod slug->exercice "pitch-1" [_]
  {::class PitchDetection
   ::props {::variation 24}})

(defmethod slug->exercice "pitch-2" [_]
  {::class PitchDetection
   ::props {::variation 12}})

(defmethod slug->exercice "pitch-3" [_]
  {::class PitchDetection
   ::props {::variation 5}})
