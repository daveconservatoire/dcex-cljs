(ns pathom.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are async testing deftest use-fixtures]]
            [pathom.core :as l]
            [om.next :as om]))

(defn q [q] (-> (om/query->ast q) :children first))

(deftest test-read-key
  (is (= (l/read-from {:ast (q [:name])} (fn [_] "value"))
         "value"))
  (is (= (l/read-from {:ast (q [:name])} (fn [_] nil))
         nil))
  (is (= (l/read-from {:ast (q [:name]) :x 42} (fn [env] (:x env)))
         42))
  (is (= (l/read-from {:ast (q [:name]) :x 42} [(fn [env] (:x env))])
         42))
  (is (= (l/read-from {:ast (q [:name]) :x 42} [{} {:name (fn [env] (:x env))}])
         42))
  (is (= (l/read-from {:ast (q [:name])} {})
         nil))
  (is (= (l/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (l/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (l/read-from {:ast (q [:name])} [])
         nil))
  (let [c (fn [_] ::l/continue)
        m (fn [_] 42)]
    (is (= (l/read-from {:ast (q [:name])} [c])
           nil))
    (is (= (l/read-from {:ast (q [:name])} [m])
           42))
    (is (= (l/read-from {:ast (q [:name])} [c m])
           42))
    (is (= (l/read-from {:ast (q [:name])} [c {:no #(str "value")} [c c] {:name #(str "Bil")}])
           "Bil"))
    (is (= (l/read-from {:ast (q [:name])} [(fn [_] nil)])
           nil))))
