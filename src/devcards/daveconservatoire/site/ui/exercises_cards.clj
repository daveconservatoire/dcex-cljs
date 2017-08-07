(ns daveconservatoire.site.ui.exercises-cards
  (:require [devcards.core]))

(defmacro defex [slug]
  (let [app-sym (symbol (str slug "-app"))
        slug-str (str slug)]
    `(do
       (def ~app-sym (atom (fulcro.client.core/new-fulcro-test-client)))

       (devcards.core/defcard ~slug
         (devcards.core/dom-node
           (fn [~'_ node#]
             (as-> (daveconservatoire.site.ui.exercises/slug->exercise ~slug-str) it#
                   (daveconservatoire.site.ui.exercises-cards/ex-container it#)
                   (fulcro.client.core/mount (deref ~app-sym) it# node#)
                   (reset! ~app-sym it#))))))))
