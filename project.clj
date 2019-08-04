(defproject technobabble "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[bidi "2.1.3" :exclusions [ring/ring-core]]
                 [bouncer "1.0.1"]
                 [buddy "2.0.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.8.1"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/react-bootstrap "0.31.5-0" :exclusions [org.webjars.bower/jquery]]
                 [clojure.java-time "0.3.2"]
                 [compojure "1.6.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.google.javascript/closure-compiler-unshaded "v20190618" :scope "provided"]
                 [com.taoensso/timbre "4.10.0"]
                 [conman "0.8.3"]
                 [cprop "0.1.14"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [funcool/struct "1.4.0"]
                 [jayq "2.5.5"]
                 [kibu/pushy "0.3.8"]
                 [luminus-http-kit "0.1.6"]
                 [luminus-immutant "0.1.0"]
                 [luminus-migrations "0.6.5"]
                 [luminus-transit "0.1.1"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.0"]
                 [metosin/compojure-api "1.1.12"]
                 [metosin/muuntaja "0.5.0"]
                 [metosin/reitit "0.3.9"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.16"]
                 [nrepl "0.6.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/google-closure-library "0.0-20190213-2033d5d9" :scope "provided"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.postgresql/postgresql "42.2.6"]
                 [org.webjars.npm/bulma "0.7.5"]
                 [org.webjars.npm/material-icons "0.3.0"]
                 [org.webjars/webjars-locator "0.36"]
                 [re-frame "0.10.8"]
                 [reagent "0.8.1"]
                 [reagent-forms "0.5.43"]
                 [reagent-utils "0.3.1"]
                 [ring-middleware-format "0.7.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.1"]
                 [prismatic/schema "1.1.11"]
                 [selmer "1.12.12"]
                 [org.clojure/core.async  "0.4.490"]
                 [jarohen/chord "0.8.1"]
                 [medley "1.1.0"]
                 [http-kit "2.4.0-alpha2"]]

  :min-lein-version "2.0.0"
  :jvm-opts ["-server"]

  :heroku {:app-name      "technobabbleapp"
           :include-files ["target/uberjar/technobabble.jar"]}

  :main technobabble.core
  ;; Necessary at a global level for uberjar deployments to Heroku
  :uberjar-name "technobabble.jar"

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.0.9"]
            [lein-heroku "0.5.3"]
            [migratus-lein "0.5.0"]]

  :migratus {:store         :database
             :migration-dir "migrations"}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :test-paths ["test/clj" "test/cljs" "test/cljc"]

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :cljsbuild
  {:builds        {:app {:source-paths ["src/cljs"]
                         :compiler     {:asset-path    "/js/out"
                                        :externs       ["react/externs/react.js" "externs/jquery-1.9.js" "externs/misc-externs.js"]
                                        :optimizations :none
                                        :output-to     "target/cljsbuild/public/js/technobabble.js"
                                        :output-dir    "target/cljsbuild/public/js/out"
                                        :pretty-print  true}}}
   :test-commands {"test" ["phantomjs" "phantom/unit-test.js" "phantom/unit-test.html"]}}

  :profiles
  {:uberjar      {:omit-source    true
                  :uberjar-name   "technobabble.jar"
                  :prep-tasks     ["clean" "compile" ["cljsbuild" "once"]]
                  :aot            :all
                  :source-paths   ["env/prod/clj"]
                  :resource-paths ["env/prod/resources"]
                  :hooks          [leiningen.cljsbuild]
                  :cljsbuild      {:jar true
                                   :builds
                                   {:app
                                    {:source-paths ["env/prod/cljs"]
                                     :compiler     {:optimizations    :advanced
                                                    :pretty-print     false
                                                    :closure-warnings {:externs-validation :off
                                                                       :non-standard-jsdoc :off}}}}}}

   :dev          [:project/dev :profiles/dev]
   :test         [:project/test :profiles/test]

   :project/dev  {:jvm-opts       ["-server" "-Dconf=dev-config.edn"]
                  :dependencies   [[binaryage/devtools "0.9.9"]
                                   [prone "1.5.0"]
                                   [ring/ring-mock "0.3.2"]
                                   [ring/ring-devel "1.6.3"]
                                   [pjstadig/humane-test-output "0.8.3"]
                                   [figwheel-sidecar "0.5.15"]
                                   [com.cemerick/piggieback "0.2.2"]]
                  :source-paths   ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :plugins        [[lein-figwheel "0.5.15" :exclusions [org.clojure/clojure]]]
                  :cljsbuild      {:builds {:app {:source-paths ["env/dev/cljs"]
                                                  :compiler     {:main "technobabble.app"}}}}
                  :figwheel       {:http-server-root "public"
                                   :nrepl-port       7002
                                   :css-dirs         ["resources/public/css"]
                                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                  :repl-options   {:init-ns user}
                  :injections     [(require 'pjstadig.humane-test-output)
                                   (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts       ["-server" "-Dconf=test-config.edn"]
                  :source-paths   ["env/test/clj" "test/clj" "test/cljc" "test/cljs"]
                  :dependencies   [[ring/ring-mock "0.3.2"]] ; Added so I can run individual tests on a test REPL
                  ; :hooks          [leiningen.cljsbuild]
                  :resource-paths ["env/dev/resources" "env/test/resources"]
                  :cljsbuild      {:builds {:test {:source-paths ["src/cljs"]
                                                   :compiler
                                                   {:output-dir    "target/test/"
                                                    :externs       ["react/externs/react.js" "externs/jquery-1.9.js" "externs/misc-externs.js"]
                                                    :optimizations :whitespace
                                                    :pretty-print  true
                                                    :output-to     "target/test/technobabble-tests.js"}}}}}})
