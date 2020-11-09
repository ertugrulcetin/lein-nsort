(ns lein-nsort.core
  (:require [clojure.java.io :as io]
            [com.rpl.specter :as s]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.file :as file]
            [clojure.pprint :as pp]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.parser :as p]))


(defn- get-index-of [ns]
  (let [idx (.indexOf ns :as)]
    (if (neg? idx)
      (.indexOf ns :refer)
      idx)))


(def sort-fns {:asc               {:fn first}
               :desc              {:fn first :comp #(compare %2 %1)}
               :alias-bottom-asc  {:fn (juxt #(* -1 (get-index-of %)) first)}
               :alias-bottom-desc {:fn (juxt #(get-index-of %) first) :comp #(compare %2 %1)}})

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
                 :duplicate-decl? (> nses-count 1)
                 :file            (:file opts)}))))


(defmacro ignore-reader-exception
  [& body]
  `(try ~@body
        (catch Exception e#
          (if (= :reader-exception (:type (ex-data e#)))
            nil
            (throw e#)))))


(defn- find-ns-decls-in-dir [dir platform]
  (keep #(ignore-reader-exception
          {:file    %
           :ns-decl (file/read-file-ns-decl % (:read-opts platform))})
        (ns-find/find-sources-in-dir dir platform)))


(defn- get-invalid-declarations [{:keys [src-dir]
                                  :or   {src-dir "src"} :as opts}]
  (let [ns-decls (find-ns-decls-in-dir (io/file src-dir) ns-find/clj)]
    (reduce
     (fn [acc decl]
       (let [{:keys [file ns-decl]} decl
             decl ns-decl]
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
                                         :nses    v
                                         :file    file}))
                    acc
                    (group-by first (s/select path decl)))))
     []
     ns-decls)))


(defn- replace-ns-decls [opts]
  (try
    (let [start (. System (nanoTime))
          files (atom #{})
          nses  (get-invalid-declarations (or opts {}))
          _     (doseq [[ns* errors] (group-by :ns nses)
                        err errors]
                  (try
                    (println "Replacing ns:" ns* (:type err) "form")
                    (spit (:file err)
                          (let [form    (with-out-str (pp/pprint (cons (:type err) (:sorted err))))
                                data    (z/of-file (:file err))
                                prj-map (z/find-value data z/next 'ns)
                                req     (z/find-value prj-map z/next (:type err))]
                            (-> req m/up (z/replace (p/parse-string form)) z/root-string)))
                    (swap! files conj (.getAbsolutePath (:file err)))
                    (catch Exception e
                      (println "Error occurred. Could not update ns: " ns*)
                      (clojure.pprint/pprint e))))
          took  (long (/ (double (- (. System (nanoTime)) start)) 1000000.0))]
      (println "Took:" took "msecs -" (count @files) "files updated.")
      (System/exit 0))
    (catch Exception e
      (pp/pprint e)
      (System/exit 2))))


(defn- check-ns-decl [opts]
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


(defn nsort [opts subtask]
  (case subtask
    ("-r" "--replace") (replace-ns-decls opts)
    (check-ns-decl opts)))
