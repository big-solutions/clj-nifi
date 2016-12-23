(def project 'big-solutions/clj-nifi)
(def version "0.1.0")

(set-env! :resource-paths #{"resources" "src"}
          :dependencies   '[[org.clojure/clojure "1.8.0"]
                            [org.apache.nifi/nifi-api "1.1.0"]
                            [org.apache.nifi/nifi-processor-utils "1.1.0"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Clojure DSL for Apache NiFi"
      :url         "https://github.com/big-solutions/clj-nifi"
      :scm         {:url "https://github.com/big-solutions/clj-nifi"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (aot :all true) (jar) (install)))

