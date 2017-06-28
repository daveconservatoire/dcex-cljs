(ns common.template
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::fragment
  (s/or :str string? :key keyword? :expression (s/coll-of ::fragment)))

(defn translate [piece m]
  (cond
    (string? piece)
    piece

    (keyword? piece)
    (get m piece)

    (sequential? piece)
    (->> (map #(translate % m) piece)
         (str/join ""))))

(s/fdef translate
  :args (s/cat :x ::fragment)
  :ret string?)
