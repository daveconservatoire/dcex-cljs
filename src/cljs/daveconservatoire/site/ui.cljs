(ns daveconservatoire.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [bidi.bidi :as bidi]
            [untangled.client.core :as uc]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]))

(defn html-attr-merge [a b]
  (cond
    (map? a) (merge a b)
    (string? a) (str a " " b)
    :else b))

(defn parse-route [{:keys [::r/handler ::r/route-params] :as attrs}]
  (cond-> attrs
    handler (assoc :href (r/path-for {:handler handler :route-params (or route-params {})}))))

(s/fdef parse-route
  :args (s/cat :attrs map?)
  :ret map?)

(defn props->html [attrs props]
  (->> (merge-with html-attr-merge attrs props)
       (parse-route)
       (filter (fn [[k _]] (not (namespace k))))
       (into {})
       (clj->js)))

(s/fdef props->html
  :args (s/cat :attrs map?
               :props map?)
  :ret map?)

(s/def ::component om/component?)
(s/def ::button-color #{"yellow" "orange" "redorange" "red"})

(om/defui ^:once Button
  Object
  (render [this]
    (let [{:keys [::button-color]
           :or   {::button-color "orange"} :as props} (om/props this)]
      (dom/a (props->html {:href      "#"
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
    (let [{:keys [to params]
           :or   {params {}}} (om/props this)]
      (dom/a #js {:href (r/path-for {:handler to :route-params params})}
        (om/children this)))))

(def link (om/factory Link))

(defn model-ident [{:keys [db/id db/table]}]
  (if (and table id)
    [(keyword (name table) "by-id") id]
    [:unknown 0]))

(om/defui ^:once TopicLink
  static om/IQuery
  (query [_] [:topic/title :url/slug])

  static om/Ident
  (ident [_ props] (model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/title url/slug]} (om/props this)]
      (button {::r/handler ::r/topic ::r/route-params {::r/slug slug}} title))))

(def topic-link (om/factory TopicLink {:key-fn :db/id}))

(om/defui ^:once HomeCourse
  static om/IQuery
  (query [_] [:course/title :course/description
              {:course/topics (om/get-query TopicLink)}])

  static om/Ident
  (ident [_ props] (model-ident props))

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

(defn route-props [c [k route-param]]
  (let [{:keys [app/route] :as props} (if (om/component? c) (om/props c) c)]
    (get props [k (get-in route [:route-params route-param])])))

(om/defui ^:once LessonCell
  static om/IQuery
  (query [_] [:lesson/title :youtube/id])

  static om/Ident
  (ident [_ props] (model-ident props))

  Object
  (render [this]
    (let [{:keys [lesson/title]} (om/props this)]
      (dom/div nil "Lesson: " title))))

(def lesson-cell (om/factory LessonCell {:key-fn :db/id}))

(om/defui ^:once TopicSideBarLink
  static om/IQuery
  (query [_] [:topic/title :url/slug])

  static om/Ident
  (ident [_ props] (model-ident props))

  Object
  (render [this]
    (let [{:keys [url/slug topic/title]} (om/props this)]
      (dom/div nil
        (button {::r/handler ::r/topic ::r/route-params {::r/slug slug}} title)))))

(def topic-side-bar-link (om/factory TopicSideBarLink))

(om/defui ^:once TopicPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  static r/IRouteMiddleware
  (remote-query [this route]
    (let [slug (get-in route [:route-params ::r/slug])]
      [{[:topic/by-slug slug] (om/get-query this)}]))

  static om/Ident
  (ident [_ props] (model-ident props))

  static om/IQuery
  (query [_] [{:topic/course [:course/title
                              {:course/topics (om/get-query TopicSideBarLink)}]}
              {:topic/lessons (om/get-query LessonCell)}])

  Object
  (render [this]
    (let [{:keys [topic/lessons topic/course]} (route-props this [:topic/by-slug ::r/slug])]
      (dom/div nil
        (:course/title course)
        (dom/div nil
          (map topic-side-bar-link (:course/topics course))
          (map lesson-cell lessons))))))

(defmethod r/route->component ::r/topic [_] TopicPage)

(om/defui ^:once NotFoundPage
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod r/route->component :default [_] NotFoundPage)

(defn route->factory [route] (om/factory (r/route->component route)))

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

(defn normalize-route-data-query [q]
  (conj (or q [])
        {:ui/fetch-state ['*]}
        {[:app/route '_] ['*]}))

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
          initial-query (om/get-query (r/route->component route))]
      (om/set-query! this {:params {:route/data (normalize-route-data-query initial-query)}})))

  (render [this]
    (let [{:keys [app/route route/data ui/react-key]} (om/props this)]
      (dom/div #js {:key react-key}
        (dom/h1 nil "Header")
        (dom/ul nil
          (dom/li nil (link {:to ::r/home} "Home"))
          (dom/li nil (link {:to ::r/about} "About"))
          (dom/li nil (link {:to ::r/topic :params {::r/slug "getting-started"}} "Topic Getting Started")))
        (if (= :loading (get-in data [:ui/fetch-state ::df/type]))
          (loading nil)
          ((route->factory route) data))))))
