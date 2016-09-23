(ns pathom.test)

(defmacro db-test [binding & body]
  (let [[name list] binding]
    `(cljs.test/async done#
       (cljs.core.async.macros/go
         (try
           (doseq [[name# db#] ~list]
             (cljs.test/testing name#
               (let [~name db#]
                 ~@body)))
           (catch :default e#
             (cljs.test/do-report
               {:type :error, :message (.-stack e#) :actual e#})))
         (done#)))))
