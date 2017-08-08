(ns daveconservatoire.site.ui.exercises
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [common.template :as template]
            [fulcro.client.core :as uc]
            [fulcro.client.mutations :as um]
            [cljs.spec.alpha :as s]
            [daveconservatoire.audio.core :as audio]
            [daveconservatoire.site.ui.vexflow :as vf]
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

(s/def ::hints (s/coll-of ::template/fragment))
(s/def ::hints-used nat-int?)

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

(s/def ::read-note-order (s/coll-of string? :kind vector?))
(s/def ::read-note-prefix string?)

(s/def ::read-music-props
  (s/merge ::ex-props
           (s/keys :req [::read-note-order ::read-note-prefix])))

(om/defui ^:once ProgressBar
  Object
  (render [this]
    (let [{::keys [progress-value progress-total] :as props} (om/props this)
          pct (-> (/ progress-value progress-total) (* 100))]
      (dom/div (u/props->html {:className "progress progress-striped active success" :style {:margin 20}}
                              props)
        (dom/div #js {:className "bar" :style #js {:width (str pct "%")}})))))

(def progress-bar (om/factory ProgressBar))

(defn answer-label [{::keys [correct-answer options]}]
  (-> (into {} options) (get correct-answer)))

(s/fdef progress-bar
        :args (s/cat :props (s/keys :opt [::progress-total ::progress-value]))
        :ret ::react-component)

(defn note->node [note]
  {::audio/node-gen (->> note audio/semitone->note (get @audio/*sound-library*))})

(defn prepare-notes [notes]
  (->> notes
       (into [] (map (fn [s]
                       (let [[note duration] (if (vector? s) s [s 2])]
                         (-> (note->node note)
                             (assoc ::audio/duration duration))))))))

(defn play-notes [notes]
  (let [nodes (prepare-notes notes)]
    (audio/play-sequence nodes {::audio/time (audio/current-time)})))

(defn play-sound [props]
  ((-> props ::play-notes) (-> props ::notes)))

(om/defui UserScore
  static om/IQuery
  (query [_] [:db/table :db/id :user/score])

  static om/Ident
  (ident [_ props] (u/model-ident props)))

(defmethod um/mutate 'dcex/request-hint
  [{:keys [state ref]} _ _]
  {:action
   (fn []
     (swap! state #(-> (update-in % (conj ref ::hints-used) inc)
                       (assoc-in (conj ref ::streak-count) 0))))})

(defmethod um/mutate 'dcex/check-answer
  [{:keys [state ref]} _ _]
  {:action
   (fn []
     (let [{::keys [ex-answer correct-answer streak-count class last-error] :as props} (get-in @state ref)]
       (if (and ex-answer (not= ex-answer ""))
         (if (= ex-answer correct-answer)
           (let [next-streak (inc streak-count)
                 new-props   (if last-error
                               (assoc props ::streak-count next-streak
                                            ::last-error false)
                               (new-round class
                                 (merge props
                                        {::streak-count next-streak
                                         ::hints-used   0
                                         ::ex-answer    nil})))]
             (swap! state assoc-in ref new-props)
             (play-sound new-props))
           (swap! state update-in ref assoc ::streak-count 0 ::last-error true)))))

   :remote
   (let [{::keys [name streak-count ex-total-questions hints-used]} (get-in @state ref)]
     (cond
       (= streak-count ex-total-questions)
       (-> (om/query->ast `[(exercise/score-master {:url/slug ~name :user-activity/hints-used ~hints-used})])
           :children first)

       (> streak-count 0)
       (-> (om/query->ast `[(exercise/score {:url/slug ~name :user-activity/hints-used ~hints-used})])
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
                        ::streak-count       0
                        ::hints-used         0
                        ::play-notes         play-notes})

  static om/IQuery
  (query [_] ['*])

  Object
  (render [this]
    (let [{::keys [options ex-answer ex-total-questions streak-count notes hints hints-used]
           :as    props} (om/props this)
          [opt-type _] (s/conform ::options options)
          parent       (om/parent this)
          hints        (if hints (hints props) [])
          check-answer #(do
                          (om/transact! parent `[(dcex/check-answer)
                                                 (fulcro/load {:query [{:app/me ~(om/get-query UserScore)}] :marker false})]))]
      (s/assert ::ex-props props)
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
                      (if (completed? props)
                        (dom/div #js {:key "done" :className "alert alert-success masterymsg"}
                          (dom/strong nil "Well done! ")
                          "You've mastered this skill - time to move on to something new"))
                      (dom/div #js {:id "problem-and-answer" :className "framework-khan-exercises"}
                        (dom/div #js {:id "problemarea"}
                          (dom/div #js {:id "workarea"}
                            (dom/div #js {:id "problem-type-or-description"}
                              (dom/div #js {:className "problem"}
                                (om/children this)
                                (if (seq notes)
                                  (dom/a #js {:className "btn_primary"
                                              :onClick   #(play-sound props)}
                                    "Play Again")))))
                          (dom/div #js {:id "hintsarea"}
                            (for [[hint i] (map vector (take hints-used hints) (range))]
                              (dom/p #js {:key i} hint))))
                        (dom/div #js {:id "answer_area_wrap"}
                          (dom/div #js {:id "answer_area"}
                            (dom/form #js {:id "answerform" :name "answerform" :onSubmit #(do
                                                                                            (check-answer)
                                                                                            (.preventDefault %))}
                              (dom/div #js {:className "info-box" :id "answercontent"}
                                (dom/span #js {:className "info-box-header"}
                                  "Answer")
                                (dom/div #js {:className "fancy-scrollbar" :id "solutionarea"}
                                  (case opt-type
                                    :text
                                    (dom/input #js {:type     "text"
                                                    :value    (or ex-answer "")
                                                    :onChange #(um/set-string! parent ::ex-answer :event %)})

                                    :select
                                    (dom/ul nil
                                      (for [[value label] options]
                                        (dom/li #js {:key value}
                                          (dom/label nil
                                            (dom/button #js {:type    "button"
                                                             :onClick #(do
                                                                         (um/set-string! parent ::ex-answer :value value)
                                                                         (check-answer))}
                                              label)))))))
                                (if (contains? #{:text} opt-type)
                                  (dom/div #js {:className "answer-buttons"}
                                    (dom/div #js {:className "check-answer-wrapper"}
                                      (dom/input #js {:className "simple-button green" :type "button" :value "Check Answer"
                                                      :onClick   check-answer}))
                                    (dom/input #js {:className "simple-button green"
                                                    :id        "next-question-button"
                                                    :name      "correctnextbutton"
                                                    :style     #js {:display "none"}
                                                    :type      "button"
                                                    :value     "Correct! Next Question..."})
                                    (dom/div #js {:id "positive-reinforcement" :style #js {:display "none"}}
                                      (dom/img #js {:src "/images/face-smiley.png"})))))

                              (if (> (count hints) 0)
                                (dom/div #js {:className "info-box hint-box"}
                                  (dom/span #js {:className "info-box-header"}
                                    "Need help?")
                                  (dom/div #js {:id "get-hint-button-container"}
                                    (dom/input #js {:className "simple-button orange full-width"
                                                    :type      "button"
                                                    :disabled  (= (count hints) hints-used)
                                                    :onClick   #(om/transact! parent `[(dcex/request-hint)])
                                                    :value     (if (zero? hints-used)
                                                                 "I'd like a hint"
                                                                 (str "I'd like another hint (" (- (count hints) hints-used) " hints left)"))}))
                                  (dom/span #js {:id "hint-remainder"}))))))
                        (dom/div #js {:style #js {:clear "both"}})))))))))))))

(def exercise (om/factory Exercise))

(defn rand-direction [] (rand-nth [1 -1]))

(s/fdef rand-direction
        :args (s/cat)
        :ret #{-1 1})

(defn vary-pitch [{:keys [::pitch ::variation ::direction]}]
  (let [direction (or direction [-1 1])
        a         (descriptor->value pitch)
        b         (+ a (* (descriptor->value variation)
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
         ::variation [3 5 6 7 8 9 15 16 12 12 12 12 12 12 12]
         ::hints     (fn [props]
                       ["Does this sound like two flavours of the same thing?"
                        "Can you sing the notes between the two pitches?  Do they make a complete scale?"
                        (str "The answer is " (get props ::correct-answer))])}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [[a b :as notes] (vary-pitch props)
          distance (- b a)
          octave?  (= (js/Math.abs distance) 12)]
      (assoc props
        ::notes notes
        ::correct-answer (if octave? "yes" "no"))))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "You will hear two notes. Are they an octave apart?"))))

(def notes-treble
  {::read-note-order  ["E" "F" "G" "A" "B" "C" "D" "E" "F"]
   ::read-note-prefix "trebleclefimages"
   ::hints            (fn [props]
                        ["Does this note sit on a line or in a space?"
                         "\"FACE in the space\" and \"Every Green Bus Drives Fast\""
                         (str "This note is " (get props ::correct-answer))])})

(def notes-bass
  {::read-note-order  ["G" "A" "B" "C" "D" "E" "F" "G" "A"]
   ::read-note-prefix "bassclefimages"
   ::hints            (fn [props]
                        ["Does this note sit on a line or in a space?"
                         "\"All Cows Eat Grass\" and \"Good Boys Deserve Fresh Apples\""
                         (str "This note is " (get props ::correct-answer))])})

(def notes-grand-staff
  {::read-note-order  ["G" "A" "B" "C" "D" "E" "F" "G" "A" "B" #_"TREBLE" "D" "E" "F" "G" "A" "B" "C" "D" "E" "F" "C"]
   ::read-note-prefix "grandstaffimages"
   ::hints            (fn [props]
                        ["Identify if the note belongs to treble clef or the bass clef."
                         "The treble clef is the upper staff, the bass clef is the lower staff."
                         (str "This note is " (get props ::correct-answer))])})

(defn rand-int-new [n old]
  "Generate a new random number like rand-int, if number is equals to `old` a new
  number will be generated until it's different."
  (loop []
    (let [x (rand-int n)]
      (if (= x old)
        (recur)
        x))))

(om/defui ^:once ReadingMusic
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name    "reading-music"
         ::options ::option-type-text} props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [{::keys [read-note-order read-note]} props
          pos    (rand-int-new (count read-note-order) read-note)
          note   (get read-note-order pos)
          octave (if (> pos 4) 4 3)
          notes  [(str note octave)]]
      (assoc props
        ::read-note pos
        ::notes notes
        ::correct-answer (.toLowerCase note))))

  Object
  (render [this]
    (let [{::keys [read-note read-note-prefix] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} "Enter the letter name of the note displayed below. Please use a lower case letter (e.g. e, f or c).")
        (dom/div #js {:key "img"}
          (dom/img #js {:src (str "/img/" read-note-prefix "/" (inc read-note) ".jpg")}))))))

(def available-rhytm-notes
  {1 "quarter"
   2 "half"
   4 "whole"})

(om/defui ^:once RhythmMath
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name    "rhythm-math"
         ::options ::option-type-text}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [notes (->> (repeatedly #(rand-nth (keys available-rhytm-notes)))
                     (take 3))]
      (assoc props
        ::rhytm-notes notes
        ::notes []
        ::correct-answer (str (reduce + notes)))))

  Object
  (render [this]
    (let [{:keys [::rhytm-notes] :as props} (om/props this)
          display-notes (->> rhytm-notes
                             (map available-rhytm-notes)
                             (interpose "plus"))]
      (exercise props
        (dom/p #js {:key "p"} "Solve this problem, adding together the lengths in beats of the notes shown.")
        (dom/div #js {:key "images"}
          (for [[i note] (map vector (range) display-notes)]
            (dom/span #js {:key i}
              (dom/img #js {:src (str "/img/rhythmimages/" note ".jpg")}))))))))

(def uk-symbols {"semibreve"                   4
                 "minim"                       2
                 "crochet"                     1
                 "dotted minim"                3
                 "dotted semibreve"            6
                 "minim tied to a crochet"     3
                 "semibreve tied to a minim"   6
                 "semibreve tied to a crochet" 5})


(om/defui ^:once Quiz
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name    "uk-note-names"
         ::options ::option-type-text}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [sym (rand-nth (keys (get props ::quiz-map)))]
      (assoc props
        ::quiz-question sym
        ::notes []
        ::correct-answer (str (get (get props ::quiz-map) sym)))))

  Object
  (render [this]
    (let [{:keys [::quiz-question] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} (str "For how many beats does a " quiz-question " last?"))))))

(def type->arrengement
  {"major" audio/MAJOR-TRIAD
   "minor" audio/MINOR-TRIAD})

(om/defui ^:once ChordType
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name       "rhythm-math"
         ::options    [["major" "Major"] ["minor" "Minor"]]
         ::play-notes (fn [notes]
                        (let [time (audio/current-time)]
                          (audio/global-stop-all)
                          (doseq [note notes]
                            (audio/play (-> (note->node note)
                                            (assoc ::audio/time time))))))}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [type      (rand-nth ["major" "minor"])
          base-note (descriptor->value ["C3" ".." "F3"])]
      (assoc props
        ::notes (audio/chord base-note (type->arrengement type))
        ::correct-answer type)))

  Object
  (render [this]
    (let [{:keys [] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} "Tell which type of chord is being played.")))))

(def rhythm-combinations
  [["w"]
   ["h" "h"]
   ["h" "q" "q"]
   ["q" "h" "q"]
   ["q" "q" "h"]
   ["q" "q" "q" "q"]])

(defn duration->seconds
  ([v] (duration->seconds v 60))
  ([v bpm]
   (let [whole (* (/ bpm 60) 4)]
     (case v
       "w" (* whole 1)
       "h" (* whole 0.5)
       "q" (* whole 0.25)))))

(defn to-note [duration] {::vf/keys ["b/4"] ::vf/duration duration})

(defn random-rhythm []
  (->> (rand-nth rhythm-combinations)
       (map to-note)))

(defn random-bars [] (repeatedly #(hash-map ::vf/notes (random-rhythm))))

(def rhytm-metronome (flatten1 (repeat 5 [["high" 1] ["low" 1] ["low" 1] ["low" 1]])))

(def rhythm-options
  {0 "A" 1 "B" 2 "C" 3 "D"})

(om/defui ^:once RhythmReading
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name       "rhythm-reading"
         ::options    (into [] (map (fn [[k v]] [(str k) v])) rhythm-options)
         ::play-notes (let [last-play       (atom [])
                            time-multiplier 0.7]
                        (fn [notes]
                          (let [time  (audio/current-time)
                                nodes (map #(update % ::audio/duration (partial * time-multiplier)) (prepare-notes notes))
                                metro (map #(update % ::audio/duration (partial * time-multiplier)) (prepare-notes rhytm-metronome))]
                            (run! audio/stop @last-play)
                            (reset! last-play (concat (audio/play-sequence metro {::audio/time time})
                                                      (audio/play-sequence nodes {::audio/time (+ time (* 4 time-multiplier))}))))))
         ::hints      (fn [props]
                        ["Choose one bar and really focus on listening to that when the music plays."
                         "Clap along with the version you chose as the music plays. "
                         (str "The correct version is " (answer-label props))])}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [rhytms (partition 4 (take 16 (random-bars)))
          idx    (rand-int 4)
          notes  (->> (nth rhytms idx)
                      (map ::vf/notes)
                      (flatten)
                      (map (comp #(vector "B4" %)
                                 duration->seconds
                                 ::vf/duration)))]
      (assoc props
        ::rhytms rhytms
        ::notes notes
        ::correct-answer (str idx))))

  Object
  (render [this]
    (let [{:keys [::rhytms] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} "Listen the sound clip and choose the correct rhythm. You will hear a metronome count in and then the piano will play the rhythm over this.")
        (dom/div #js {:key "scores"}
          (for [[i bars] (map vector (range) rhytms)]
            (dom/div #js {:style #js {:display "flex" :alignItems "center"}}
              (dom/div #js {:style #js {:fontWeight "bold" :fontSize 23 :width 30}} (get rhythm-options i))
              (vf/score #::vf {:width 470 :height 110 :clef ::vf/treble :_/react-key (hash bars)
                               :bars  bars}))))))))

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
   ::props notes-treble})

(defmethod slug->exercise "bass-clef-reading" [name]
  {::name  name
   ::class ReadingMusic
   ::props notes-bass})

(defmethod slug->exercise "grand-staff-reading" [name]
  {::name  name
   ::class ReadingMusic
   ::props notes-grand-staff})

(defmethod slug->exercise "rhythm-maths" [name]
  {::name  name
   ::class RhythmMath
   ::props {}})

(defmethod slug->exercise "rhythm-reading" [name]
  {::name  name
   ::class RhythmReading
   ::props {}})

(defmethod slug->exercise "chord-recognition" [name]
  {::name  name
   ::class ChordType
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

(defmethod slug->exercise "euro-names" [name]
  {::name  name
   ::class Quiz
   ::props {::quiz-map uk-symbols}})
