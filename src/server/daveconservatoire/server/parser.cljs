(ns daveconservatoire.server.parser
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.next :as om]
            [common.async :refer-macros [<? go-catch]]
            [cljs.core.async :refer [<! >! put! close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [daveconservatoire.models]
            [pathom.core :as p]
            [pathom.sql :as ps]
            [daveconservatoire.server.data :as d :refer [schema]]))

;; ROOT READS

(defn ast-sort [env sort]
  (assoc-in env [:ast :params :sort] sort))

(def root-endpoints
  {:route/data     #(p/read-chan-values ((:parser %) % (:query (:ast %))))
   :topic/by-slug  #(ps/sql-first-node (assoc % ::ps/table :topic)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :lesson/by-slug #(ps/sql-first-node (assoc % ::ps/table :lesson ::ps/union-selector :lesson/type)
                                       [[:where {:urltitle (p/ast-key-id (:ast %))}]])
   :app/courses    #(ps/sql-table-node (-> (ast-sort % "homepage_order")
                                           (assoc ::ps/union-selector :course/home-type)) :course)
   :app/me         #(if-let [id (:current-user-id %)]
                     (ps/sql-first-node (assoc % ::ps/table :user)
                                        [[:where {:id id}]]))})

(def root-reader
  [root-endpoints p/placeholder-node #(vector :error :not-found)])

;; MUTATIONS

(defmulti mutate om/dispatch)

(defmethod mutate 'app/logout
  [{:keys [http-request]} _ _]
  {:action (fn [] (.logout http-request))})

(defmethod mutate 'lesson/save-view
  [{:keys [current-user-id] :as env} _ {:keys [db/id]}]
  {:action
   (fn []
     (go
       (when current-user-id
         (<? (d/hit-video-view env {:user-view/user-id   current-user-id
                                    :user-view/lesson-id id}))
         nil)))})

(defmethod mutate 'user/update
  [env _ data]
  {:action #(d/update-current-user env data)})

;; PARSER

(def parser (om/parser {:read p/read :mutate mutate}))

(defn parse [env tx]
  (-> (parser
        (assoc env
          ::p/reader root-reader
          ::ps/query-cache (atom {})
          ::ps/schema schema) tx)
      (p/read-chan-values)))
