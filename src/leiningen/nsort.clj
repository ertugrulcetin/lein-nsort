(ns leiningen.nsort
  (:require [leiningen.core.eval :as lein]
            [leiningen.core.project :as project]))


(defn- plugin-dep-vector
  [{:keys [plugins]}]
  (some
   (fn [[plugin-symb :as dep-vector]]
     (when (= plugin-symb 'lein-nsort/lein-nsort)
       dep-vector))
   plugins))


(defn- check-namespace-decls-profile
  [project]
  {:dependencies [(plugin-dep-vector project)]})


(defn nsort
  "Checks namespace decelerations order"
  ([project]
   (nsort project nil))
  ([project subtask]
   (let [project (project/merge-profiles project [(check-namespace-decls-profile project)])]
     (lein/eval-in-project
      project
      `(lein-nsort.core/nsort '~(:nsort project) ~subtask)
      '(require 'lein-nsort.core)))))
