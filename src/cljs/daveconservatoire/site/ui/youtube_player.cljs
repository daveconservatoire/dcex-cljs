(ns daveconservatoire.site.ui.youtube-player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [chan put! <!]]
            [clojure.data :refer [diff]]
            [goog.object :as gobj]
            [om.next :as om :include-macros true]
            [om.dom :as dom]))

; youtube video sizes
; 426x240
; 640x360
; 854x480
; 1280x720

(def YT_PLAYER_STATE_UNSTARTED -1)
(def YT_PLAYER_STATE_ENDED 0)
(def YT_PLAYER_STATE_PLAYING 1)
(def YT_PLAYER_STATE_PAUSED 2)
(def YT_PLAYER_STATE_BUFFERING 3)
(def YT_PLAYER_STATE_CUED 5)

;; wrappers to call Youtube iFrame API

(defn call-external [obj method & args]
  (if-let [f (gobj/get obj method)]
    (.apply f obj (clj->js args))))

(defn video-loaded-fraction [video] (js-invoke video "getVideoLoadedFraction"))
(defn video-current-time [video] (js-invoke video "getCurrentTime"))
(defn video-duration [video] (js-invoke video "getDuration"))
(defn video-seek [video time opt] (js-invoke video "seekTo" time (clj->js opt)))
(defn video-play [video] (js-invoke video "playVideo"))
(defn video-pause [video] (js-invoke video "pauseVideo"))
(defn video-load-by-id [video id] (call-external video "loadVideoById" id))
(defn video-set-size [video width height] (js-invoke video "setSize" width height))
(defn video-playback-rate [video] (js-invoke video "getPlaybackRate"))
(defn video-set-playback-rate [video rate] (js-invoke video "setPlaybackRate" rate))

(defn call-computed-fn [owner k & args]
  (if-let [f (om/get-computed owner k)]
    (apply f args)))

(defn player-state-changed [owner e]
  (let [data (gobj/get e "data")
        playing? (= data YT_PLAYER_STATE_PLAYING)]
    (om/set-state! owner {:video-state data})
    (call-computed-fn owner :on-state-change owner playing?)))

(defn load-external-script [url]
  (.appendChild (.-body js/document)
                (doto (.createElement js/document "script")
                  (aset "src" url))))

(defonce load-youtube-api
  (memoize (fn []
             (let [c (chan)]
               (gobj/set js/window "onYouTubeIframeAPIReady" #(async/close! c))
               (load-external-script "https://www.youtube.com/iframe_api")
               c))))

(defn create-youtube-player [node options]
  (let [YT (gobj/get js/window "YT")
        Player (gobj/get YT "Player")]
    (assert Player "Youtube player is not available, make sure you have it initialized before calling this function")
    (Player. node (clj->js options))))

(om/defui ^:once YoutubePlayer
  Object
  (initLocalState [_] {:player nil})

  (componentDidMount [this]
    (go
      (<! (load-youtube-api))
      (let [props (assoc (om/props this) :events {:onStateChange (partial player-state-changed this)})
            player (create-youtube-player (om/react-ref this "player-container") props)]
        (om/set-state! this {:player player})
        (call-computed-fn this :on-player-ready this player))))

  (componentWillUpdate [this next-props _]
    (if-let [player (om/get-state this :player)]
      (let [[{:keys [videoId] :as changes}] (diff next-props (om/get-props this))]
        (if videoId
          (video-load-by-id player (:videoId next-props)))
        (if (or (:width changes) (:height changes))
          (video-set-size player (:width next-props) (:height next-props))))))

  (render [this]
    (let [{:keys [width height]
           :or   {width  640
                  height 360}} (om/props this)]
      (dom/div #js {:style #js {:width  width
                                :height height}}
        (dom/div #js {:ref "player-container"} "")))))

(def youtube-player (om/factory YoutubePlayer))
