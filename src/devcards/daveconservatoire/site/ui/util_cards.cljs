(ns daveconservatoire.site.ui.util-cards
  (:require [devcards.core :refer-macros [defcard deftest]]
            [cljs.test :refer-macros [is are run-tests async testing]]
            [daveconservatoire.site.routes :as r]
            [daveconservatoire.site.ui.util :as u]))

(defn deep-equal [a b]
  (= (js->clj a) (js->clj b)))

(deftest test-props->html
  (are [attrs props comb] (deep-equal (u/props->html attrs props) comb)
    {} {}
    #js {}

    {:href "#"} {}
    #js {:href "#"}

    {:href "#"} {::color "black"}
    #js {:href "#"}

    {:href "#"} {:className "hello"}
    #js {:href "#" :className "hello"}

    {:href "#" :className "btn"} {:className "hello"}
    #js {:href "#" :className "btn hello"}

    {:style {:background "#000"}} {}
    #js {:style #js {:background "#000"}}

    {} {:style {:background "#000"}}
    #js {:style #js {:background "#000"}}

    {:style {:background "#000"}} {:style {:color "#fff"}}
    #js {:style #js {:background "#000"
                     :color      "#fff"}}

    {} {::r/handler ::r/home}
    #js {:href "/"}

    {} {::r/handler ::r/topic ::r/route-params {::r/slug "start"}}
    #js {:href "/topic/start"}))
