(ns daveconservatoire.server.facebook
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.nodejs :as node]
            [cljs.core.async :refer [<! put! close! promise-chan]]
            [clojure.string :as str]
            [goog.string :as gstr]
            [common.async :as a :refer-macros [<? go-catch]]))

(def sa-request (node/require "superagent"))

(defn request [url] (a/promise->chan (.get sa-request url)))

(s/def ::node string?)
(s/def ::client-id string?)
(s/def ::redirect-uri string?)
(s/def ::client-secret string?)
(s/def ::code string?)
(s/def ::scope-item
  #{:public-profile :user-friends :email :user-about-me :user-actions.books :user-actions.fitness
    :user-actions.music :user-actions.news :user-actions.video :user-birthday
    :user-education-history :user-events :user-games-activity :user-hometown
    :user-likes :user-location :user-managed-groups :user-photos :user-posts
    :user-relationships :user-relationship-details :user-religion-politics :user-tagged-places
    :user-videos :user-website :user-work-history :read-custom-friendlists :read-insights
    :read-audience-network-insights :read-page-mailboxes :manage-pages :publish-pages
    :publish-actions :rsvp-event :pages-show-list :pages-manage-cta
    :pages-manage-instant-articles :ads-read :ads-management :business-management
    :pages-messaging :pages-messaging-phone-number})

(s/def ::scope (s/coll-of ::scope-item))

(s/def ::code string?)
(s/def ::error string?)
(s/def ::error-reason string?)
(s/def ::error-description string?)

(s/def ::code-response (s/keys :req [::code]))
(s/def ::error-response (s/keys :req [::error ::error-reason ::error-description]))
(s/def ::auth-response (s/or :success ::code-response
                             :error ::error-response))

(s/def ::token-response (s/keys :req [::access-token ::token-type ::expires-in]))

(s/def ::picture-url string?)
(s/def ::picture (s/keys :opt [::picture-url]))

(s/def ::user-id pos-int?)
(s/def ::user-name string?)
(s/def ::user-email string?)
(s/def ::user-picture string?)
(s/def ::user-info (s/keys :req [::user-id]
                           :opt [::user-name ::user-email ::picture]))

(defn user-info [access-token]
  (go-catch
    (-> sa-request
        (.get "https://graph.facebook.com/v2.7/me")
        (.query #js {:access_token access-token
                     :fields       "id,name,email,picture"})
        (a/promise->chan) <?
        .-text js/JSON.parse (js->clj :keywordize-keys true))))

(defn login-url [{:keys [::client-id ::redirect-uri ::scope]}]
  (cond-> (str "https://www.facebook.com/dialog/oauth"
               "?client_id=" client-id
               "&redirect_uri=" redirect-uri)
    scope (str "&scope=" (str/join "," (map #(.replace (name %) (js/RegExp. "-" "g") "_") scope)))))

(s/fdef login-url
  :args (s/cat :options (s/keys :req [::client-id ::redirect-uri]))
  :ret string?)

(defn get-token-url [{:keys [::client-id ::redirect-uri ::client-secret ::code]}]
  (str "https://graph.facebook.com/v2.7/oauth/access_token"
       "?client_id=" client-id
       "&redirect_uri=" redirect-uri
       "&client_secret=" client-secret
       "&code=" code))

(s/fdef get-token-url
  :args (s/cat :options (s/keys :req [::client-id ::redirect-uri ::client-secret ::code]))
  :ret string?)

(defn exchange-token [code-request]
  (go-catch
    (let [url (get-token-url code-request)
          response (<? (request url))]
      (.. response -body -access_token))))

(defn facebook-error-message [e]
  (if-let [response (.-response e)]
    (-> (.-text response)
        js/JSON.parse .-error .-message)
    (.-message e)))

(comment
  (let [req (assoc daveconservatoire.server.core/facebook
              ::code "AQA7wOYrWfnABEGlX4l8uA2CPZ5RPOMADmO8gAMEjmnv_m-1l8yhxxFp_5hCJQuShdWcsIZeTmZzF4LozKuFKb7KC7GQbqnWBLOG2NBfGDb5EkS6UmGBCUA1C0kiJUsdCLQV04GlYsd7A-gBukzuQHjJpv0ggk7kze-d7in-y1xjPMN2-WVNKMTSO0zbsKvA5s8_DGyqtqfB2QgHf5QwBXK7A1Va4mZhpQc1r82tCDC_yojboC2hYh76A3xqnbd85_cJEWS_LQjdvXE9Z0B6XAdJLZJswspXDz6jjzCtx5w-XPIS9D-3uHS03M5V0HbeOLk")
        url (get-token-url req)]
    (js/console.log "req" url)
    (go
      (-> (request url) <!
          .-body
          js/console.log)))

  (go
    (try
      (let [req (assoc daveconservatoire.server.core/facebook
                  ::code "AQBUBbL9mosUrp1sqgkVCRZ27c4afGW8XMECXkll73ne1CrueuxJUrOb-Usrdh_T5bx1pD45YQvMDMqYfnlDWvy-ITnui53jY_bM9YTSf9y65Z-gumSKvuf_mKPzD9D7OMDRNoqqwFMbsw6008o6H7x4xK0bvdOOKwrR42uv9-jWhY1pAedIm6G_-WnK6O2Xb2uwMsUjZ6y76DGdArr_1PM7qr6YtsLEKwZj2JxEc-n0jWGHJXcx53h4D9wJWtoIWvs1fcQnwQLhQi9qurMimmmbe0_HKGglYXs6mmx8Q8Z8NpNpImyQvEh5qmxwypOieec")
            token (<? (exchange-token req))]
        (-> token js/console.log))
      (catch :default e
        (js/console.log "error requesting token" (facebook-error-message e)))))
  )
