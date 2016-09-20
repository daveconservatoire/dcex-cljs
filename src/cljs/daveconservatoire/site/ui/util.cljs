(ns daveconservatoire.site.ui.util
  (:require [om.next :as om :include-macros true]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [om.util :as omu]
            [goog.string :as gstr]
            [cljs.spec :as s]))

(def transition-group (js/React.createFactory js/React.addons.CSSTransitionGroup))

(defn html-attr-merge [a b]
  (cond
    (map? a) (merge a b)
    (string? a) (str a " " b)
    :else b))

(s/fdef html-attr-merge
  :args (s/cat :a any?
               :b any?)
  :ret any?)

(defn parse-route [{:keys [::r/handler] :as attrs}]
  (cond-> attrs
    handler (assoc :href (r/path-for attrs))))

(s/fdef parse-route
  :args (s/cat :attrs map?)
  :ret map?)

(defn merge-props [a b]
  (merge-with html-attr-merge a b))

(s/fdef merge-props
  :args (s/cat :a map? :b map?)
  :ret map?)

(defn props->html
  ([props] (props->html {} props))
  ([attrs props]
   (->> (dissoc props :react-key)
        (merge-props attrs)
        (parse-route)
        (into {} (filter (fn [[k _]] (not (namespace k)))))
        (clj->js))))

(s/fdef props->html
  :args (s/cat :attrs map?
               :props map?)
  :ret map?)

(defn model-ident [{:keys [db/id db/table]}]
  (if (and table id)
    [(keyword (name table) "by-id") id]
    [:unknown 0]))

(s/fdef model-ident
  :args (s/cat :model (s/keys :opt [:db/id :db/table]))
  :ret omu/ident?)

(defn route-prop [c [k route-param]]
  (let [{:keys [app/route] :as props} (if (om/component? c) (om/props c) c)]
    (get props [k (get-in route [::r/params route-param])])))

(s/fdef route-prop
  :args (s/cat :component om/component?
               :target (s/tuple keyword? keyword?))
  :ret any?)

(defn normalize-route-data-query [q]
  (conj (or q [])
        {:ui/fetch-state ['*]}
        {[:app/route '_] ['*]}))

(defn route->factory [route] (om/factory (r/route->component route)))

(s/fdef route->factory
  :args (s/cat :route ::r/route)
  :ret fn?)

(defn lesson-thumbnail-url [{:keys [lesson/type] :as lesson}]
  (case type
    :lesson.type/playlist "/img/playlist.jpg"
    :lesson.type/exercise "/img/exercise.jpg"
    :lesson.type/video (str "http://img.youtube.com/vi/" (:youtube/id lesson) "/default.jpg")))

(s/fdef lesson-thumbnail-url
  :args (s/cat :lesson (s/keys :req [:lesson/type]))
  :ret string?)

(defn current-uri-slug? [handler slug]
  (= (r/current-handler) {::r/handler handler
                          ::r/params  {::r/slug (gstr/trim slug)}}))

(s/fdef current-uri-slug?
  :args (s/cat :handler ::r/handler :slug ::r/slug)
  :ret boolean?)
