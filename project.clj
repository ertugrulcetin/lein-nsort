(defproject lein-nsort "0.1.0-SNAPSHOT"

  :description "FIXME: write description"

  :url "http://example.com/FIXME"

  :author "Ertuğrul Çetin"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.namespace "1.0.0"]
                 [com.rpl/specter "1.1.3"]]

  :repl-options {:init-ns lein-nsort.core}

  :eval-in-leiningen true)
