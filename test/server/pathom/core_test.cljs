(ns pathom.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [do-report is are async testing deftest use-fixtures]]
            [pathom.core :as p]
            [cljs.core.async :refer [<!]]
            [om.next :as om]))

(defn q [q] (-> (om/query->ast q) :children first))

(deftest test-read-key
  (is (= (p/read-from {:ast (q [:name])} (fn [_] "value"))
         "value"))
  (is (= (p/read-from {:ast (q [:name])} (fn [_] nil))
         nil))
  (is (= (p/read-from {:ast (q [:name]) :x 42} (fn [env] (:x env)))
         42))
  (is (= (p/read-from {:ast (q [:name]) :x 42} [(fn [env] (:x env))])
         42))
  (is (= (p/read-from {:ast (q [:name]) :x 42} [{} {:name (fn [env] (:x env))}])
         42))
  (is (= (p/read-from {:ast (q [:name])} {})
         nil))
  (is (= (p/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (p/read-from {:ast (q [:name])} {:name #(str "value")})
         "value"))
  (is (= (p/read-from {:ast (q [:name])} [])
         nil))
  (let [c (fn [_] ::p/continue)
        m (fn [_] 42)]
    (is (= (p/read-from {:ast (q [:name])} [c])
           nil))
    (is (= (p/read-from {:ast (q [:name])} [m])
           42))
    (is (= (p/read-from {:ast (q [:name])} [c m])
           42))
    (is (= (p/read-from {:ast (q [:name])} [c {:no #(str "value")} [c c] {:name #(str "Bil")}])
           "Bil"))
    (is (= (p/read-from {:ast (q [:name])} [(fn [_] nil)])
           nil))))

(deftest test-read
  (let [parser (om/parser {:read p/read})
        reader (fn [{:keys [parser ast path] :as env}]
                 (case (namespace (:key ast))
                   "down" (p/read-chan-values (parser (assoc env :path (conj path (:key ast)))
                                                      (:query ast)))
                   "val" (-> ast :key name)
                   "cval" (go (-> ast :key name))
                   "case" (case (:key ast)
                            :a 1)
                   "err" (throw (ex-info (str "Fake Error: " (-> ast :key name)) {}))
                   "cerr" (go (ex-info (str "Fake Error: " (-> ast :key name)) {}))
                   "end"))]

    #_ (async done
      (go
        (is (= (-> (parser {::p/reader reader}
                           [:app/some
                            {:down/info [:val/sample
                                         :cval/other
                                         :err/invalid
                                         {:down/more [:cerr/error
                                                      :case/bla]}]}])
                   p/read-chan-values <!)
               {})))

      (done))))
