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

(s/def ::name string?)
(s/def ::description string?)
(s/def ::option (s/cat :value string? :label string?))
(s/def ::options (s/or :text #{::option-type-text}
                       :select (s/+ (s/spec ::option))))

(s/def ::ex-props (s/keys :req [::options]))
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
        :list (s/coll-of ::audio/sound)))

(s/def ::pitch ::value-descriptor)
(s/def ::variation ::value-descriptor)
(s/def ::direction ::value-descriptor)
(s/def ::intervals (s/every ::audio/semitone))
(s/def ::direction? #{-1 1})

(om/defui ^:once ProgressBar
  Object
  (render [this]
    (let [{::keys [progress-value progress-total] :as props} (om/props this)
          pct (-> (/ progress-value progress-total) (* 100))]
      (dom/div (u/props->html {:className "progress progress-striped active success" :style {:margin 20}}
                              props)
        (dom/div #js {:className "bar" :style #js {:width (str pct "%")}})))))

(def progress-bar (om/factory ProgressBar))

(s/fdef progress-bar
  :args (s/cat :props (s/keys :opt [::progress-total ::progress-value]))
  :ret ::react-component)

(defn play-notes [notes]
  (let [nodes (->> notes
                   (into [] (map (fn [s]
                                   {::audio/node-gen (->> s audio/semitone->note (get @audio/*sound-library*))}))))]
    (audio/play-regular-sequence nodes {::audio/time     (audio/current-time)
                                        ::audio/interval 2})))

(defn play-sound [c] (play-notes (-> (om/props c) ::notes)))

(defmethod um/mutate 'dcex/check-answer
  [{:keys [state ref]} _ _]
  {:action
   (fn []
     (let [{:keys [::ex-answer ::correct-answer ::streak-count ::class ::ex-total-questions] :as props} (get-in @state ref)]
       (if (= ex-answer correct-answer)
         (let [next-streak (inc streak-count)
               new-props (new-round class
                           (merge props
                             {::streak-count next-streak
                              ::ex-answer    nil}))]
           (swap! state assoc-in ref new-props)
           (play-notes (::notes new-props)))
         (swap! state update-in ref assoc ::streak-count 0))))

   :remote
   (let [{::keys [name streak-count ex-total-questions]} (get-in @state ref)]
     (cond
       (= streak-count ex-total-questions)
       (-> (om/query->ast `[(exercise/score-master {:url/slug ~name})])
           :children first)

       (> streak-count 0)
       (-> (om/query->ast `[(exercise/score {:url/slug ~name})])
           :children first)))})

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

(defn completed? [{:keys [::ex-total-questions ::streak-count]}]
  (>= streak-count ex-total-questions))

(om/defui ^:once Exercise
  static uc/InitialAppState
  (initial-state [_ _] {::ex-answer          nil
                        ::ex-total-questions 10
                        ::streak-count       0})

  static om/IQuery
  (query [_] ['*])

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  Object
  (render [this]
    (let [{:keys [::options ::ex-answer ::ex-total-questions
                  ::streak-count] :as props} (om/props this)
          [opt-type _] (s/conform ::options options)]
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
                      (if-not (completed? props)
                        (progress-bar {::progress-value streak-count
                                       ::progress-total ex-total-questions}))
                      (u/transition-group #js {:transitionName "ex-complete"
                                               :transitionEnterTimeout 200
                                               :transitionLeaveTimeout 200}
                        (if (completed? props)
                          (dom/div #js {:key "done" :className "alert alert-success masterymsg"}
                            (dom/strong nil "Well done! ")
                            "You've mastered this skill - time to move on to something new")))
                      (dom/div #js {:id "problem-and-answer" :className "framework-khan-exercises"}
                        (dom/div #js {:id "problemarea"}
                          (dom/div #js {:id "workarea"}
                            (dom/div #js {:id "problem-type-or-description"}
                              (dom/div #js {:className "problem"}
                                (om/children this)
                                (dom/a #js {:className "btn_primary"
                                            :onClick   #(play-sound this)}
                                  "Play Again"))))
                          (dom/div #js {:id "hintsarea"}))
                        (dom/div #js {:id "answer_area_wrap"}
                          (dom/div #js {:id "answer_area"}
                            (dom/form #js {:id "answerform" :name "answerform" :onSubmit #(do
                                                                                           (om/transact! this `[(dcex/check-answer)])
                                                                                           (.preventDefault %))}
                              (dom/div #js {:className "info-box" :id "answercontent"}
                                (dom/span #js {:className "info-box-header"}
                                  "Answer")
                                (dom/div #js {:className "fancy-scrollbar" :id "solutionarea"}
                                  (case opt-type
                                    :text
                                    (dom/input #js {:type     "text"
                                                    :value    (or ex-answer "")
                                                    :onChange #(um/set-string! this ::ex-answer :event %)})

                                    :select
                                    (dom/ul nil
                                      (for [[value label] options]
                                        (dom/li #js {:key value}
                                          (dom/label nil
                                            (dom/input #js {:type     "radio" :name "exercise-answer"
                                                            :checked  (= ex-answer value)
                                                            :value    value
                                                            :onChange #(um/set-string! this ::ex-answer :event %)})
                                            (dom/span #js {:className "value"} label)))))))
                                (dom/div #js {:className "answer-buttons"}
                                  (dom/div #js {:className "check-answer-wrapper"}
                                    (dom/input #js {:className "simple-button green" :type "button" :value "Check Answer"
                                                    :onClick   #(om/transact! this `[(dcex/check-answer)])}))
                                  (dom/input #js {:className "simple-button green" :id "next-question-button" :name "correctnextbutton" :style #js {:display "none"} :type "button" :value "Correct! Next Question..."})
                                  (dom/div #js {:id "positive-reinforcement" :style #js {:display "none"}}
                                    (dom/img #js {:src "/images/face-smiley.png"})))))))
                        (dom/div #js {:style #js {:clear "both"}})))))))))))))

(def exercise (om/factory Exercise))

(defn rand-direction [] (rand-nth [1 -1]))

(s/fdef rand-direction
  :args (s/cat)
  :ret #{-1 1})

(defn vary-pitch [{:keys [::pitch ::variation ::direction]}]
  (let [direction (or direction [-1 1])
        a (descriptor->value pitch)
        b (+ a (* (descriptor->value variation)
                  (descriptor->value direction)))]
    [a b]))

(s/fdef vary-pitch
  :args (s/cat :data (s/keys :req [::pitch ::variation]))
  :ret (s/tuple ::audio/semitone ::audio/semitone))

(om/defui ^:once PitchDetection
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name      "pitch-detection"
         ::options   [["lower" "Lower"] ["higher" "Higher"]]
         ::pitch     ["C3" ".." "B5"]
         ::variation 24}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [[a b :as notes] (vary-pitch props)]
      (assoc props
        ::notes notes
        ::correct-answer (if (< a b) "higher" "lower"))))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "You will hear two notes. Is the second note lower or higher in pitch?"))))

(om/defui ^:once IdentifyOctaves
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name      "identify-octaves"
         ::options   [["yes" "Yes"] ["no" "No"]]
         ::pitch     ["C3" ".." "B5"]
         ::variation [3 5 6 7 8 9 12 15 16]}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [[a b :as notes] (vary-pitch props)
          distance (- b a)
          octave? (zero? (mod distance 8))]
      (assoc props
        ::notes notes
        ::correct-answer (if octave? "yes" "no"))))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "You will hear two notes. Are they an octave apart?"))))

(om/defui ^:once ReadingMusic
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name    "reading-music"
         ::options ::option-type-text}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [order ["E" "F" "G" "A" "B" "C" "D" "E" "F"]
          pos (rand-int 9)
          note (get order pos)
          notes [(str note (cond-> 3
                             (> pos 4) inc))]]
      (assoc props
        ::read-note (inc pos)
        ::notes notes
        ::correct-answer (.toLowerCase note))))

  Object
  (render [this]
    (let [{:keys [::read-note] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} "Enter the letter name of the note displayed below. Please use a lower case letter (e.g. e, f or c).")
        (dom/div #js {:key "img"}
          (dom/img #js {:src (str "/img/trebleclefimages/" read-note ".jpg")}))))))

(def INTERVAL-NAMES
  {2  "Major 2nd"
   4  "Major 3rd"
   5  "Perfect 4th"
   7  "Perfect 5th"
   9  "Major 6th"
   11 "Major 7th"
   12 "Octave"})

(om/defui ^:once Intervals
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (let [intervals (get props ::intervals [12 7 4 5 9 2 11])]
        (merge
          (uc/initial-state Exercise nil)
          {::name      "intervals"
           ::options   (mapv #(vector (str %) (INTERVAL-NAMES %)) intervals)
           ::pitch     ["C3" ".." "B5"]
           ::variation intervals}
          props))))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [[a b :as notes] (vary-pitch props)
          distance (js/Math.abs (- b a))]
      (assoc props
        ::notes notes
        ::correct-answer (str distance))))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "You will hear two notes - what is their interval?"))))

(defmulti slug->exercise identity)

(defmethod slug->exercise :default [_] nil)

(defmethod slug->exercise "pitch-1" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [12 ".." 24]}})

(defmethod slug->exercise "pitch-2" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [8 ".." 16]}})

(defmethod slug->exercise "pitch-3" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [1 ".." 9]}})

(defmethod slug->exercise "identifying-octaves" [name]
  {::name  name
   ::class IdentifyOctaves
   ::props {}})

(defmethod slug->exercise "treble-clef-reading" [name]
  {::name  name
   ::class ReadingMusic
   ::props {}})

(defmethod slug->exercise "intervals-1" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-2" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-3" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-4" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-5" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-6" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2 11]
            ::pitch     ["C3" "C4" "C5"]
            ::direction 1}})

(defmethod slug->exercise "intervals-7" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7]
            ::direction 1}})

(defmethod slug->exercise "intervals-8" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4]
            ::direction 1}})

(defmethod slug->exercise "intervals-9" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5]
            ::direction 1}})

(defmethod slug->exercise "intervals-10" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9]
            ::direction 1}})

(defmethod slug->exercise "intervals-11" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2]
            ::direction 1}})

(defmethod slug->exercise "intervals-12" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2 11]
            ::direction 1}})

(defmethod slug->exercise "intervals-13" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-14" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-15" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-16" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-17" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-18" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2 11]
            ::pitch     ["C3" "C4" "C5"]
            ::direction -1}})

(defmethod slug->exercise "intervals-19" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7]
            ::direction -1}})

(defmethod slug->exercise "intervals-20" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4]
            ::direction -1}})

(defmethod slug->exercise "intervals-21" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5]
            ::direction -1}})

(defmethod slug->exercise "intervals-22" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9]
            ::direction -1}})

(defmethod slug->exercise "intervals-23" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2]
            ::direction -1}})

(defmethod slug->exercise "intervals-24" [name]
  {::name  name
   ::class Intervals
   ::props {::intervals [12 7 4 5 9 2 11]
            ::direction -1}})
