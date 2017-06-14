(ns daveconservatoire.site.ui.util
  (:require [om.next :as om :include-macros true]
            [daveconservatoire.site.routes :as r :refer [routes]]
            [om.util :as omu]
            [goog.string :as gstr]
            [goog.dom :as gdom]
            [cljs.spec :as s]
            [cljs.core.async :as async :refer [chan]]))

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

(defn model-map? [m]
  (and (contains? m :db/id)
       (contains? m :db/table)))

(defn model-ident [props]
  (let [props (if (model-map? props) props (first (vals props)))
        {:keys [db/id db/table]} props]
    (if (and table id)
      [(keyword (name table) "by-id") id]
      [:unknown nil])))

(s/fdef model-ident
  :args (s/cat :model (s/keys :opt [:db/id :db/table]))
  :ret omu/ident?)

(defn route-prop [c [k route-param]]
  (let [{:keys [app/route] :as props} (if (om/component? c) (om/props c) c)]
    (if route
      (get props [k (get-in route [::r/params route-param])])
      (some-> props vals first))))

(s/fdef route-prop
  :args (s/cat :component (s/or :component om/component?
                                :props map?)
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

(defonce scripts (atom {}))

(defn load-external-script [path]
  (if-let [script (get @scripts path)]
    script
    (let [c (chan)
          script (gdom/createDom "script" #js {:src    path
                                               :async  true
                                               :type   "text/javascript"
                                               :onload #(async/close! c)})]
      (swap! scripts assoc path c)
      (gdom/append js/document.body script)
      c)))
