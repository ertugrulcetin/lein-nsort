(defproject lein-nsort "0.1.11"

  :description "Leiningen plugin that checks that order of namespace declarations for Clojure files\n\n"

  :url "https://github.com/ertugrulcetin/lein-nsort"

  :author "Ertuğrul Çetin"

  :license {:name "MIT License"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.namespace "1.0.0"]
                 [com.rpl/specter "1.1.3"]]

  :eval-in-leiningen true)
