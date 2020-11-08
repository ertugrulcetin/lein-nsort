(ns leiningen.check-namespace-decls
  (:require [leiningen.core.eval :as lein]
            [leiningen.core.project :as project]))


(defn check-namespace-decls
  [project]
  (lein/eval-in-project
   project
   `(lein-nsort.core/check-namespace-decls '~(:nsort project))
   '(require 'lein-nsort.core)))
