(ns lein-nsort.core
  (:require [clojure.java.io :as io]
            [com.rpl.specter :as s]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.pprint :as pp]))


(def sort-fns {:asc               {:fn first}
               :desc              {:fn first :comp #(compare %2 %1)}
               :alias-bottom-asc  {:fn (juxt #(* -1 (.indexOf % :as)) first)}
               :alias-bottom-desc {:fn (juxt #(.indexOf % :as) first) :comp #(compare %2 %1)}})

(def path (s/path (s/codewalker #(and (list? %)
                                      (#{:import :require} (first %))))))


(defn- add-decl-err [acc opts]
  (let [all-nses    (mapcat rest (:nses opts))
        sorted-nses (sort-by (:key-fn opts) (:comp-fn opts) all-nses)
        nses-count  (count (:nses opts))]
    (if (and (= all-nses sorted-nses) (= nses-count 1))
      acc
      (conj acc {:type            (:type opts)
                 :ns              (:ns opts)
                 :sorted          sorted-nses
                 :duplicate-decl? (> nses-count 1)}))))


(defn get-invalid-declarations [{:keys [src-dir]
                                 :or   {src-dir "src"} :as opts}]
  (let [ns-decls (ns-find/find-ns-decls-in-dir (io/file src-dir) ns-find/clj)]
    (reduce
     (fn [acc decl]
       (reduce-kv (fn [acc k v]
                    (add-decl-err acc {:type    k
                                       :ns      (second decl)
                                       :key-fn  (or (when (keyword? (get opts k))
                                                      (get-in sort-fns [(get opts k) :fn]))
                                                    (when (list? (get-in opts [k :sort-fn]))
                                                      (eval (get-in opts [k :sort-fn])))
                                                    (get-in sort-fns [:asc :fn]))
                                       :comp-fn (or (when (keyword? (get opts k))
                                                      (get-in sort-fns [(get opts k) :comp]))
                                                    (when (list? (get-in opts [k :comp]))
                                                      (eval (get-in opts [k :comp])))
                                                    compare)
                                       :nses    v}))
                  acc
                  (group-by first (s/select path decl))))
     []
     ns-decls)))


(defn nsort [opts]
  (try
    (let [start (. System (nanoTime))
          nses  (get-invalid-declarations (or opts {}))
          _     (doseq [[ns* errors] (group-by :ns nses)
                        err errors]
                  (println (format "--- %s %s form should be sorted like this:\n\n%s"
                                   ns*
                                   (:type err)
                                   (with-out-str (pp/pprint (cons (:type err) (:sorted err))))))
                  (when (:duplicate-decl? err)
                    (println (format "There is duplicate %s declaration found\n" (:type err)))))
          took  (long (/ (double (- (. System (nanoTime)) start)) 1000000.0))]
      (println "Took:" took "msecs")
      (if (pos? (count nses))
        (do
          (println (count nses) "namespace declaration errors exist")
          (System/exit 1))
        (do
          (println "All namespace declarations are valid.")
          (System/exit 0))))
    (catch Exception e
      (pp/pprint e)
      (System/exit 2))))
