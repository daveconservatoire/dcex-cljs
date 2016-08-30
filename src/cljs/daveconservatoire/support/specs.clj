(ns daveconservatoire.support.specs)

(defmacro keys-js [& {:keys [req-un opt-un]}]
  (let [req-un (or req-un [])
        opt-un (or opt-un [])]
    `(cljs.spec/and (cljs.spec/conformer #(cljs.core/js->clj % :keywordize-keys true))
                    (cljs.spec/keys :req-un ~req-un :opt-un ~opt-un))))
