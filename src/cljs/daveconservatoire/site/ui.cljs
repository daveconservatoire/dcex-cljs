(ns daveconservatoire.site.ui
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [daveconservatoire.site.ui.util :as u]
            [daveconservatoire.site.ui.exercises :as ux]
            [daveconservatoire.site.ui.youtube-player :as ytp]
            [daveconservatoire.site.ui.portal :refer [portal]]
            [untangled.client.core :as uc]
            [untangled.client.mutations :as um]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]
            [daveconservatoire.site.ui.listeners :as l])
  (:import goog.i18n.DateTimeFormat))

(defn ordinal-suffix [i]
  (let [j (mod i 10)
        k (mod i 100)]
    (cond
      (and (= 1 j) (not= 11 k)) (str i "st")
      (and (= 2 j) (not= 12 k)) (str i "nd")
      (and (= 3 j) (not= 13 k)) (str i "rd")
      :else (str i "th"))))

(defn format-time [date format]
  (let [date (if (int? date) (js/Date. (* 1000 date)) date)]
    (-> (DateTimeFormat. format)
        (.format date)
        (clojure.string/replace #"(\d+)o" #(ordinal-suffix (second %))))))

(s/def ::component om/component?)
(s/def ::button-color #{"yellow" "orange" "redorange" "red"})

(defn auth-state [state]
  (let [user (get state :app/me)]
    (cond
      (some-> user first (= :user/by-id)) ::authenticated
      (or (some-> user first (= :unknown))
          (and (map? user)
               (contains? user :ui/fetch-state))) ::loading
      :else ::guest)))

(defprotocol IRequireAuth
  (auth-required? [_]))

(defn container [& children]
  (dom/div #js {:className "container wrapper"}
    (apply dom/div #js {:className "inner_content"}
      children)))

(defn banner [title]
  (dom/div #js {:className "banner"}
    (dom/div #js {:className "container intro_wrapper"}
      (dom/div #js {:className "inner_content"}
        (dom/h1 #js {:className "title"} title)))))

(defn button [props & children]
  (let [{:keys [::button-color]
         :or   {::button-color "orange"} :as props} props]
    (apply dom/a (u/props->html {:href      "#"
                                 :className (str "btn dc-btn-" button-color)}
                                props)
      children)))

(s/fdef button
  :args (s/cat :props (s/keys :opt [::button-color])
               :children (s/* ::component)))

(defn link [props & children]
  (apply dom/a (u/props->html props) children))

(defn nav-list [props & children]
  (apply dom/div (u/props->html {:className "nav nav-list bs-docs-sidenav"} props)
    children))

(defn nav-item [props content]
  (dom/li (u/props->html
            (cond-> {}
              (::selected? props) (assoc :className "dc-bg-orange active"))
            (dissoc props ::r/handler))
    (link (select-keys props [::r/handler ::r/params])
      (dom/i #js {:className (or (::icon props) "icon-chevron-right")}) content)))

(om/defui ^:once Hero
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div #js {:className "banner"}
        (dom/div #js {:className "container intro_wrapper"}
          (dom/div #js {:className "inner_content"}
            (dom/div #js {:className "welcome_index animated fadeInDown"}
              "Welcome to Dave Conservatoire")))))))

(def hero (om/factory Hero))

(om/defui ^:once Homeboxes
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div #js {:className "container wrapper"}
        (dom/div #js {:className "inner_content"}
          (dom/div #js {:className "pad45"})
          (dom/div #js {:className "row"}
            (dom/div #js {:className "span3"}
              (dom/div #js {:className "tile introboxes"}
                (dom/div #js {:className "intro-icon-disc cont-large"}
                  (dom/i #js {:className "icon-film intro-icon-large dc-text-yellow"}))
                (dom/h6 #js {}
                  (dom/small #js {}
                    "WATCH"))
                (dom/p #js {}
                  "Join Dave, your friendly guide, in over 100 video music lessons, introducing how music works from the very beginning.")
                (dom/a #js {:id "getstarted", :className "btn btn-primary  btn-custom btn-rounded btn-block dc-btn-yellow"}
                  "Get Started"))
              (dom/div #js {:className "pad25"}))
            (dom/div #js {:className "span3"}
              (dom/div #js {:className "tile introboxes"}
                (dom/div #js {:className "intro-icon-disc cont-large"}
                  (dom/i #js {:className "icon-star intro-icon-large dc-text-orange"}))
                (dom/h6 #js {}
                  (dom/small #js {}
                    "PRACTICE"))
                (dom/p #js {}
                  "Test your understanding as you go, with interactive exercises designed to enhance your awareness and sensitivity to music.")
                (dom/a #js {:href "/login", :className "btn btn-primary  btn-custom btn-rounded btn-block dc-btn-orange"}
                  "Sign in to track your progress"))
              (dom/div #js {:className "pad25"}))
            (dom/div #js {:className "span3"}
              (dom/div #js {:className "tile introboxes"}
                (dom/div #js {:className "intro-icon-disc cont-large"}
                  (dom/i #js {:className "icon-info intro-icon-large dc-text-redorange"}))
                (dom/h6 #js {}
                  (dom/small #js {}
                    "ABOUT"))
                (dom/p #js {}
                  "Find out all about Dave Conservatoire; the story so far, where we hope to head in the future and how you can lend a hand.")
                (dom/a #js {:href "/about", :className "btn btn-primary  btn-custom btn-rounded btn-block dc-btn-redorange"}
                  "Find out more"))
              (dom/div #js {:className "pad25"}))
            (dom/div #js {:className "span3"}
              (dom/div #js {:className "tile introboxes"}
                (dom/div #js {:className "intro-icon-disc cont-large"}
                  (dom/i #js {:className "icon-money  intro-icon-large dc-text-red"}))
                (dom/h6 #js {}
                  (dom/small #js {}
                    "DONATE"))
                (dom/p #js {}
                  "Dave Conservatoire will be totally free forever.  Our dream is that no-one should miss out on having music in their life. ")
                (dom/a #js {:href "/donate", :className "btn btn-primary  btn-custom btn-rounded btn-block dc-btn-red"}
                  "How you can help"))
              (dom/div #js {:className "pad25"}))))))))

(def homeboxes (om/factory Homeboxes))

(om/defui ^:once Footer
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div #js {:className "navbar" :style #js {:marginBottom 0 :textAlign "center"}}
        (dom/div #js {:className "navbar-inner"}
          (dom/div #js {:className "container"}
            (dom/div #js {:className "row"}
              (dom/div #js {:className "span12"}
                (dom/div #js {:className "copyright" :style #js {:paddingTop 10}}
                  "© Dave Conservatoire 2016. The videos and exercises on this site are available under a "
                  (dom/a #js {:href "http://creativecommons.org/licenses/by-nc-sa/3.0/", :target "_blank"}
                    "CC BY-NC-SA Licence") ".")))))))))

(def footer (om/factory Footer))

(om/defui ^:once ButtonDropdown
  Object
  (initLocalState [_] {:open? false})

  (render [this]
    (let [{:keys [::title] :as props} (om/props this)
          {:keys [open?]} (om/get-state this)]
      (dom/div #js {:className (str "btn-group loginbutton" (if open? " open"))}
        (link (u/merge-props
                {:className "btn btn-success"
                 :style     {:marginRight 0}}
                props)
          title)
        (if open?
          (l/simple-listener {::l/on-trigger #(om/set-state! this {:open? false})}))
        (dom/a #js {:className "btn btn-success dropdown-toggle", :href "#"
                    :onClick   #(om/set-state! this {:open? (not open?)})}
          (dom/span #js {:className "caret"}))
        (apply dom/ul #js {:className "dropdown-menu profilemenudd"} (om/children this))))))

(def button-dropdown (om/factory ButtonDropdown))

(defn button-dropdown-item [props & children]
  (apply dom/li (u/props->html props) children))

(defn button-dropdown-divider [props]
  (dom/li (u/props->html {:className "divider"} props)))

(defn user-menu-status [comp]
  (let [{:keys [user/name user/score]} (om/props comp)]
    (button-dropdown
      {::r/handler ::r/profile
       :react-key  "user-menu-status"
       ::title     (dom/div nil
                     (dom/i #js {:className "icon-user icon-white"}) " " name " ("
                     (dom/span #js {:id "pointstotal"} score) " Points)")}
      (button-dropdown-item {:key "profile-link"}
                            (link {::r/handler ::r/profile}
                              (dom/i #js {:className "icon-pencil"}) " My Profile"))
      (button-dropdown-divider {:key "div1"})
      (button-dropdown-item {:key "logout-link"}
                            (dom/a #js {:href "#" :onClick #(om/transact! comp ['(app/logout)])}
                              (dom/i #js {:className "icon-share-alt"}) "Logout")))))

(om/defui ^:once DesktopMenu
  static om/Ident
  (ident [_ props] (u/model-ident props))

  static om/IQuery
  (query [_] [:user/name :user/score])

  Object
  (render [this]
    (let [{:user/keys [name] :as props} (om/props this)]
      (dom/div #js {:className "header hidden-phone"}
        (dom/div #js {:className "navbar"}
          (dom/div #js {:className "navbar-inner"}
            (dom/div #js {:className "container"}
              (link {:id "desktopbrand", :className "brand", ::r/handler ::r/home}
                (dom/img #js {:src "/img/dclogo3.png" :alt "Dave Conservatoire" :key "logo"}))
              (dom/div #js {:className "navbar"}
                (dom/div #js {:className "navbuttons"}
                  (button {:react-key "btn-0" ::r/handler ::r/about, ::button-color "yellow"}
                          "About")
                  (button {:react-key "btn-1" :href "/donate", ::button-color "orange"}
                          "Donate")
                  (button {:react-key "btn-2" :href "/tuition", ::button-color "redorange"}
                          "Personal Tuition")
                  (button {:react-key "btn-3" :href "/contact", ::button-color "red"}
                          "Contact")
                  (if name
                    (user-menu-status this)
                    (button {:react-key "btn-4" ::r/handler ::r/login :className "loginbutton", ::button-color "red"}
                            "Login"))
                  (dom/span #js {:id "socialmediaicons"}
                    (dom/a #js {:href "http://www.youtube.com/daveconservatoire", :target "_blank"}
                      (dom/img #js {:className "socialicon", :src "/img/socialicons/youtube.png"}))
                    (dom/a #js {:href "http://www.twitter.com/dconservatoire", :target "_blank"}
                      (dom/img #js {:className "socialicon", :src "/img/socialicons/twitter.png"}))
                    (dom/a #js {:href "http://www.facebook.com/daveconservatoire", :target "_blank"}
                      (dom/img #js {:className "socialicon", :src "/img/socialicons/facebook.png"}))
                    (dom/a #js {:href "https://plus.google.com/113803255247330342246", :rel "publisher", :target "_blank"}
                      (dom/img #js {:className "socialicon", :src "/img/socialicons/gplus.png"}))))))))))))

(def desktop-menu (om/factory DesktopMenu))

(defn course-banner [{:keys [title intro]}]
  (dom/div #js {:className "banner"}
    (dom/div #js {:className "container intro_wrapper"}
      (dom/div #js {:className "inner_content"}
        (dom/div #js {:className "pad30"})
        (dom/h1 #js {:className "title"} title)
        (dom/h1 #js {:className "intro" :dangerouslySetInnerHTML #js {:__html intro}})))))

(om/defui ^:once LoginPage
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil
        (banner "Login")
        (container
          (dom/h3 nil
            "Do you already have an account on one of these sites? Click the logo to use your account to login here:")
          (dom/p nil \u00a0)
          (dom/div #js {:className "services"}
            (dom/div #js {:className "auth-services row"}
              (dom/div #js {:style #js {:height 100}, :className "span4 suggested-action google_oauth"}
                (dom/a #js {:className "auth-link google_oauth", :href "/auth/google"}
                  (dom/span #js {:className "auth-title"}
                    "Login with Google")
                  (dom/span #js {:className "auth-icon google_oauth"}
                    (dom/i #js {}))))
              (dom/div #js {:style #js {:height 100}, :className "span4 suggested-action facebook"}
                (dom/a #js {:className "auth-link facebook", :href "/auth/facebook"}
                  (dom/span #js {:className "auth-title"}
                    "Login with Facebook")
                  (dom/span #js {:className "auth-icon facebook"}
                    (dom/i #js {}))))))
          (dom/p nil \u00a0)
          (dom/h3 nil
            "Why should I log in?")
          (dom/p nil
            "Logging in helps us to keep track of what you've been learning about, assess your progress and make the site better!")
          (dom/p nil \u00a0)
          (dom/h3 nil
            "Why are you asking me to login with my Google or Facebook account?")
          (dom/p nil
            "Rather than make you sign up for yet another online service with a new username and password to remember, you can use an account you already have set up.  This is the safest and easiest way for you to login with us.")
          (dom/p nil \u00a0)
          (dom/h3 nil
            "Won't this give you access to all my private data?")
          (dom/p nil
            "Definitely not!  The only information we will ever store about you is your name and email address - and how awesome you're becoming at music!")
          (dom/p nil \u00a0)
          (dom/h3 nil
            "What if I don't have an account with one of these sites?")
          (dom/p nil
            "No problem!  Just go sign up "
            (dom/a #js {:href "https://accounts.google.com/SignUp?continue=https%3A%2F%2Faccounts.google.com%2FManageAccount", :target "_blank"}
              "here")
            " or "
            (dom/a #js {:href "http://www.facebook.com/r.php?locale=en_GB", :target "_blank"}
              "here")
            ". "))))))

(defmethod r/route->component ::r/login [_] LoginPage)

(om/defui ^:once AboutPage
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (course-banner {:title "About" :intro "Intro"}))))

(defmethod r/route->component ::r/about [_] AboutPage)

(om/defui ^:once DonatePage
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (course-banner {:title "Donate" :intro "Intro"}))))

(defmethod r/route->component ::r/donate [_] DonatePage)

(om/defui ^:once LessonCell
  static om/IQuery
  (query [_] [:db/id :lesson/title :youtube/id :lesson/type :lesson/view-state :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [lesson/title lesson/view-state url/slug] :as lesson} (om/props this)]
      (dom/div #js {:className "span2"}
        (link {::r/handler ::r/lesson ::r/params {::r/slug slug}
               :className  (cond-> "thumbnail vertical-shadow suggested-action"
                             (= view-state :lesson.view-state/viewed) (str " ribbon ribbon-viewed")
                             (= view-state :lesson.view-state/started) (str " ribbon ribbon-inprogress"))}
          (dom/img #js {:src (u/lesson-thumbnail-url lesson) :key "img"})
          (dom/p #js {:key "p"} title))))))

(def lesson-cell (om/factory LessonCell {:keyfn :db/id}))

(om/defui ^:once TopicSideBarLink
  static om/IQuery
  (query [_] [:topic/title :topic/started? :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [url/slug topic/title topic/started?]} (om/props this)
          selected? (or (u/current-uri-slug? ::r/topic slug)
                        (= slug (om/get-computed this :ui/topic-slug)))]
      (nav-item {::selected? selected? ::icon (if started? "icon-adjust")
                 ::r/handler ::r/topic ::r/params {::r/slug slug}}
        title))))

(def topic-side-bar-link (om/factory TopicSideBarLink))

(om/defui ^:once CourseTopicsMenu
  static om/IQuery
  (query [_] [:course/title
              {:course/topics (om/get-query TopicSideBarLink)}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [course/title course/topics]} (om/props this)
          slug (om/get-computed this :ui/topic-slug)]
      (dom/div nil
        (dom/h6 nil "Course:")
        (nav-list {:className "activetopic" :style #js {:marginTop 10}}
          (dom/li #js {:className "dc-bg-yellow active activetopiclink"}
            (dom/a #js {:className "activetopiclink" :href "#"}
              title)))
        (dom/h6 nil "Topics:")
        (nav-list {}
          (map (comp topic-side-bar-link #(om/computed % {:ui/topic-slug slug}))
               topics))))))

(def course-topics-menu (om/factory CourseTopicsMenu))

(om/defui ^:once TopicPage
  static r/IRouteMiddleware
  (remote-query [this route]
    (let [slug (get-in route [::r/params ::r/slug])]
      [{[:topic/by-slug slug] (om/get-query this)}]))

  static om/Ident
  (ident [_ props] (u/model-ident props))

  static om/IQuery
  (query [_] [{:topic/course (om/get-query CourseTopicsMenu)}
              {:topic/lessons (om/get-query LessonCell)}])

  Object
  (render [this]
    (let [{:keys [topic/lessons topic/course]} (u/route-prop this [:topic/by-slug ::r/slug])]
      (container
        (dom/div #js {:className "row"}
          (dom/div #js {:className "span4"}
            (course-topics-menu course))
          (dom/div #js {:className "span8"}
            (dom/div #js {:className "tab-content" :style #js {:marginTop 32}}
              (dom/div #js {:className "tab-pane active" :id "topic-all"}
                (dom/div #js {:className "thumbnails"}
                  (for [row (partition 4 4 nil lessons)]
                    (dom/div #js {:className "row" :key (:url/slug (first row)) :style #js {:margin "0px 0px 20px 0px"}}
                      (map lesson-cell row))))))))))))

(defmethod r/route->component ::r/topic [_] TopicPage)

(om/defui ^:once ProfilePage
  static uc/InitialAppState
  (initial-state [_ _] {})

  Object
  (render [this]
    (container
      (dom/div #js {:className "row"}
        (dom/div #js {:className "span2"}
          (dom/ul #js {:className "nav nav-list bs-docs-sidenav"}
            (dom/li nil
              (link {::r/handler ::r/profile} "Profile"))
            (dom/li nil
              (link {::r/handler ::r/profile-activity} "Activity Log"))
            (dom/li nil
              (link {::r/handler ::r/profile-focus} "Focus"))
            ))
        (dom/div #js {:className "span10"}

          )))))

(defmethod r/route->component ::r/profile [_] ProfilePage)

(om/defui ^:once LessonTopicMenuItem
  static om/IQuery
  (query [_] [:lesson/title :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [url/slug lesson/title]} (om/props this)
          selected? (u/current-uri-slug? ::r/lesson slug)]
      (nav-item {::r/handler ::r/lesson ::r/params {::r/slug slug}
                 ::selected? selected?}
        title))))

(def lesson-topic-menu-item (om/factory LessonTopicMenuItem))

(om/defui ^:once LessonTopicMenu
  static om/IQuery
  (query [_] [:url/slug
              {:topic/course [:course/title
                              {:course/topics (om/get-query TopicSideBarLink)}]}
              {:topic/lessons (om/get-query LessonTopicMenuItem)}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys       [url/slug]
           :topic/keys [course lessons]} (om/props this)
          {:course/keys [topics title]} course]

      (dom/div nil
        (dom/h6 nil "Course:")
        (nav-list {:className "activetopic" :style #js {:marginTop 10}}
          (dom/li #js {:className "dc-bg-yellow active activetopiclink"}
            (dom/a #js {:className "activetopiclink" :href "#"}
              title)))

        (dom/h6 nil "Lessons in this topic:")
        (nav-list {}
          (map lesson-topic-menu-item lessons))

        (dom/h6 nil "Topics:")
        (nav-list {}
          (map (comp topic-side-bar-link #(om/computed % {:ui/topic-slug slug}))
               topics))))))

(def lesson-topic-menu (om/factory LessonTopicMenu))

(defn report-video-play [c]
  (let [reported? (om/get-state c :reported?)
        {:keys [db/id]} (om/props c)]
    (when-not reported?
      (om/set-state! c {:reported? true})
      (om/transact! (om/get-reconciler c) `[(lesson/save-view {:db/id ~id})]))))

(om/defui ^:once LessonVideo
  static om/IQuery
  (query [_] [:db/id :lesson/type :lesson/description :youtube/id
              {:lesson/topic (om/get-query LessonTopicMenu)}])

  Object
  (initLocalState [this] {:reported? false})

  (componentWillReceiveProps [this next-props] (om/set-state! this {:reported? false}))

  (render [this]
    (let [{:keys [lesson/description lesson/topic youtube/id] :as props} (om/props this)]
      (container
        (dom/div #js {:className "row"}
          (dom/div #js {:className "span3"}
            (lesson-topic-menu topic))
          (dom/div #js {:className "span9"}
            (dom/div #js {:className "lesson-content"}
              (dom/div #js {:className "vendor"}
                (ytp/youtube-player (om/computed {:videoId id}
                                                 {:on-state-change #(if %2 (report-video-play this))})))
              (dom/div #js {:className "well"}
                description))))))))

(def lesson-video (om/factory LessonVideo))

(om/defui ^:once LessonPlaylistItem
  static om/IQuery
  (query [_] [:db/id :youtube/id :playlist-item/title :playlist-item/text])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [youtube/id playlist-item/title playlist-item/text]} (om/props this)]
      (dom/div nil
        (dom/div #js {:className "vendor"} (ytp/youtube-player {:videoId id}))
        (dom/div #js {:className "well"}
          (dom/div nil title)
          (dom/div nil text))))))

(def lesson-playlist-item (om/factory LessonPlaylistItem))

(om/defui ^:once LessonPlaylist
  static om/IQuery
  (query [_] [:lesson/type :lesson/description :ui/selected-index :db/id
              {:lesson/playlist-items (om/get-query LessonPlaylistItem)}
              {:lesson/topic (om/get-query LessonTopicMenu)}])

  Object
  (render [this]
    (let [{:keys [db/id lesson/topic lesson/type lesson/playlist-items ui/selected-index]} (om/props this)
          selected-index (or selected-index 0)
          item (nth (vec playlist-items) selected-index)
          set-selected (fn [n]
                         (om/transact! (om/get-reconciler this)
                                       [type id]
                                       [`(ui/set-props {:ui/selected-index ~n})
                                        :route/data]))]
      (container
        (dom/div #js {:className "row"}
          (dom/div #js {:className "span3"}
            (lesson-topic-menu topic))
          (dom/div #js {:className "span9"}
            (dom/div #js {:className "lesson-content"}
              (if item
                (lesson-playlist-item item))
              (dom/div nil
                (dom/button #js {:onClick  #(set-selected (dec selected-index))
                                 :disabled (= 0 selected-index)} "<<")
                (dom/button #js {:style    #js {:float "right"} :onClick #(set-selected (inc selected-index))
                                 :disabled (= (dec (count playlist-items)) selected-index)} ">>")))))))))

(def lesson-playlist (om/factory LessonPlaylist))

(om/defui ^:once LessonExercise
  static om/IQuery
  (query [_] [:lesson/type :lesson/title :url/slug :db/id
              {:exercise/data '[*]}
              {:lesson/topic (om/get-query LessonTopicMenu)}])

  Object
  (componentDidMount [this]
    (let [{:keys [url/slug lesson/type db/id]} (om/props this)
          {:keys [::ux/class ::ux/props] :as info} (ux/slug->exercise slug)]
      (if info
        (let [state (-> (uc/initial-state class props)
                        (merge info))
              ident (om/ident class state)]
          (om/transact! (om/get-reconciler this) ident
                        [`(ui/set-props ~state)])
          (om/transact! (om/get-reconciler this) [type id]
                        [`(ui/set-props {:exercise/data ~ident})])
          (om/force-root-render! (om/get-reconciler this))))))

  (render [this]
    (let [{:keys [lesson/topic url/slug exercise/data]} (om/props this)
          {:keys [::ux/class]} (ux/slug->exercise slug)]
      (container
        (dom/div #js {:className "row"}
          (dom/div #js {:className "span3"}
            (lesson-topic-menu topic))
          (dom/div #js {:className "span9"}
            (if class
              (when (get data ::ux/streak-count)
                ((om/factory class) data))
              (str "Exercice [" slug "] not implemented"))))))))

(def lesson-exercise (om/factory LessonExercise))

(om/defui ^:once LessonPage
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
      (case type
        :lesson.type/video (lesson-video lesson)
        :lesson.type/playlist (lesson-playlist lesson)
        :lesson.type/exercise (lesson-exercise lesson)
        (dom/noscript nil)))))

(defmethod r/route->component ::r/lesson [_] LessonPage)

(om/defui ^:once ProfileRecentActivity
  static om/IQuery
  (query [_] [:db/id {:user-view/lesson [:lesson/title :url/slug]}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [user-view/lesson]} (om/props this)
          {:keys [lesson/title url/slug]} lesson]
      (dom/tr nil
        (dom/td nil
          (dom/i #js {:className "icon-facetime-video"}))
        (dom/td nil
          (dom/strong #js {} "Watched: ")
          (link {::r/handler ::r/lesson ::r/params {::r/slug slug}}
            title))))))

(def profile-recent-activity (om/factory ProfileRecentActivity {:keyfn :db/id}))

(defn modal [{:ui.modal/keys [title onClose onSave]} & children]
  (portal {:append-to "body"}
    (dom/div #js {:style #js {:position   "absolute"
                              :left       0
                              :top        0
                              :background "rgba(0, 0, 0, 0.5)"
                              :width      "100vw"
                              :height     "100vh"}}
      (dom/div #js {:id "myModal" :className "modal hide fade in" :tabIndex "-1" :role "dialog" :style #js {:display "block"}}
        (dom/div #js {:className "modal-header"}
          (dom/button #js {:type "button" :className "close" :onClick #(if onClose (onClose))}
            "×")
          (dom/h3 #js {:id "myModalLabel"}
            title))
        (dom/div #js {:className "modal-body"}
          (apply dom/div nil children)
          (dom/div #js {:className "row buttons"})
          (dom/div #js {:className "modal-footer"}
            (dom/button #js {:className "btn"
                             :onClick   #(if onClose (onClose))}
              "Close")
            (dom/button #js {:className "btn btn-primary"
                             :onClick   #(if onSave (onSave))}
              "Save")))))))

(defn user-info-modal [this]
  (let [{:keys [user/about ui/tmp-about]} (om/props this)]
    (modal #:ui.modal {:title   "Update your Profile"
                       :onClose #(do
                                  (um/set-value! this :ui/editing-info? false)
                                  (um/set-value! this :ui/tmp-about about))
                       :onSave  #(do
                                  (om/transact! this `[(user/update {:user/about ~tmp-about})])
                                  (um/set-value! this :ui/editing-info? false))}
      (dom/form #js {:id "user-form" :action "profile/update" :method "post"}
        (dom/div #js {:id "user-form_es_" :className "errorSummary" :style #js {:display "none"}}
          (dom/p nil
            "Please fix the following input errors:")
          (dom/ul nil
            (dom/li nil
              "dummy")))
        (dom/label #js {:htmlFor "User_biog"}
          (dom/h3 nil
            "About you") "Who are you? What are your musical goals? What instruments do you play? "
          (dom/br nil)
          "(max. 160 characters)"
          (dom/br nil)
          (dom/br nil))
        (dom/textarea #js {:size      "60"
                           :maxLength "160"
                           :id        "User_biog"
                           :value     (or tmp-about about)
                           :onChange  #(um/set-string! this :ui/tmp-about :event %)
                           :style     #js {:zIndex "auto" :position "relative" :lineHeight "20px" :fontSize 14 :transition "none" :background "none 0% 0% / auto repeat scroll padding-box border-box rgb(255 255 255)"}})
        (dom/div #js {:className "errorMessage" :id "User_biog_em_" :style #js {:display "none"}})))))

(om/defui ^:once ProfileDashboard
  static om/Ident
  (ident [_ props]
    (u/model-ident props))

  static om/IQuery
  (query [_]
    [:db/id :db/table :user/name :user/about :user/score :ui/editing-info?
     :user/lessons-viewed-count :user/created-at :ui/tmp-about
     {:user/user-views (om/get-query ProfileRecentActivity)}])

  Object
  (render [this]
    (let [{:user/keys [name about user-views score lessons-viewed-count created-at]
           :ui/keys   [editing-info?]} (om/props this)]
      (dom/div #js {:className "span10"}
        (dom/div #js {:className "row"}
          (dom/div #js {:className "profile-topbar"}
            (dom/div #js {:className "span5 whiteback"}
              (dom/div #js {:className "padding"}
                (if editing-info?
                  (user-info-modal this))
                (dom/a #js {:href        "#myModal"
                            :role        "button"
                            :onClick     #(um/set-value! this :ui/editing-info? true)
                            :className   "btn dc-btn-red pull-right"
                            :data-toggle "modal"}
                  "Update your info")
                (dom/h1 #js {:style #js {:margin 0}} name)
                (dom/h3 nil "About me")
                (dom/p nil about)))
            (dom/div #js {:className "span5 whiteback"}
              (dom/div #js {:className "padding"}
                (dom/i #js {:className "icon-star intro-icon-large dc-text-orange pull-right"})
                (dom/h1 #js {:style #js {:margin 0}} "My Subscription")
                (dom/h1 nil "$9.00 per month")
                (dom/p nil "Thank you so much for your subscription. Your support is vital in helping us serve music students around the world.")))))
        (dom/div #js {:className "pad30"})
        (dom/div #js {:className "row"}
          (dom/div #js {:className "span5 whiteback"}
            (dom/div #js {:className "padding"}
              (dom/h3 nil
                "Statistics")
              (dom/table #js {:style #js {:margin 0 :width "100%"}}
                (dom/tbody nil
                  (dom/tr nil
                    (dom/td nil "Groove Score:")
                    (dom/td #js {:className "pull-right"} score))
                  (dom/tr nil
                    (dom/td nil "Lessons Viewed:")
                    (dom/td #js {:className "pull-right"} lessons-viewed-count))
                  (dom/tr nil
                    (dom/td nil "Exercises Answered:")
                    (dom/td #js {:className "pull-right"} "XXX"))
                  (dom/tr nil
                    (dom/td nil "Member Since:")
                    (dom/td #js {:className "pull-right"} (format-time (js/Date. (* 1000 created-at)) "MMMM ddo yyyy")))))))
          (dom/div #js {:className "span5 whiteback"}
            (dom/div #js {:className "padding"}
              (dom/h3 nil "Recent Activity")
              (dom/table #js {:className "table" :style #js {:margin 0}}
                (dom/tbody nil
                  (map profile-recent-activity user-views))))))))))

(def profile-dashboard (om/factory ProfileDashboard))

(defn profile-page [child]
  (container
    (dom/div #js {:style #js {:height 13}})
    (dom/div #js {:className "row"}
      (dom/div #js {:className "span2"}
        (nav-list {}
          (nav-item {::r/handler ::r/profile} "Dashboard")
          (nav-item {::r/handler ::r/profile-activity} "Activity Log")
          (nav-item {::r/handler ::r/profile-focus} "Focus")))
      child)))

(om/defui ^:once ProfilePage
  static om/IQuery
  (query [_]
    [{:app/me (om/get-query ProfileDashboard)}])

  static IRequireAuth
  (auth-required? [_] true)

  Object
  (render [this]
    (let [{:keys [app/me]} (om/props this)]
      (profile-page (profile-dashboard me)))))

(defmethod r/route->component ::r/profile [_] ProfilePage)

(om/defui ^:once ProfileActivity
  static om/IQuery
  (query [_] [:db/id :user-view/timestamp {:user-view/lesson [:lesson/title :url/slug]}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [user-view/lesson user-view/timestamp]} (om/props this)
          {:keys [new-streak?]} (om/get-computed this)
          {:keys [lesson/title url/slug]} lesson]
      (dom/tr nil
        (dom/td nil
          (if new-streak?
            (format-time timestamp "EEEE ddo MMMM yyyy")))
        (dom/td nil
          (dom/i #js {:className "icon-facetime-video"}))
        (dom/td nil
          (dom/strong #js {} "Watched: ")
          (link {::r/handler ::r/lesson ::r/params {::r/slug slug}}
            title))))))

(def profile-activity (om/factory ProfileActivity {:keyfn :db/id}))

(defn process-view-consecutive-date [views]
  (->> (reduce
         (fn [[acc last] v]
           (let [time (format-time (:user-view/timestamp v) "ddMMyyyy")]
             [(conj acc (om/computed v {:new-streak? (not= last time)}))
              time]))
         [[]]
         views)
       (first)))

(om/defui ^:once ProfileActivityPageInternal
  static om/Ident
  (ident [_ props] (u/model-ident props))

  static om/IQuery
  (query [_] [{:user/user-views (om/get-query ProfileActivity)}])

  Object
  (render [this]
    (let [{:keys [user/user-views]} (om/props this)]
      (dom/div #js {:className "span10"}
        (dom/h2 nil "Here's what you've been working on:")
        (dom/br nil)
        (dom/table #js {:className "table"}
          (dom/tbody nil
            (map profile-activity (process-view-consecutive-date user-views))))))))

(def profile-activity-page-internal (om/factory ProfileActivityPageInternal))

(om/defui ^:once ProfileActivityPage
  static om/IQuery
  (query [_] [{:app/me (om/get-query ProfileActivityPageInternal)}])

  static IRequireAuth
  (auth-required? [_] true)

  Object
  (render [this]
    (let [{:keys [app/me]} (om/props this)]
      (profile-page (profile-activity-page-internal me)))))

(defmethod r/route->component ::r/profile-activity [_] ProfileActivityPage)

(om/defui ^:once HomeCourseTopic
  static om/IQuery
  (query [_] [:db/id :topic/title :topic/started? :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/title topic/started? url/slug]} (om/props this)]
      (dom/li #js {:className (cond-> "span4"
                                started? (str " ribbon ribbon-inprogress"))
                   :style #js {"marginBottom" 5}}
        (link {:className "btn btn-large btn-block dc-btn-yellow" ::r/handler ::r/topic ::r/params {::r/slug slug} :react-key "link"}
          (dom/h3 #js {:key "title"}
            title))))))

(def home-course-topic (om/factory HomeCourseTopic {:keyfn :db/id}))

(om/defui ^:once CourseWithTopics
  static om/IQuery
  (query [_] [:db/id :course/home-type :course/title :course/description
              {:course/topics (om/get-query HomeCourseTopic)}])

  Object
  (render [this]
    (let [{:keys [course/title course/topics course/description]} (om/props this)]
      (dom/div #js {}
        (course-banner {:title title :intro description})
        (dom/div #js {:className "pad30"})
        (dom/div #js {:className "container wrapper"}
          (dom/div #js {:className "thumbnails tabbable"}
            (dom/ul #js {:className "courselist"}
              (map home-course-topic topics))))

        (dom/div #js {:className "pad30"})))))

(def course-with-topics (om/factory CourseWithTopics {:keyfn :db/id}))

(om/defui ^:once HomeCourseTopicOpen
  static om/IQuery
  (query [_] [:db/id {:topic/lessons (om/get-query LessonCell)}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/lessons]} (om/props this)
          lesson-groups (partition 6 6 nil lessons)]
      (dom/div #js {:className "thumbnails"}
        (for [[i lessons] (map vector (range) lesson-groups)]
          (dom/div #js {:className "row" :style #js {:margin "0 0 20px 0"}
                        :key       i}
            (map lesson-cell lessons)))))))

(def home-course-topic-open (om/factory HomeCourseTopicOpen {:keyfn :db/id}))

(om/defui ^:once CourseWithSingleTopic
  static om/IQuery
  (query [_] [:db/id :course/home-type :course/title :course/description
              {:course/topics (om/get-query HomeCourseTopicOpen)}])

  Object
  (render [this]
    (let [{:keys [course/title course/topics course/description]} (om/props this)]
      (dom/div #js {}
        (course-banner {:title title :intro description})
        (dom/div #js {:className "pad30"})
        (dom/div #js {:className "container wrapper"}
          (dom/div #js {:className "tab-content"}
            (dom/div #js {:className "tab-pane active"}
              (home-course-topic-open (first topics)))))

        (dom/div #js {:className "pad30"})))))

(def course-with-single-topic (om/factory CourseWithSingleTopic {:keyfn :db/id}))

(om/defui ^:once HomeCourse
  static om/IQuery
  (query [_]
    {:course.type/multi-topic  (om/get-query CourseWithTopics)
     :course.type/single-topic (om/get-query CourseWithSingleTopic)})

  static om/Ident
  (ident [_ {:keys [course/home-type db/id]}]
    [(or home-type :unknown) id])

  Object
  (render [this]
    (let [{:keys [course/home-type] :as course} (om/props this)]
      (case home-type
        :course.type/multi-topic (course-with-topics course)
        :course.type/single-topic (course-with-single-topic course)))))

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
        (hero {:react-key "hero"})
        (homeboxes {:react-key "homeboxes"})
        (map home-course courses)))))

(defmethod r/route->component ::r/home [_] HomePage)

(om/defui ^:once NotFoundPage
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil "Page not found"))))

(defmethod r/route->component :default [_] NotFoundPage)

(om/defui ^:once Loading
  Object
  (initLocalState [_] {:show? true})

  (componentDidMount [this]
    (js/setTimeout
      (fn [] (if (om/mounted? this) (om/set-state! this {:show? true})))
      100))

  (render [this]
    (dom/div #js {:style #js {:position "fixed" :top 0 :left 0 :right 0}}
      (dom/div #js {:className "loading-bar"
                    :style     #js {:background "#F7941E"
                                    :transition "width 200ms"
                                    :height     4}}))))

(def loading (om/factory Loading))

(om/defui ^:once Root
  static uc/InitialAppState
  (initial-state [_ _]
    (let [route (r/current-handler)]
      {:app/route    route
       :ui/react-key (random-uuid)
       :route/data   (r/route->initial-state route)
       :app/me       {:ui/fetch-state {}}}))

  static om/IQueryParams
  (params [this]
    {:route/data []})

  static om/IQuery
  (query [this]
    [:app/route :ui/react-key
     {:app/me (om/get-query DesktopMenu)}
     {:route/data '?route/data}])

  Object
  (componentWillMount [this]
    (let [{:keys [app/route]} (om/props this)
          page-comp (r/route->component route)
          initial-query (om/get-query page-comp)]
      (om/set-query! this {:params {:route/data (u/normalize-route-data-query initial-query)}})))

  (componentDidMount [this]
    (add-watch (some-> (om/get-reconciler this) :config :state)
               :auth-state-detector
               (fn [_ _ o n]
                 (let [{:app/keys [route]} n]
                   (if (or (not= (:app/route o) route)
                           (not= (:app/me o) (:app/me n)))
                     (let [comp (some-> route r/route->component*)
                           auth-req? (if (implements? IRequireAuth comp)
                                       (auth-required? comp) false)]
                       (when (and auth-req? (= (auth-state n) ::guest))
                         (om/transact! this `[(app/set-route ~{::r/handler ::r/login})]))))))))

  (componentWillUnmount [this]
    (remove-watch (some-> (om/get-reconciler this) :config :state) :auth-state-detector))

  (render [this]
    (let [{:keys [app/route app/me route/data ui/react-key]} (om/props this)]
      (dom/div #js {:key react-key}
        (desktop-menu (assoc me :react-key "desktop-menu"))
        (u/transition-group #js {:transitionName "loading" :transitionEnterTimeout 200 :transitionLeaveTimeout 200}
          (if (= :loading (get-in data [:ui/fetch-state ::df/type]))
            (loading nil)))
        ((u/route->factory route) data)
        (footer {:react-key "footer"})))))
