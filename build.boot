(def project 'big-solutions/clj-nifi)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.8.0"]
                            [org.apache.nifi/nifi-api "1.1.0"]
                            [org.apache.nifi/nifi-bootstrap "1.1.0"]
                            [org.apache.nifi/nifi-runtime "1.1.0"]
                            [org.apache.nifi/nifi-processor-utils "1.1.0"]

                            [onetom/boot-lein-generate "0.1.3" :scope "test"]])

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
  (comp (pom) (jar) (install)))

(deftask idea
         "Updates project.clj for Idea to pick up dependency changes."
         []
         (require 'boot.lein)
         (let [runner (resolve 'boot.lein/generate)]
           (runner)))
