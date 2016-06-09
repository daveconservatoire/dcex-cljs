(ns daveconservatorie.support.specs
  (:require [cljs.spec :as s]
            [clojure.string :as str]))

(s/def ::uint8-array #(instance? js/Uint8Array %))
(s/def ::array-buffer #(instance? js/ArrayBuffer %))

(defn base64-str? [str] (str/starts-with? str "data:audio"))

(s/def ::base64-string (s/and string? base64-str?))
(s/def ::channel identity)
