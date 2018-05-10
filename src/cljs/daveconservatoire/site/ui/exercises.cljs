(ns daveconservatoire.site.ui.exercises
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [common.template :as template]
            [fulcro.client.core :as uc]
            [fulcro.client.mutations :as um]
            [cljs.spec.alpha :as s]
            [cljs.core.async :refer [<!]]
            [daveconservatoire.audio.core :as audio]
            [daveconservatoire.site.ui.vexflow :as vf]
            [daveconservatoire.site.ui.util :as u]
            [clojure.test.check.generators]
            [clojure.test.check.properties]
            [cljs.core.async :as async]))

(defprotocol IExercise
  (new-round [this props]))

(defn new-round! [this props]
  (let [props' (new-round this props)]
    (assert (s/valid? (s/keys) props') (s/explain-str (s/keys) props'))
    props'))

(s/def ::progress-value number?)
(s/def ::progress-total number?)
(s/def ::react-component object?)

(s/def ::name string?)
(s/def ::description string?)
(s/def ::option (s/cat :value string? :label string?))
(s/def ::options (s/or :text #{::option-type-text}
                   :select (s/coll-of ::option :kind vector?)))

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
    (let [{:ui/keys [progress-value progress-total] :as props} (om/props this)
          pct (-> (/ progress-value progress-total) (* 100))]
      (dom/div (u/props->html {:className "progress progress-striped active success" :style {:margin 20}}
                 props)
        (dom/div #js {:className "bar" :style #js {:width (str pct "%")}})))))

(def progress-bar (om/factory ProgressBar))

(defn completed? [{:keys [::ex-total-questions ::streak-count]}]
  (>= streak-count ex-total-questions))

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

(defn play-notes [{::keys [notes]}]
  (audio/global-stop-all)
  (let [nodes (prepare-notes notes)]
    (audio/play-sequence nodes {::audio/time (audio/current-time)})))

(defn play-chord [{::keys [notes]}]
  (let [time (audio/current-time)]
    (audio/global-stop-all)
    (doseq [note notes]
      (audio/play (-> (note->node note)
                      (assoc ::audio/time time))))))

(defn play-sound [props]
  ((-> props ::play-notes) props))

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
     (let [{::keys [ex-answer correct-answer streak-count class last-error ex-total-questions] :as props} (get-in @state ref)]
       (if (and ex-answer (seq ex-answer))
         (if (= ex-answer correct-answer)
           (let [next-streak (inc streak-count)
                 new-props (if last-error
                             (assoc props ::streak-count next-streak
                                          ::last-error false
                                          ::confirm-answer true)
                             (new-round class
                               (merge props
                                 {::streak-count   next-streak
                                  ::hints-used     0
                                  ::ex-answer      nil
                                  ::confirm-answer true})))]
             (swap! state assoc-in ref new-props)
             (if-not (completed? new-props)
               (play-sound new-props)))
           (swap! state update-in ref assoc ::streak-count 0 ::last-error false ::ex-answer nil ::confirm-answer false
             (play-sound props)))
         (swap! state update-in ref assoc ::confirm-answer false))))

   :remote
   (let [{::keys [name streak-count ex-total-questions hints-used confirm-answer]} (get-in @state ref)]
     (if confirm-answer
       (cond
         (= streak-count ex-total-questions)
         (-> (om/query->ast `[(exercise/score-master {:url/slug ~name :user-activity/hints-used ~hints-used})])
           :children first)

         (> streak-count 0)
         (-> (om/query->ast `[(exercise/score {:url/slug ~name :user-activity/hints-used ~hints-used})])
           :children first))))})

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



(defn handle-progress [comp progress-chan]
  (go
    (loop []
      (when-let [progress (<! progress-chan)]
        (um/set-value! comp ::custom-sounds-progress progress)
        (recur))))
  progress-chan)

(defn flatten-request [sound-req]
  (into {} (mapcat (fn [[id urls]]
                     (map-indexed #(-> [[id %] %2]) urls)))
        sound-req))

(om/defui ^:once Exercise
  static uc/InitialAppState
  (initial-state [_ _] {::ex-answer              nil
                        ::ex-total-questions     3
                        ::streak-count           0
                        ::hints-used             0
                        ::play-notes             play-notes
                        ::started?               false
                        ::custom-sounds-progress {:ui/progress-value 0
                                                  :ui/progress-total 1}
                        ::custom-sounds-req      nil})

  static om/IQuery
  (query [_] ['*])

  Object
  (componentDidMount [this]
    (let [{::keys [custom-sounds-req]} (om/props this)]
      (if custom-sounds-req
        (go
          (let [progress-chan (handle-progress (om/parent this) (async/chan 10))
                sounds (<! (audio/load-sound-library
                             (flatten-request custom-sounds-req)
                             progress-chan))]
            (um/set-value! (om/parent this) ::custom-sounds sounds))))))

  (render [this]
    (let [{::keys [options ex-answer ex-total-questions streak-count notes hints
                   hints-used started? custom-sounds-req custom-sounds custom-sounds-progress]
           :as    props} (om/props this)
          state        (-> this om/get-reconciler om/app-state deref)
          me           (get-in state (get state :app/me))
          [opt-type _] (s/conform ::options options)
          parent       (om/parent this)
          hints        (if hints (hints props) [])
          check-answer #(do
                          (om/transact! parent `[(dcex/check-answer)
                                                 (fulcro/load {:query [{:app/me ~(om/get-query UserScore)}] :marker false})]))]
      (s/assert ::ex-props props)
      (js/console.log "ENTER EX RENDER!!" me)
      (dom/div #js {:className "lesson-content"}
        (cond
          (not started?)
          (dom/div #js {:style #js {"textAlign" "center"}}
            (dom/h1 nil "Make sure you have your volume up, many of our exercises play sound!")
            (if (and custom-sounds-req (not custom-sounds))
              (dom/div nil
                (some-> custom-sounds-progress progress-bar)
                (dom/div nil "Loading..."))
              (dom/a #js {:className "btn btn-primary"
                          :onClick   (fn [e]
                                       (.preventDefault e)
                                       (um/set-value! parent ::started? true)
                                       (play-sound props))}
                (dom/h1 nil "Start Exercise"))))

          (completed? props)
          (cond
            (= -1 (get me :db/id))
            (dom/div nil "You can login to save this progress!")

            (= "0" (get me :user/subscription-amount))
            (dom/div nil "Please donate! ")

            :else
            (dom/div nil "Congratulations! You've completed this exercise. You Rock!"))

          :else
          (dom/article #js {:className "exercises-content clearfix"}
            (dom/div #js {:className "exercises-body"}
              (dom/div #js {:className "exercises-stack"})
              (dom/div #js {:className "exercises-card current-card"}
                (dom/div #js {:className "current-card-container card-type-problem"}
                  (dom/div #js {:className "current-card-container-inner vertical-shadow" :style #js {"width" "100%"}}
                    (dom/div #js {:className "current-card-contents"}
                      (progress-bar {:ui/progress-value streak-count
                                     :ui/progress-total ex-total-questions})
                      (dom/div #js {:id "problem-and-answer" :className "framework-khan-exercises"}
                        (dom/div #js {:id "problemarea" :style #js {"marginTop" "0"}}
                          (dom/div #js {:id "workarea" :style #js {"backgroundColor" "white" "margin" "5px" "padding" "10px" "border" "1px solid #ddd"}}
                            (dom/div #js {:id "problem-type-or-description"}
                              (dom/div #js {:className "problem"}
                                (om/children this)
                                (if (seq notes)
                                  (dom/a #js {:className "btn btn-primary"
                                              :onClick   #(play-sound props)}
                                    "Play Again")))))
                          (dom/div #js {:id "hintsarea" :style #js{"margin" "0"}}
                            (for [[hint i] (map vector (take hints-used hints) (range))]
                              (dom/p #js {:key i :style #js {"margin" "5px" "padding" "5px" "background" "#eee" "border" "1px #ddd solid"}} hint))))
                        (dom/div #js {:id "answer_area_wrap"}
                          (dom/div #js {:id "answer_area"}
                            (dom/form #js {:id "answerform" :name "answerform" :onSubmit #(do
                                                                                            (check-answer)
                                                                                            (.preventDefault %))}
                              (dom/div #js {:className "info-box" :id "answercontent"}
                                (dom/span #js {:className "info-box-header"}
                                  "Answer")
                                (dom/div #js {:className "fancy-scrollbar" :id "solutionarea"
                                              :style     #js {"borderBottom" "none"}}
                                  (case opt-type
                                    :text
                                    (dom/input #js {:type     "text"
                                                    :value    (or ex-answer "")
                                                    :onChange #(um/set-string! parent ::ex-answer :event %)})

                                    :select
                                    (dom/ul #js {:style #js {"margin" "0"}}
                                      (for [[value label] options]
                                        (dom/li #js {:key value}
                                          (dom/label nil
                                            (dom/button #js {:type      "button"
                                                             :className (if (= ex-answer value) "btn btn-block btn-success" "btn btn-block dc-btn-orange")
                                                             :onClick   #(um/set-string! parent ::ex-answer :value value)}
                                              label)))))))
                                (dom/div #js {:className "answer-buttons"}
                                  (dom/div #js {:className "check-answer-wrapper"}
                                    (dom/input #js {:className "btn btn-success btn-block" :type "button" :value "Check Answer"
                                                    :onClick   check-answer}))
                                  (dom/input #js {:className "btn btn-success btn-block"
                                                  :id        "next-question-button"
                                                  :name      "correctnextbutton"
                                                  :style     #js {:display "none"}
                                                  :type      "button"
                                                  :value     "Continue"})
                                  (dom/div #js {:id "positive-reinforcement" :style #js {:display "none"}}
                                    (dom/img #js {:src "/images/face-smiley.png"}))))

                              (if (> (count hints) 0)
                                (dom/div #js {:className "info-box hint-box"}
                                  (dom/span #js {:className "info-box-header"}
                                    "Need help?")
                                  (dom/div #js {:id "get-hint-button-container"}
                                    (dom/input #js {:className "btn btn-block dc-btn-orange"
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
         ::options ::option-type-text}
        props)))

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
  {"major"            audio/MAJOR-TRIAD
   "minor"            audio/MINOR-TRIAD
   "dominant-seventh" audio/DOMINANT-SEVENTH})

(om/defui ^:once ChordType
  static uc/InitialAppState
  (initial-state [this props]
    (new-round this
      (merge
        (uc/initial-state Exercise nil)
        {::name       "rhythm-math"
         ::options    [["major" "Major"] ["minor" "Minor"] ["dominant-seventh" "Dominant Seventh"]]
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
    (let [type      (rand-nth ["major" "minor" "dominant-seventh"])
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
      (js/console.log "start interval" (assoc props
                                         ::notes notes
                                         ::correct-answer (str distance)))
      (assoc props
        ::notes notes
        ::correct-answer (str distance))))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "You will hear two notes - what is their interval?"))))

(def SCALE-NAMES
  {"Major"          [2 2 1 2 2 2 1]
   "Natural Minor"  [2 1 2 2 1 2 2]
   "Harmonic Minor" [2 1 2 2 1 3 1]
   "Chromatic"      [1 1 1 1 1 1 1 1 1 1 1 1 1]
   "Whole Tone"     [2 2 2 2 2 2]
   "Melodic Minor"  [[2 1 2 2 2 2 1] [2 2 1 2 2 1 2]]})

(s/def ::scale-offset (s/with-gen pos-int? #(s/gen #{1 2})))
(s/def ::scale (s/coll-of ::scale-offset :kind vector? :max-count 12))
(s/def ::full-scale (s/or :regular ::scale :irregular (s/tuple ::scale ::scale)))

(defn build-up-scale [tonic scale]
  (let [[type scale] (s/conform ::full-scale scale)
        scale (if (= :regular type) scale (first scale))]
    (reduce
      (fn [notes offset]
        (conj notes (+ (last notes) offset)))
      [tonic]
      scale)))

(defn build-down-scale [tonic scale]
  (let [[type scale] (s/conform ::full-scale scale)
        scale (if (= :regular type) scale (second scale))]
    (cond-> (reduce
              (fn [notes offset]
                (conj notes (+ (last notes) offset)))
              [tonic]
              scale)
      :regular (-> rseq vec))))

(defn build-scale [tonic scale]
  (->> (concat (build-up-scale tonic scale)
               (next (build-down-scale tonic scale)))
       vec))

(om/defui ^:once Scales
  static uc/InitialAppState
  (initial-state [this props]
    (new-round! this
      (let [scales (get props ::scales)]
        (merge
          (uc/initial-state Exercise nil)
          {::name           "scales"
           ::audio/duration 2
           ::options        (mapv #(vector (str %) (str %)) scales)}
          props))))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [tonic    (descriptor->value ["B2" ".." "F5"])
          duration (::audio/duration props)
          scale    (rand-nth (::scales props))
          notes    (as-> (build-scale tonic (get SCALE-NAMES scale)) <>
                     (mapv vector <> (repeat duration)))]
      (assoc props
        ::notes notes
        ::correct-answer scale)))

  Object
  (render [this]
    (exercise (om/props this)
      (dom/p #js {:key "p"} "What kind of scale do you hear?"))))

(om/defui ^:once SingleSoundListening
  static uc/InitialAppState
  (initial-state [this props]
    (new-round! this
      (merge
        (uc/initial-state Exercise nil)
        {::name              "single-sound"
         ::options           []
         ::notes             ['_]
         ::play-notes        (fn [{::keys [custom-sounds correct-answer custom-sound-idx]}]
                               (audio/global-stop-all)
                               (audio/play {::audio/node-gen #(audio/buffer-node (get custom-sounds [correct-answer custom-sound-idx]))
                                            ::audio/time     (audio/current-time)}))
         ::custom-sounds-req {}}
        props)))

  static om/Ident
  (ident [_ props] [:exercise/by-name (::name props)])

  static om/IQuery
  (query [_] '[*])

  static IExercise
  (new-round [_ props]
    (let [instrument (-> props ::custom-sounds-req keys vec rand-nth)
          sound-idx  (-> props ::custom-sounds-req (get instrument) count rand-int)]
      (assoc props
        ::custom-sound-idx sound-idx
        ::correct-answer instrument)))

  Object
  (render [this]
    (let [{:keys [::question] :as props} (om/props this)]
      (exercise props
        (dom/p #js {:key "p"} question)))))

(defmulti slug->exercise identity)

(defmethod slug->exercise :default [_] nil)

(defmethod slug->exercise "pitch-1" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [12 ".." 24]}
   ::hints (fn [props]
             ["Pitch is the sensation of a note being higher or lower"
              "Does the second note sound higher or lower than the first?"
              (str "The second note is " (answer-label props))])})

(defmethod slug->exercise "pitch-2" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [8 ".." 16]}
   ::hints (fn [props]
             ["Here the distance in pitch is getting smaller, but the same idea applies."
              "Does the second note sound higher or lower than the first?"
              (str "The second note is " (answer-label props))])})

(defmethod slug->exercise "pitch-3" [name]
  {::name  name
   ::class PitchDetection
   ::props {::variation [1 ".." 9]}
   ::hints (fn [props]
             ["Even smaller steps in pitch now."
              "Sometimes the eyebrows test is a good approach. Does the second note make your eyebrows want to go up or down?  I'm not joking!"
              (str "The second note is " (answer-label props))])})

(defmethod slug->exercise "identifying-octaves" [name]
  {::name  name
   ::class IdentifyOctaves
   ::props {}
   ::hints (fn [props]
             ["An octave is a very special sound in music.  What does it sound like?"
              "An octave sounds like two flavours of the same sound.  Is that the case here?"
              (str "Are these notes an octave apart? " (answer-label props) ".")])})

(defmethod slug->exercise "treble-clef-reading" [name]
  {::name  name
   ::class ReadingMusic
   ::props notes-treble})

(defmethod slug->exercise "identifying-scales" [name]
  {::name  name
   ::class Scales
   ::props {::scales ["Major" "Natural Minor"]}})

(defmethod slug->exercise "identifying-scales-2" [name]
  {::name  name
   ::class Scales
   ::props {::scales ["Major" "Natural Minor" "Harmonic Minor"]}})

(defmethod slug->exercise "identifying-scales-3" [name]
  {::name  name
   ::class Scales
   ::props {::scales ["Major" "Natural Minor" "Harmonic Minor" "Melodic Minor"]}})

(defmethod slug->exercise "identifying-scales-4" [name]
  {::name  name
   ::class Scales
   ::props {::scales ["Major" "Natural Minor" "Harmonic Minor" "Melodic Minor" "Whole Tone"]}})

(defmethod slug->exercise "identifying-scales-5" [name]
  {::name  name
   ::class Scales
   ::props {::scales ["Major" "Natural Minor" "Harmonic Minor" "Melodic Minor" "Whole Tone" "Chromatic"]}})

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
   ::props {}}

  {::name  name
   ::class SingleSoundListening
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

(defmethod slug->exercise "test-ex" [name]
  {::name  name
   ::class SingleSoundListening
   ::props {::options           [["violin" "Violin"] ["flute" "Flute"] ["drum" "Drum"]]
            ::custom-sounds-req {"flute"  ["/audio/flute_As4_15_piano_normal"]
                                 "violin" ["/audio/violin_Gs3_15_fortissimo_arco-normal"]
                                 "drum"   ["/audio/0a"
                                           "/audio/1a"
                                           "/audio/2a"]}}})

(defmethod slug->exercise "recognising-chords-1" [name]
  {::name  name
   ::class SingleSoundListening
   ::props {::options           [["major" "Major"] ["minor" "Minor"]]
            ::custom-sounds-req {"major"  ["/audio/chord_recog/1"
                                           "/audio/chord_recog/2"
                                           "/audio/chord_recog/3"
                                           "/audio/chord_recog/4"]
                                 "minor" ["/audio/chord_recog/5"
                                          "/audio/chord_recog/6"
                                          "/audio/chord_recog/7"
                                          "/audio/chord_recog/8"]}
            ::question "What kind of chord is this?  Is it major or minor?"}})

