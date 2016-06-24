(ns daveconservatoire.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [bidi.bidi :as bidi]
            [untangled.client.core :as uc]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]))

(comment (s/alias 'daveconservatoire.site.ui.button 'button))

(s/def ::component om/component?)
(s/def ::button-color #{"orange" "redorange"})

(om/defui ^:once Button
  Object
  (render [this]
    (let [{:keys [color]
           :or {color "orange"}} (om/props this)]
      (dom/a #js {:href      "#"
                  :className (str "btn dc-btn-" color)}
        (om/children this)))))

(def button (om/factory Button))

(s/fdef button
  :args (s/cat :props (s/keys :opt [::button-color])
               :children (s/* ::component)))

(defn model-ident [{:keys [db/id db/table]}]
  [(keyword (name table) "by-id") id])

(om/defui ^:once TopicLink
  static om/IQuery
  (query [_] [:db/id :db/table :topic/title])

  static om/Ident
  (ident [_ props] (model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/title]} (om/props this)]
      (button nil title))))

(def topic-link (om/factory TopicLink))

(om/defui ^:once HomeCourse
  static om/IQuery
  (query [_] [:db/id :db/table :course/title :course/description
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

(om/defui ^:once Home
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

(defmethod r/route->component ::r/home [_] Home)

(om/defui ^:once About
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "About"))))

(defmethod r/route->component ::r/about [_] About)

(om/defui ^:once Topic
  static uc/InitialAppState
  (initial-state [_ _] {})

  static om/Ident
  (ident [_ props] [:topic/by-id (:db/id props)])

  static om/IQuery
  (query [_] [:topic/slug
              :topic/title])

  Object
  (render [this]
    (let [{:keys [topic/slug]} (om/props this)]
      (dom/div nil
        "Topic" slug))))

(defmethod r/route->component ::r/topic [_] Topic)

(om/defui ^:once NotFound
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod r/route->component :default [_] NotFound)

(om/defui ^:once Link
  Object
  (render [this]
    (let [{:keys [to params]
           :or {params {}}} (om/props this)]
      (dom/a #js {:href (r/path-for {:handler to :route-params params})}
        (om/children this)))))

(def link (om/factory Link))

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

(om/defui ^:once Root
  static uc/InitialAppState
  (initial-state [_ _]
    (let [route (r/current-handler)]
      {:app/route route
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
      (om/set-query! this {:params {:route/data (conj (or initial-query [])
                                                      {:ui/fetch-state ['*]})}})))

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
          ((route->factory route) (assoc data :ref :page)))))))
