(ns daveconservatorie.support.base64
  (:require [cljs.spec :as s]
            [daveconservatorie.support.specs :as ss]))

(def KEY-STR "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=")

(defn remove-padding-chars [input]
  (let [lkey (as-> (count input) it
                   (- it 1)
                   (.charAt input it)
                   (.indexOf input it))]
    (if (= 64 lkey)
      (.substring input 0 (- (count input) 1))
      input)))

(defn decode [input]
  (let [input (-> input remove-padding-chars remove-padding-chars)
        bytes (js/parseInt (-> (count input) (/ 4) (* 3)) 10)
        uarray (js/Uint8Array. bytes)
        input (.replace input (js/RegExp "[^A-Za-z0-9\\+\\/\\=]" "g") "")]
    (loop [i 0
           j 0]
      (let [e1 (.indexOf KEY-STR (.charAt input j))
            e2 (.indexOf KEY-STR (.charAt input (+ j 1)))
            e3 (.indexOf KEY-STR (.charAt input (+ j 2)))
            e4 (.indexOf KEY-STR (.charAt input (+ j 3)))
            c1 (bit-or (bit-shift-left e1 2) (bit-shift-right e2 4))
            c2 (bit-or (bit-shift-left (bit-and e2 15) 4) (bit-shift-right e3 2))
            c3 (bit-or (bit-shift-left (bit-and e3 3) 6) e4)]
        (aset uarray i c1)
        (aset uarray (+ i 1) c2)
        (aset uarray (+ i 2) c3))
      (if (< (+ i 3) bytes)
        (recur (+ i 3)
               (+ j 4))))
    uarray))

(s/fdef decode
  :args string?
  :ret ::ss/uint8-array)
