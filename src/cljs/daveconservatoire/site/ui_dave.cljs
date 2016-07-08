(ns daveconservatoire.site.ui-dave
  (:require [om.next :as om :include-macros true]
            [om.dom :as dom]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [daveconservatoire.site.ui.util :as u]
            [untangled.client.core :as uc]
            [untangled.client.impl.data-fetch :as df]
            [cljs.spec :as s]))

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

(om/defui ^:once Link
  Object
  (render [this]
    (dom/a (u/props->html {} (om/props this))
      (om/children this))))

(def link (om/factory Link))

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
      (dom/div #js {:className "navbar"}
        (dom/div #js {:className "navbar-inner"}
          (dom/div #js {:className "container"}
            (dom/div #js {:className "row"}
              (dom/div #js {:className "span12"}
                (dom/div #js {:className "copyright"}
                  "© Dave Conservatoire 2016. The videos and exercises on this site are available under a "
                  (dom/a #js {:href "http://creativecommons.org/licenses/by-nc-sa/3.0/", :target "_blank"}
                    "CC BY-NC-SA Licence"))))))))))

(def footer (om/factory Footer))

(om/defui ^:once DesktopMenu
  Object
  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div #js {:className "header hidden-phone"}
        (dom/div #js {:className "navbar"}
          (dom/div #js {:className "navbar-inner"}
            (dom/div #js {:className "container"}
              (link {:id "desktopbrand", :className "brand", ::r/handler ::r/home}
                (dom/img #js {:src "/img/dclogo3.png", :alt "Dave Conservatoire"}))
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
                  (button {:react-key "btn-4" :href "/login" :className "loginbutton", ::button-color "red"}
                    "Login")
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
          (dom/h3 nil
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

