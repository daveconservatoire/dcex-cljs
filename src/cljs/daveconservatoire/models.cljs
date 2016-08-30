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
(s/def :course/topics (s/every :model/topic))
(s/def :course/lessons (s/every :model/lesson))

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
(s/def :lesson/type #{:lesson.type/video
                      :lesson.type/exercise
                      :lesson.type/playlist})
(s/def :lesson/topic-id :db/id)
(s/def :lesson/topic :model/topic)
(s/def :lesson/course-id :db/id)
(s/def :lesson/course :model/course)
(s/def :lesson/playlist-items (s/coll-of :model/playlist-item))

(s/def :model/lesson
  (s/keys :opt [:db/id :url/slug :youtube/id
                :lesson/title :lesson/description :lesson/keywords
                :lesson/type :lesson/topic-id :lesson/topic
                :lesson/course-id :lesson/course]))

(s/def :playlist-item/title string?)
(s/def :playlist-item/text string?)
(s/def :playlist-item/credit string?)
(s/def :playlist-item/lesson-id :db/id)
(s/def :playlist-item/lesson :model/lesson)

(s/def :model/playlist-item
  (s/keys :opt [:db/id :youtube/id :playlist-item/title
                :playlist-item/text :playlist-item/credit]))

(s/def :user/name string?)
(s/def :user/email string?)
(s/def :user/about string?)

(s/def :model/user
  (s/keys :opt [:db/id :user/name :user/email]))

(s/def :user-view/user-id :db/id)
(s/def :user-view/user :model/user)
(s/def :user-view/lesson-id :db/id)
(s/def :user-view/lesson :model/lesson)
(s/def :user-view/status any?)
(s/def :user-view/position any?)
(s/def :user-view/timestamp any?)

(s/def :model/user-view
  (s/keys :opt [:db/id :user-view/status :user-view/position :user-view/timestamp
                :user-view/user-id :user-view/user
                :user-view/lesson-id :user-view/lesson]))
