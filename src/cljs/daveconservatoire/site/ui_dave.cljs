(ns daveconservatoire.site.ui-dave
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [daveconservatoire.site.ui.util :as u]
            [daveconservatoire.site.ui.listeners :as l]
            [untangled.client.core :as uc]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]))

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

(defn menu-link [{:keys [::selected?] :as options} child]
  (dom/li #js {:className (if selected? "dc-bg-orange active" "")}
    (link options
      (dom/i #js {:className "icon-chevron-right" :key "i"})
      child)))

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
                    :onClick #(om/set-state! this {:open? (not open?)})}
          (dom/span #js {:className "caret"}))
        (apply dom/ul #js {:className "dropdown-menu profilemenudd"} (om/children this))))))

(def button-dropdown (om/factory ButtonDropdown))

(defn button-dropdown-item [props & children]
  (apply dom/li (u/props->html props) children))

(defn button-dropdown-divider [props]
  (dom/li (u/props->html {:className "divider"} props)))

(defn user-menu-status [comp]
  (let [{:user/keys [name]} (om/props comp)]
    (button-dropdown
      {::r/handler ::r/profile
       :react-key  "user-menu-status"
       ::title     (dom/div nil
                     (dom/i #js {:className "icon-user icon-white"}) " " name " ("
                     (dom/span #js {:id "pointstotal"}
                       "XXX")
                     " Points)")}
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

(om/defui ^:once CourseBanner
  Object
  (render [this]
    (let [{:keys [title intro]} (om/props this)]
      (dom/div #js {:className "banner"}
        (dom/div #js {:className "container intro_wrapper"}
          (dom/div #js {:className "inner_content"}
            (dom/div #js {:className "pad30"})
            (dom/h1 #js {:className "title"} title)
            (dom/h1 #js {:className "intro" :dangerouslySetInnerHTML #js {:__html intro}})))))))

(def course-banner (om/factory CourseBanner))

(om/defui ^:once HomeCourseTopic
  static om/IQuery
  (query [_] [:db/id :topic/title :url/slug])

  static om/Ident
  (ident [_ props] (u/model-ident props))

  Object
  (render [this]
    (let [{:keys [topic/title url/slug]} (om/props this)]
      (dom/li #js {:className "span4", :style #js {"marginBottom" 5}}
        (link {:className "btn btn-large btn-block dc-btn-yellow" ::r/handler ::r/topic ::r/params {::r/slug slug} :react-key "link"}
              (dom/h3 #js {:key "title"}
                title))))))

(def home-course-topic (om/factory HomeCourseTopic {:keyfn :db/id}))

(om/defui ^:once CourseWithTopics
  static om/IQuery
  (query [_] [:db/id :course/title :course/description
              {:course/topics (om/get-query HomeCourseTopic)}])

  static om/Ident
  (ident [_ props] (u/model-ident props))

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

(om/defui ^:once PageBanner
  Object
  (render [this]
    (let [{:keys [title intro]} (om/props this)]
      (dom/div #js {:className "banner"}
        (dom/div #js {:className "pad30"})
        (dom/div #js {:className "container intro_wrapper"}
          (dom/div #js {:className "inner_content"}
            (dom/h1 #js {:className "title"} title)
            (dom/h1 #js {:className "intro"} intro)))))))

(def page-banner (om/factory PageBanner))



