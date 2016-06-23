(ns daveconservatoire.models
  (:require [cljs.spec :as s]))

(s/def :db/id pos-int?)
(s/def :url/slug string?)
(s/def :ordering/position pos-int?)
(s/def :youtube/id string?)

(s/def :course/title string?)
(s/def :course/description string?)
(s/def :course/author string?)

(s/def :model/course
  (s/keys :opt [:db/id :url/slug :ordering/position
                :course/title :course/description :course/author]))

(s/def :topic/title string?)
(s/def :topic/colour string?)
(s/def :topic/course-id :db/id)
(s/def :topic/course :model/course)

(s/def :model/topic
  (s/keys :opt [:db/id :url/slug :ordering/position
                :topic/title :topic/colour :topic/course]))

(s/def :lesson/title string?)
(s/def :lesson/description string?)
(s/def :lesson/keywords (s/coll-of string? []))
(s/def :lesson/file-type string?)

(s/def :model/lesson (s/keys :opt [:db/id :url/slug :youtube/id
                                   :lesson/title :lesson/description]))
