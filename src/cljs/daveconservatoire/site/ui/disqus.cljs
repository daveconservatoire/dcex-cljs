(ns daveconservatoire.site.ui.disqus
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [daveconservatoire.site.ui.util :as u]
            [cljs.core.async :refer [<!]]
            [om.next :as om]
            [om.dom :as dom]
            [cljs.spec :as s]
            [clojure.set :refer [rename-keys]]
            [goog.string :as gstr]
            [goog.object :as gobj]))

(s/def ::id string?)
(s/def ::shortname string?)
(s/def ::identifier string?)
(s/def ::title string?)
(s/def ::url string?)
(s/def ::category-id string?)
(s/def ::on-new-comment (s/fspec :args (s/cat :comment object?)))

(s/def ::disqus-thread-props
  (s/keys :req [::shortname]
          :opt [::id ::identifier ::title ::url ::category-id ::on-new-comment]))

;;;;;;;;;;;;;

(defn require-disqus [shortname] (u/load-external-script (str "//" shortname ".disqus.com/embed.js")))

(def prop-map
  {::id             "id"
   ::shortname      "shortname"
   ::identifier     "identifier"
   ::title          "title"
   ::url            "url"
   ::category-id    "category_id"
   ::on-new-comment "onNewComment"})

(defn current-url [] js/window.location.href)

(defn Disqus [] js/window.DISQUS)

(defn start-thread [this]
  (go
    (<! (require-disqus (-> (om/props this) ::shortname)))
    (let [props (om/props this)
          props (cond-> props
                  (gstr/isEmpty (::url props)) (assoc ::url (current-url)))
          props (-> props (rename-keys prop-map) clj->js)
          config (fn []
                   (let [t (js-this)
                         url (-> (gobj/getValueByKeys t "page" "url")
                                 (.replace #"#.*" "")
                                 (str "#!newthread"))]
                     (gobj/extend (gobj/get t "page") props)
                     (gobj/set (gobj/get t "page") "url" url)))]
      (js/window.DISQUS.reset (js-obj "reload" true "config" config)))))

(om/defui ^:once DisqusThread
  Object
  (componentDidMount [this] (start-thread this))

  (componentDidUpdate [this _ _] (start-thread this))

  (render [this]
    (let [{:keys []} (om/props this)]
      (dom/div nil
        (dom/div #js {:id "disqus_thread"})))))

(def disqus-thread (om/factory DisqusThread {:validator #(s/valid? ::disqus-thread-props %)}))
