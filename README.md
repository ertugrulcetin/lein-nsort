# lein-nsort

Leiningen plugin that checks that order of namespace declarations for Clojure files

## Usage

[![Clojars Project](https://clojars.org/lein-nsort/latest-version.svg)](https://clojars.org/lein-nsort)

### Adding the dependency to `:plugins`

Add `[lein-nsort "0.1.10"]` into the `:plugins` vector of your
`project.clj` or `~/.lein/profiles.clj`.

```clj
(defproject my-project
  :plugins [[lein-nsort "0.1.10"]])
```

### Running the checker

Run the nsort like this:

```
lein nsort
```

This will check `ns` declaration forms for all Clojure source files in
the `{:nsort {:src-dir "src"}}` of your project (default `./src/`).


## Configuration (Optional)

Add a `:nsort` key to your `project.clj` to customize the checker.

```clj
(defproject my-project
  :nsort {:src-dir "src" ;;(default)
          :require :asc ;;(default)
          :import  :desc})
```

There are 4 pre-defined sorting checks for `:require` and 2 for `:import`.

**require**: `:asc`, `:desc`, `:alias-bottom-asc` and `:alias-bottom-desc`

**import**: `:asc` and `:desc`

`:alias-bottom-asc` and `:alias-bottom-desc` keep namespaces without aliases at the bottom and sort rest of the namespace declarations:

```clj
;; :alias-bottom-asc
(:require
 [clojure.java.io :as io]
 [clojure.pprint :as pp]
 [clojure.tools.namespace.find :as ns-find]
 [clojure.string])
```

Also you can **define** your own sorting functions like:
```clj
{...
:nsort {:require {:sort-fn (juxt #(.indexOf % :as) first)}
        :import  {:sort-fn first
                  :comp    #(compare %2 %1)}} ;; converting to (sort-by first #(compare %2 %1) namespace-decls)
...}
```

## License

MIT License

Copyright (c) 2020 Ertuğrul Çetin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.