(ns daveconservatoire.models
  (:require [cljs.spec :as s]))

(s/def :db/id pos-int?)
(s/def :db/table keyword?)
(s/def :url/slug string?)
(s/def :ordering/position pos-int?)
(s/def :youtube/id string?)

(s/def :course/title string?)
(s/def :course/description string?)
(s/def :course/author string?)
(s/def :course/topics (s/coll-of :model/topic []))
(s/def :course/lessons (s/coll-of :model/lesson []))

(s/def :model/course
  (s/keys :opt [:db/id :url/slug :ordering/position
                :course/title :course/description :course/author
                :course/topics :course/lessons]))

(s/def :topic/title string?)
(s/def :topic/colour string?)
(s/def :topic/course-id :db/id)
(s/def :topic/course :model/course)

(s/def :model/topic
  (s/keys :opt [:db/id :url/slug :ordering/position
                :topic/title :topic/colour :topic/course-id :topic/course]))

(s/def :lesson/title string?)
(s/def :lesson/description string?)
(s/def :lesson/keywords string?)
(s/def :lesson/type string?)
(s/def :lesson/topic-id :db/id)
(s/def :lesson/topic :model/topic)
(s/def :lesson/course-id :db/id)
(s/def :lesson/course :model/course)

(s/def :model/lesson
  (s/keys :opt [:db/id :url/slug :youtube/id
                :lesson/title :lesson/description :lesson/keywords
                :lesson/type :lesson/topic-id :lesson/topic
                :lesson/course-id :lesson/course]))
