(ns daveconservatoire.site.ui.exercises-cards
  (:require [devcards.core]))

(defmacro defex [slug]
  (let [app-sym (symbol (str slug "-app"))
        slug-str (str slug)]
    `(do
       (def ~app-sym (atom (untangled.client.core/new-untangled-test-client)))

       (devcards.core/defcard ~slug
         (devcards.core/dom-node
           (cljs.core/fn [~'_ node#]
             (cljs.core/as-> (daveconservatoire.site.ui.exercises/slug->exercise ~slug-str) it#
                   (daveconservatoire.site.ui.exercises-cards/ex-container it#)
                   (untangled.client.core/mount (deref ~app-sym) it# node#)
                   (cljs.core/reset! ~app-sym it#))))))))
