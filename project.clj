(defproject clj-comm "0.0.1-SNAPSHOT"
  :description "Clojure Adapter for the Java Communications API"
  :url "https://github.com/linneman/clj-comm"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.nrepl "0.2.7"]
                 [org.clojure/core.async "0.2.374"]]
  :plugins [[cider/cider-nrepl "0.9.0-SNAPSHOT"]]
  :resource-paths ["RXTXcomm.jar"]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:unchecked"]
  :main clj-comm.api
  :aot [clj-comm.api])
