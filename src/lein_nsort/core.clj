(ns lein-nsort.core
  (:require [clojure.java.io :as io]
            [com.rpl.specter :as s]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.pprint :as pp]))


(def sort-fns {:asc               {:fn first}
               :desc              {:fn first :comp #(compare %2 %1)}
               :alias-bottom-asc  {:fn (juxt #(* -1 (.indexOf % :as)) first)}
               :alias-bottom-desc {:fn (juxt (juxt #(.indexOf % :as) first)) :comp #(compare %2 %1)}})

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
                                                    (get-in opts [k :sort-fn])
                                                    (get-in sort-fns [:asc :fn]))
                                       :comp-fn (or (when (keyword? (get opts k))
                                                      (get-in sort-fns [(get opts k) :comp]))
                                                    (get-in opts [k :comp])
                                                    compare)
                                       :nses    v}))
                  acc
                  (group-by first (s/select path decl))))
     []
     ns-decls)))


(defn check-namespace-decls [opts]
  (try
    (let [nses (get-invalid-declarations (or opts {}))]
      (doseq [[ns* errors] (group-by :ns nses)]
        (println (format "Invalid namespace declaration: %s\n" ns*))
        (doseq [err errors]
          (when (:duplicate-decl? err)
            (println (format "There is duplicate %s declaration" (:type err))))
          (let [ns-or-package (if (= :require (:type err)) "Namespaces" "Packages")]
            (println (format "%s inside %s form should be sorted like this:\n\n%s"
                             ns-or-package
                             (:type err)
                             (with-out-str (pp/pprint (cons (:type err) (:sorted err)))))))))
      (if (pos? nses)
        (System/exit 1)
        (System/exit 0)))
    (catch Exception e
      (println "Error occurred:" (.getMessage e))
      (System/exit 2))))
