(ns daveconservatoire.site.ui.vexflow-cards
  (:require [devcards.core :refer [defcard deftest dom-node]]
            [cljs.test :refer [is are run-tests async testing]]
            [daveconservatoire.site.ui.vexflow :as v]))

(defcard blank-score
  (fn [_ _]
    (v/score {::v/width 500 ::v/height 150}))
  {})

(defcard single-bar
  (fn [_ _]
    (v/score {::v/width 500 ::v/height 150
              ::v/clef ::v/treble
              ::v/bars [{::v/notes [{::v/keys ["c/5"] ::v/duration "q"}
                                    {::v/keys ["d/5"] ::v/duration "q"}
                                    {::v/keys ["e/5"] ::v/duration "q"}
                                    {::v/keys ["c/4"] ::v/duration "q"}]}]}))
  {})

(defcard multi-bar
  (fn [_ _]
    (v/score {::v/width 500 ::v/height 150
              ::v/clef ::v/treble
              ::v/bars [{::v/notes [{::v/keys ["c/5"] ::v/duration "q"}
                                    {::v/keys ["d/5"] ::v/duration "q"}
                                    {::v/keys ["e/5"] ::v/duration "h"}]}
                        {::v/notes [{::v/keys ["g/5"] ::v/duration "q"}
                                    {::v/keys ["g/5"] ::v/duration "q"}
                                    {::v/keys ["e/5"] ::v/duration "q"}
                                    {::v/keys ["f/5"] ::v/duration "q"}]}]}))
  {})

(defcard multi-bar2
  (fn [_ _]
    (v/score {::v/width 500 ::v/height 150
              ::v/clef ::v/treble
              ::v/bars [{::v/notes [{::v/keys ["c/5"] ::v/duration "q"}
                                    {::v/keys ["d/5"] ::v/duration "q"}
                                    {::v/keys ["e/5"] ::v/duration "q"}
                                    {::v/keys ["c/4"] ::v/duration "q"}]}
                        {::v/notes [{::v/keys ["c/5"] ::v/duration "q"}
                                    {::v/keys ["d/5"] ::v/duration "q"}
                                    {::v/keys ["e/5"] ::v/duration "q"}
                                    {::v/keys ["c/4"] ::v/duration "q"}]}]}))
  {})
