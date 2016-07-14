(ns daveconservatoire.site.ui-cards
  (:require [devcards.core :refer-macros [defcard deftest dom-node]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui :as ui]
            [untangled.client.core :as uc]))

(defcard button-cards
  (fn [_ _]
    (ui/button {:color "red"} "Content")))

(def lesson
  {:db/table           :lesson,
   :lesson/playlist-items
                       [{:db/table            :playlist-item,
                         :db/id               54,
                         :youtube/id          "dbAF0tWX53Q",
                         :playlist-item/title "Sonata - 1",
                         :playlist-item/text
                                              "So it is time to sit back, relax and listen to some music in the forms that we have just discussed.  This sonata was written by Mozart and is performed here by Chinese pianist Lang Lang.  This is Mozart's Sonata in B flat Major K.333.  We would expect opening movements of sonatas to contain some exciting fast playing and this is certainly the case here with lively scale passages in the right hand."}
                        {:db/table            :playlist-item,
                         :db/id               55,
                         :youtube/id          "bh0jhUw7eaM",
                         :playlist-item/title "Sonata - 2",
                         :playlist-item/text
                                              ". . . and so on to the second movement.  The music is much slower, graceful and sustained than the opening movement. "}
                        {:db/table            :playlist-item,
                         :db/id               56,
                         :youtube/id          "BNPyUz82JZU",
                         :playlist-item/title "Sonata - 3",
                         :playlist-item/text
                                              "Returning to the fast lively music of the opening Mozart completes this sonata with a range of flourishes that show off the piano as a very versatile and expressive instrument.  So across this piece we have seen the fast - slow - fast arrangement of movements which is so common in music of this late 18th, early 19th century period."}
                        {:db/table            :playlist-item,
                         :db/id               57,
                         :youtube/id          "PUfw5coaVPI",
                         :playlist-item/title "Concerto - 1",
                         :playlist-item/text
                                              "This is the concerto that Dimitri Shostakovich wrote as a 19th birthday present to his son Maxim.  Just as in sonatas many concertos being with a fast opening movement.  Listen out for the balance between solo playing from the pianists, ensemble playing from the orchestra and section where the two come together."}
                        {:db/table            :playlist-item,
                         :db/id               58,
                         :youtube/id          "OjPFSCW21j0",
                         :playlist-item/title "Concerto - 2 ",
                         :playlist-item/text
                                              "This beautiful slow movement is one of Shostakovich's most famous works, showing how sometimes the best music can be incredibly simple.  The piano really leads the rest of the players in shaping the expression and movement in the music."}
                        {:db/table            :playlist-item,
                         :db/id               59,
                         :youtube/id          "6CKqobY7l84",
                         :playlist-item/title "Concerto - 3",
                         :playlist-item/text
                                              "Maxim Shostakovich must have been an excellent pianist as he performed this piece only a matter of days after his birthday.  You can hear the dance rhythms alongside dazzling virtuoso passages from the piano, and this piece has one of the most exciting endings I know!"}
                        {:db/table            :playlist-item,
                         :db/id               60,
                         :youtube/id          "7MqrBauptrE",
                         :playlist-item/title "Symphony",
                         :playlist-item/text
                                              "Hope you're sitting comfortably as we listen to a symphony by Ludwig van Beethoven, his seventh.  This is a long piece and it might be better to listen to it in small sections, rather than all the way through in one go.  There is lots I could say about the music, but I think it best for you to listen and discover it for yourself.  Try and see if you can spot as the music changes mood and character throughout the work.  Enjoy!"}],
   :lesson/description "",
   :lesson/type        :lesson.type/playlist,
   :lesson/topic
                       {:topic/course
                                     {:course/title "Music:  A Beginner's Guide",
                                      :db/table     :course,
                                      :course/topics
                                                    [{:db/table    :topic,
                                                      :db/id       2,
                                                      :topic/title "Getting Started",
                                                      :url/slug    "getting-started"}
                                                     {:db/table    :topic,
                                                      :db/id       3,
                                                      :topic/title "Pitch",
                                                      :url/slug    "pitch"}
                                                     {:db/table    :topic,
                                                      :db/id       4,
                                                      :topic/title "Scales",
                                                      :url/slug    "scales"}
                                                     {:db/table    :topic,
                                                      :db/id       7,
                                                      :topic/title "Harmony",
                                                      :url/slug    "harmony"}
                                                     {:db/table    :topic,
                                                      :db/id       5,
                                                      :topic/title "Rhythm",
                                                      :url/slug    "rhythm"}
                                                     {:db/table    :topic,
                                                      :db/id       29,
                                                      :topic/title "Key",
                                                      :url/slug    "key"}
                                                     {:db/table    :topic,
                                                      :db/id       14,
                                                      :topic/title "Intervals",
                                                      :url/slug    "intervals"}
                                                     {:db/table    :topic,
                                                      :db/id       8,
                                                      :topic/title "The Circle of Fifths",
                                                      :url/slug    "the-circle-of-fifths"}
                                                     {:db/table    :topic,
                                                      :db/id       9,
                                                      :topic/title "Modes",
                                                      :url/slug    "modes"}
                                                     {:db/table    :topic,
                                                      :db/id       28,
                                                      :topic/title "Cadences",
                                                      :url/slug    "cadences"}
                                                     {:db/table    :topic,
                                                      :db/id       10,
                                                      :topic/title "Form",
                                                      :url/slug    "form"}
                                                     {:db/table    :topic,
                                                      :db/id       11,
                                                      :topic/title "Articulation",
                                                      :url/slug    "articulation"}
                                                     {:db/table    :topic,
                                                      :db/id       12,
                                                      :topic/title "Timbre",
                                                      :url/slug    "timbre"}
                                                     {:db/table    :topic,
                                                      :db/id       13,
                                                      :topic/title "Instruments",
                                                      :url/slug    "instruments"}
                                                     {:db/table    :topic,
                                                      :db/id       16,
                                                      :topic/title "Texture",
                                                      :url/slug    "texture"}
                                                     {:db/table    :topic,
                                                      :db/id       15,
                                                      :topic/title "Ensembles",
                                                      :url/slug    "ensembles"}
                                                     {:db/table    :topic,
                                                      :db/id       17,
                                                      :topic/title "Music History",
                                                      :url/slug    "music-history"}],
                                      :db/id        7},
                        :db/table    :topic,
                        :topic/lessons
                                     [{:db/table     :lesson,
                                       :db/id        45,
                                       :lesson/title "European Forms Playlist",
                                       :url/slug     "european-forms-playlist"}
                                      {:db/table     :lesson,
                                       :db/id        48,
                                       :lesson/title "Exercise: Reading the Treble Clef",
                                       :url/slug     "treble-clef-reading"}
                                      {:db/table     :lesson,
                                       :db/id        88,
                                       :lesson/title "Pitch and Octaves",
                                       :url/slug     "pitch-and-octaves"}
                                      {:db/table     :lesson,
                                       :db/id        109,
                                       :lesson/title "Major Triads",
                                       :url/slug     "major-triads"}
                                      {:db/table     :lesson,
                                       :db/id        118,
                                       :lesson/title "Homophonic Texture",
                                       :url/slug     "homophonic-texture"}
                                      {:db/table     :lesson,
                                       :db/id        193,
                                       :lesson/title "Exercise: Octave and Perfect 5th",
                                       :url/slug     "intervals-1"}
                                      {:db/table     :lesson,
                                       :db/id        229,
                                       :lesson/title "Question 3 - Scales",
                                       :url/slug     "grade1-q3"}
                                      {:db/table     :lesson,
                                       :db/id        255,
                                       :lesson/title "Using Letter Names",
                                       :url/slug     "using-letter-names"}
                                      {:db/table     :lesson,
                                       :db/id        268,
                                       :lesson/title "Finding Relative Majors and Minors",
                                       :url/slug     "finding-relative-majors-minors"}
                                      {:db/table     :lesson,
                                       :db/id        270,
                                       :lesson/title "Listening to Pitches (easy)",
                                       :url/slug     "pitch-1"}
                                      {:db/table     :lesson,
                                       :db/id        271,
                                       :lesson/title "Listening to Pitches (medium)",
                                       :url/slug     "pitch-2"}
                                      {:db/table     :lesson,
                                       :db/id        272,
                                       :lesson/title "Listening to Pitches (hard)",
                                       :url/slug     "pitch-3"}],
                        :url/slug    "pitch",
                        :db/id       3,
                        :topic/title "Pitch"},
   :db/id              45})

(defcard lesson-playlist
  (dom-node
    (fn [_ node]
      (uc/mount (uc/new-untangled-test-client :initial-state lesson)
                ui/LessonPlaylist node))))
