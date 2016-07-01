(ns daveconservatoire.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [daveconservatoire.site.ui.util :as u]
            [untangled.client.core :as uc]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]))

(s/def ::component om/component?)
(s/def ::button-color #{"yellow" "orange" "redorange" "red"})

(om/defui ^:once Button
  Object
  (render [this]
    (let [{:keys [::button-color]
           :or   {::button-color "orange"} :as props} (om/props this)]
      (dom/a (u/props->html {:href      "#"
                             :className (str "btn dc-btn-" button-color)}
                            props)
        (om/children this)))))

(def button (om/factory Button))

(s/fdef button
  :args (s/cat :props (s/keys :opt [::button-color])
               :children (s/* ::component)))

(om/defui ^:once Link
  Object
  (render [this]
    (dom/a (u/props->html {} (om/props this))
      (om/children this))))

(def link (om/factory Link))

(om/defui ^:once TopicLink
  static om/IQuery
  (query [_] [:topic/title :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/title url/slug]} (om/props this)]
      (button {::r/handler ::r/topic ::r/params {::r/slug slug}} title))))

(def topic-link (om/factory TopicLink {:key-fn :db/id}))

(om/defui ^:once HomeCourse
  static om/IQuery
  (query [_] [:course/title :course/description
              {:course/topics (om/get-query TopicLink)}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [course/title course/topics]} (om/props this)]
      (dom/div nil
        (dom/hr nil)
        title
        (map topic-link topics)))))

(def home-course (om/factory HomeCourse))

(om/defui ^:once HomePage
  static uc/InitialAppState
  (initial-state [_ _] {:app/courses []})

  static om/IQuery
  (query [_] [{:app/courses (om/get-query HomeCourse)}])

  Object
  (render [this]
    (let [{:keys [app/courses]} (om/props this)]
      (dom/div nil
        "Home"
        (map home-course courses)))))

(defmethod r/route->component ::r/home [_] HomePage)

(om/defui ^:once AboutPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "About"))))

(defmethod r/route->component ::r/about [_] AboutPage)

(om/defui ^:once LessonCell
  static om/IQuery
  (query [_] [:lesson/title :youtube/id :lesson/type :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [lesson/title url/slug] :as lesson} (om/props this)]
      (dom/div nil
        (dom/img #js {:src (u/lesson-thumbnail-url lesson)})
        (link {::r/handler ::r/lesson ::r/params {::r/slug slug}} title)))))

(def lesson-cell (om/factory LessonCell {:key-fn :db/id}))

(om/defui ^:once TopicSideBarLink
  static om/IQuery
  (query [_] [:topic/title :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [url/slug topic/title]} (om/props this)]
      (dom/div nil
        (button {::r/handler ::r/topic ::r/params {::r/slug slug}} title)))))

(def topic-side-bar-link (om/factory TopicSideBarLink))

(om/defui ^:once TopicPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  static r/IRouteMiddleware
  (remote-query [this route]
    (let [slug (get-in route [::r/params ::r/slug])]
      [{[:topic/by-slug slug] (om/get-query this)}]))

  static om/Ident
  (ident [_ props] (u/model-ident props))

  static om/IQuery
  (query [_] [{:topic/course [:course/title
                              {:course/topics (om/get-query TopicSideBarLink)}]}
              {:topic/lessons (om/get-query LessonCell)}])

  Object
  (render [this]
    (let [{:keys [topic/lessons topic/course]} (u/route-prop this [:topic/by-slug ::r/slug])]
      (dom/div nil
        (:course/title course)
        (dom/div nil
          (map topic-side-bar-link (:course/topics course))
          (map lesson-cell lessons))))))

(defmethod r/route->component ::r/topic [_] TopicPage)

(om/defui ^:once LessonVideo
  static om/IQuery
  (query [_] [:lesson/type :lesson/description :youtube/id])

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil))))

(def lesson-video (om/factory LessonVideo))

(om/defui ^:once LessonPlaylistItem
  static om/IQuery
  (query [_] [:db/id :playlist-item/title])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil))))

(def lesson-playlist-item (om/factory LessonPlaylistItem))

(om/defui ^:once LessonPlaylist
  static om/IQuery
  (query [_] [:lesson/type :lesson/description
              {:lesson/playlist-items (om/get-query LessonPlaylistItem)}])

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Playlist"))))

(def lesson-playlist (om/factory LessonPlaylist))

(om/defui ^:once LessonExercise
  static om/IQuery
  (query [_] [:lesson/type :lesson/title])

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Exercise"))))

(def lesson-exercise (om/factory LessonExercise))

(om/defui ^:once LessonPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  static om/Ident
  (ident [_ {:keys [lesson/type db/id]}]
    [(or type :unknown) id])

  static om/IQuery
  (query [_]
    {:lesson.type/video    (om/get-query LessonVideo)
     :lesson.type/playlist (om/get-query LessonPlaylist)
     :lesson.type/exercise (om/get-query LessonExercise)})

  static r/IRouteMiddleware
  (remote-query [this route]
    (let [slug (get-in route [::r/params ::r/slug])]
      [{[:lesson/by-slug slug] (om/get-query this)}]))

  Object
  (render [this]
    (let [{:keys [lesson/type] :as lesson} (u/route-prop this [:lesson/by-slug ::r/slug])]
      (dom/div nil
        "Lesson Page"
        (pr-str (om/props this))))))

(defmethod r/route->component ::r/lesson [_] LessonPage)

(om/defui ^:once NotFoundPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod r/route->component :default [_] NotFoundPage)

(om/defui ^:once Loading
  Object
  (initLocalState [_] {:show? false})

  (componentDidMount [this]
    (js/setTimeout
      (fn [] (if (om/mounted? this) (om/set-state! this {:show? true})))
      100))

  (render [this]
    (let [show? (om/get-state this :show?)]
      (if show?
        (dom/div nil "Loading page data...")
        (dom/noscript nil)))))

(def loading (om/factory Loading))

(om/defui ^:once Root
  static uc/InitialAppState
  (initial-state [_ _]
    (let [route (r/current-handler)]
      {:app/route  route
       :route/data (uc/initial-state (r/route->component route) route)}))

  static om/IQueryParams
  (params [this]
    {:route/data []})

  static om/IQuery
  (query [this]
    '[:app/route :ui/react-key
      {:route/data ?route/data}])

  Object
  (componentWillMount [this]
    (let [{:keys [app/route]} (om/props this)
          page-comp (r/route->component route)
          initial-query (om/get-query page-comp)]
      (om/set-query! this {:params {:route/data (u/normalize-route-data-query initial-query)}})))

  (render [this]
    (let [{:keys [app/route route/data ui/react-key]} (om/props this)]
      (dom/div #js {:key react-key}
        (dom/h1 nil "Header")
        (dom/ul nil
          (dom/li nil (link {::r/handler ::r/home} "Home"))
          (dom/li nil (link {::r/handler ::r/about} "About"))
          (dom/li nil (link {::r/handler ::r/topic ::r/params {::r/slug "getting-started"}} "Topic Getting Started")))
        (if (= :loading (get-in data [:ui/fetch-state ::df/type]))
          (loading nil)
          ((u/route->factory route) data))))))
